package comp3011;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

//Extracts the audio of a video, sends it to the local subtitle web server inchunks, and returns timed subtitle segments as each chunk completes.

public class SubtitleService {

    public record SubtitleSegment(double start, double end, String text) {}

    private static final String SERVER_URL = "http://localhost:8080/api/v1/subtitles";
    private static final int SAMPLE_RATE = 16000;      //16 kHz mono
    private static final int CHANNELS = 1;
    private static final int CHUNK_SECONDS = 120;      
    private static final int MAX_LINE_CHARS = 42;
    private static final int MAX_LINES = 2;
    private static final double MIN_SHOW_SECONDS = 1.0;
    private static final double MAX_SHOW_SECONDS = 7.0;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    //Runs on a background thread. onSegments is called (off the FX thread) after each chnk with the new segments; onError is called once if anything fails.

    public void transcribe(File videoFile, Consumer<List<SubtitleSegment>> onSegments, Consumer<Exception> onError) {
        Thread thread = new Thread(() -> {
            try {
                extractAndSend(videoFile, onSegments);
            } catch (Exception e) {
                System.out.println("Subtitle error: " + e.getMessage());
                onError.accept(e);
            }
        }, "subtitle-transcriber");
        thread.setDaemon(true);
        thread.start();
    }

    private void extractAndSend(File videoFile, Consumer<List<SubtitleSegment>> onSegments) throws Exception {
        final int bytesPerChunk = SAMPLE_RATE * CHANNELS * 2 * CHUNK_SECONDS;
        AudioPlayer converter = new AudioPlayer();
        ByteArrayOutputStream chunk = new ByteArrayOutputStream(bytesPerChunk);
        int chunkIndex = 0;
        boolean sawAudio = false;

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile);
        grabber.setSampleRate(SAMPLE_RATE);
        grabber.setAudioChannels(CHANNELS);
        try {
            grabber.start();
            Frame frame;
            while ((frame = grabber.grabSamples()) != null) {
                byte[] samples = converter.copySamples(frame);
                if (samples == null || samples.length == 0) {
                    continue;
                }
                sawAudio = true;
                chunk.write(samples);
                if (chunk.size() >= bytesPerChunk) {
                    sendChunk(chunk.toByteArray(), chunkIndex * (double) CHUNK_SECONDS, onSegments);
                    chunk.reset();
                    chunkIndex++;
                }
            }
        } finally {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception ignored) {
                // media may already be gone 
            }
        }

        if (chunk.size() > 0) {
            sendChunk(chunk.toByteArray(), chunkIndex * (double) CHUNK_SECONDS, onSegments);
        }
        if (!sawAudio) {
            throw new IOException("No audio found in video");
        }
    }

    private void sendChunk(byte[] pcm, double offsetSeconds, Consumer<List<SubtitleSegment>> onSegments)
            throws Exception {
        byte[] wav = toWav(pcm);
        String boundary = "----SubtitleBoundary" + System.nanoTime();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n").getBytes());
        body.write(("Content-Disposition: form-data; name=\"audio\"; filename=\"chunk.wav\"\r\n").getBytes());
        body.write("Content-Type: audio/wav\r\n\r\n".getBytes());
        body.write(wav);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Server returned " + response.statusCode() + ": " + response.body());
        }

        List<SubtitleSegment> result = new ArrayList<>();
        for (JsonNode segment : mapper.readTree(response.body()).path("segments")) {
            String text = segment.path("text").asText("").trim();
            if (!text.isBlank()) {
                double start = segment.path("start").asDouble() + offsetSeconds;
                double end = segment.path("end").asDouble() + offsetSeconds;
                result.addAll(splitIntoSubtitles(start, end, text));
            }
        }
        onSegments.accept(result);
    }

    private static byte[] toWav(byte[] pcm) throws IOException {
        int byteRate = SAMPLE_RATE * CHANNELS * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length + 44);
        out.write("RIFF".getBytes());
        writeIntLE(out, pcm.length + 36);
        out.write("WAVEfmt ".getBytes());
        writeIntLE(out, 16);
        writeShortLE(out, 1);
        writeShortLE(out, CHANNELS);
        writeIntLE(out, SAMPLE_RATE);
        writeIntLE(out, byteRate);
        writeShortLE(out, CHANNELS * 2);
        writeShortLE(out, 16);
        out.write("data".getBytes());
        writeIntLE(out, pcm.length);
        out.write(pcm);
        return out.toByteArray();
    }

    private static void writeIntLE(ByteArrayOutputStream out, int v) {
        out.write(v); out.write(v >> 8); out.write(v >> 16); out.write(v >> 24);
    }

    private static void writeShortLE(ByteArrayOutputStream out, int v) {
        out.write(v); out.write(v >> 8);
    }


    // Splits one Whisper segment into displayable subtitles with timings proportional to text length
    static List<SubtitleSegment> splitIntoSubtitles(double start, double end, String text) {
        List<String> pieces = new ArrayList<>();
        String remaining = text.trim();
        int maxChars = MAX_LINE_CHARS * MAX_LINES;
        while (remaining.length() > maxChars) {
            int cut = bestBreak(remaining, maxChars);
            pieces.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isEmpty()) {
            pieces.add(remaining);
        }

        double duration = Math.max(end - start, 0.1);
        int totalChars = pieces.stream().mapToInt(String::length).sum();
        List<SubtitleSegment> out = new ArrayList<>();
        double cursor = start;
        for (String piece : pieces) {
            double share = duration * piece.length() / Math.max(totalChars, 1);
            double shown = Math.min(MAX_SHOW_SECONDS, Math.max(MIN_SHOW_SECONDS, share));
            out.add(new SubtitleSegment(cursor, cursor + shown, wrapLines(piece)));
            cursor += share;
        }
        return out;
    }
    private static int bestBreak(String s, int limit) {
        int limitIdx = Math.min(limit, s.length() - 1);
        for (String set : new String[] {".!?", ",;:", " "}) {
            for (int i = limitIdx; i > limit / 3; i--) {
                if (set.indexOf(s.charAt(i - 1)) >= 0 || (set.equals(" ") && s.charAt(i) == ' ')) {
                    return i;
                }
            }
        }
        return limitIdx;
    } 
    private static String wrapLines(String text) {
        if (text.length() <= MAX_LINE_CHARS) {
            return text;
        }
        int best = -1;
        int mid = text.length() / 2;
        for (int i = 1; i < text.length(); i++) {
            if (i > MAX_LINE_CHARS || text.length() - i > MAX_LINE_CHARS + 1) {
                continue;
            }
            boolean afterPunct = ",;:.!?".indexOf(text.charAt(i - 1)) >= 0 && text.charAt(i) == ' ';
            boolean atSpace = text.charAt(i) == ' ';
            if (afterPunct || atSpace) {
                int score = Math.abs(i - mid) + (afterPunct ? 0 : 8);
                if (best < 0 || score < Math.abs(best - mid) + (isPunctBreak(text, best) ? 0 : 8)) {
                    best = i;
                }
            }
        }
        if (best < 0) {
            best = Math.min(MAX_LINE_CHARS, text.length() - 1);
        }
        return text.substring(0, best).trim() + "\n" + text.substring(best).trim();
    }

    private static boolean isPunctBreak(String text, int i) {
        return i > 0 && ",;:.!?".indexOf(text.charAt(i - 1)) >= 0 && text.charAt(i) == ' ';
    }
}