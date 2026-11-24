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
            try {
                File audioFile = extractAudio(videoFile);
                List<SubtitleSegment> subtitles = sendToServer(audioFile);

                audioFile.delete();
                onComplete.accept(subtitles);

            } catch (Exception e) {
                System.out.println("Subtitle error: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private File extractAudio(File videoFile) throws Exception {
        return videoFile;
    }
    private List<SubtitleSegment> sendToServer(File audioFile) throws IOException {
        return List.of();
    }
}