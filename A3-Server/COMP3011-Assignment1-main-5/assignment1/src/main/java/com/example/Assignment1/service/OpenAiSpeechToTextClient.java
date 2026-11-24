
package com.example.assignment1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiSpeechToTextClient implements SpeechToTextClient {
    private static final Logger log =
            LoggerFactory.getLogger(OpenAiSpeechToTextClient.class);
    private static final String OPENAI_URL =
            "https://api.openai.com/v1/audio/transcriptions";

    private static final String MODEL = "whisper-1";
    private static final int CONNECT_TIMEOUT_MS= 5000;
    private static final int READ_TIMEOUT_MS = 60000;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient restClient;

    public OpenAiSpeechToTextClient() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public TranscriptionResult transcribe(
            byte[] audioBytes,
            String filename
    ) throws Exception {

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY environment variable is not set"
            );
        }
        MultiValueMap<String, Object> payload =
                new LinkedMultiValueMap<>();
        payload.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        payload.add("model", MODEL);
        payload.add("response_format", "verbose_json");

        // Request both word and segment level timestamps
        payload.add("timestamp_granularities[]", "word");
        payload.add("timestamp_granularities[]", "segment");

        long startTime = System.nanoTime();

        String response = restClient.post()
                .uri(OPENAI_URL)
                .header("Authorization", "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(String.class);

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        JsonNode root= mapper.readTree(response);

        String text = root.path("text").asText("");
        double durationSeconds =root.path("duration").asDouble(0.0);

        List<TranscriptionResult.SubtitleSegment> segments =new ArrayList<>();
        JsonNode segmentsNode = root.path("segments");

        if (segmentsNode.isArray()) {
            for (JsonNode segment : segmentsNode) {
                double segmentStart =segment.path("start").asDouble(0.0);
                double segmentEnd =segment.path("end").asDouble(0.0);

                String segmentText = segment.path("text").asText("");
                double noSpeechProb =
                        segment.path("no_speech_prob").asDouble(0.0);

                double avgLogProb =segment.path("avg_logprob").asDouble(0.0);
                segments.add(
                        new TranscriptionResult.SubtitleSegment(segmentStart, segmentEnd,segmentText, noSpeechProb,avgLogProb
                        )
                );
            }
        }
        log.info(
                "Whisper transcription completed in {}ms. Duration: {} seconds. Segments: {}",
                elapsedMs,
                durationSeconds,
                segments.size()
        );
        return new TranscriptionResult( text, segments,durationSeconds,
                0,
                0
        );
    }
}