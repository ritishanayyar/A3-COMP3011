package com.example.assignment1.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
//Thread-safe counter forSTT token usage since server start.
@Service
public class StatisticsService {
    private final AtomicLong inputTokens = new AtomicLong(0);
    private final AtomicLong outputTokens = new AtomicLong(0);
    private final DoubleAdder usageSeconds = new DoubleAdder();

    public void addTokens(long input, long output) {
        inputTokens.addAndGet(input);
        outputTokens.addAndGet(output);
    }
    
    public long getInputTokens() {
        return inputTokens.get();
    }
    public void addUsageSeconds(double seconds) {
        if (Double.isFinite(seconds) && seconds > 0) {
            usageSeconds.add(seconds);
        }
    }
    public long getOutputTokens() {
        return outputTokens.get();
    }
    public double getUsageSeconds() {
        return usageSeconds.sum();
    }
}
