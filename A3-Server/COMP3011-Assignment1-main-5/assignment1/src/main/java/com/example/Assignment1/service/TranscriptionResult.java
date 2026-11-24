package com.example.assignment1.service;
import java.util.List;
public record TranscriptionResult(
        String text,
        List<SubtitleSegment> segments,
        double durationSeconds,
        long inputTokens,
        long outputTokens
) {
    public record SubtitleSegment(
            double start, double end, String text, double noSpeechProb, double avgLogProb
    ) {
    }
    public TranscriptionResult(
            String text, long inputTokens,long outputTokens
    ) {
        this(text, List.of(), 0.0, inputTokens, outputTokens);
    }
}