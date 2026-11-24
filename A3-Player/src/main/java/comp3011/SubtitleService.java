
package comp3011;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

public class SubtitleService {

    public record SubtitleSegment(double start, double end, String text) {}

    private final String serverUrl = "http://localhost:8080/api/v1/subtitles";

    public void transcribe(File videoFile, Consumer<List<SubtitleSegment>> onComplete) {
        Thread thread = new Thread(() -> {
            File audioFile = null;

            try {
                audioFile = extractAudio(videoFile);
                List<SubtitleSegment> subtitles = sendToServer(audioFile);

                onComplete.accept(subtitles);

            } catch (Exception e) {
                System.out.println("Subtitle error: " + e.getMessage());
            } finally {
                if (audioFile != null) {
                    audioFile.delete();
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private File extractAudio(File videoFile) throws Exception {
        File audioFile = File.createTempFile("subtitle-audio-", ".wav");
        File rawFile = File.createTempFile("subtitle-audio-", ".pcm");

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile);
        AudioPlayer audioPlayer = new AudioPlayer();

        int sampleRate;
        int channels;
        long totalBytes = 0;

        try (var output = new java.io.FileOutputStream(rawFile)) {

            grabber.start();

            sampleRate = grabber.getSampleRate();
            channels = grabber.getAudioChannels();

            Frame frame;

            while ((frame = grabber.grabSamples()) != null) {
                byte[] samples = audioPlayer.copySamples(frame);

                if (samples != null) {
                    output.write(samples);
                    totalBytes += samples.length;
                }
            }

        } finally {
            grabber.stop();
            grabber.release();
        }

        if (totalBytes == 0) {
            rawFile.delete();
            audioFile.delete();
            throw new IOException("No audio found in video");
        }

        try (var output = new java.io.FileOutputStream(audioFile);
             var input = new java.io.FileInputStream(rawFile)) {

            int byteRate = sampleRate * channels * 2;
            int blockAlign = channels * 2;

            output.write(new byte[] {
                'R','I','F','F',
                (byte)(totalBytes + 36),
                (byte)((totalBytes + 36) >> 8),
                (byte)((totalBytes + 36) >> 16),
                (byte)((totalBytes + 36) >> 24),
                'W','A','V','E',
                'f','m','t',' ',
                16,0,0,0,
                1,0,
                (byte)channels,0,
                (byte)sampleRate,
                (byte)(sampleRate >> 8),
                (byte)(sampleRate >> 16),
                (byte)(sampleRate >> 24),
                (byte)byteRate,
                (byte)(byteRate >> 8),
                (byte)(byteRate >> 16),
                (byte)(byteRate >> 24),
                (byte)blockAlign,0,
                16,0,
                'd','a','t','a',
                (byte)totalBytes,
                (byte)(totalBytes >> 8),
                (byte)(totalBytes >> 16),
                (byte)(totalBytes >> 24)
            });

            input.transferTo(output);
        }

        rawFile.delete();
        return audioFile;
    }

    private List<SubtitleSegment> sendToServer(File audioFile) throws Exception {
        String boundary = "----JavaBoundary123456";

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

        String fileName = audioFile.getName();

        byte[] fileBytes = java.nio.file.Files.readAllBytes(audioFile.toPath());

        java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();

        body.write(("--" + boundary + "\r\n").getBytes());
        body.write(("Content-Disposition: form-data; name=\"audio\"; filename=\"" + fileName + "\"\r\n").getBytes());
        body.write(("Content-Type: audio/wav\r\n\r\n").getBytes());
        body.write(fileBytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes());

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(serverUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        java.net.http.HttpResponse<String> response = client.send(
                request,
                java.net.http.HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException("Server returned: " + response.statusCode() + " " + response.body());
        }

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response.body());

        java.util.ArrayList<SubtitleSegment> result = new java.util.ArrayList<>();

        for (com.fasterxml.jackson.databind.JsonNode segment : root.path("segments")) {

            double start = segment.path("start").asDouble();
            double end = segment.path("end").asDouble();
            String text = segment.path("text").asText("");

            if (!text.isBlank()) {
                result.add(new SubtitleSegment(start, end, text.trim()));
            }
        }

        return result;
    }
}
