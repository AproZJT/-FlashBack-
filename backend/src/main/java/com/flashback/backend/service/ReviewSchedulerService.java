package com.flashback.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ReviewSchedulerService {
    private static final long HOUR = 60L * 60L * 1000L;
    private static final long DAY = 24L * HOUR;

    public long nextReviewTimeV1(int mastery, String feedback, long now) {
        if ("again".equals(feedback)) return now + 12 * HOUR;
        if ("hard".equals(feedback)) return now + DAY;
        if ("good".equals(feedback)) {
            return switch (mastery) {
                case 1 -> now + 2 * DAY;
                case 2 -> now + 4 * DAY;
                case 3 -> now + 7 * DAY;
                default -> now + DAY;
            };
        }
        return now + 10 * DAY; // easy
    }

    public long nextReviewTimeV2(Map<String, Object> card, String feedback, long now) {
        double difficulty = ((Number) card.getOrDefault("difficulty", 0.35)).doubleValue();
        double stability = ((Number) card.getOrDefault("stability", 0.0)).doubleValue();

        switch (feedback) {
            case "again" -> {
                difficulty = Math.min(1.0, difficulty + 0.10);
                stability = Math.max(0.0, stability * 0.45);
            }
            case "hard" -> {
                difficulty = Math.min(1.0, difficulty + 0.05);
                stability = stability + 0.6;
            }
            case "good" -> {
                difficulty = Math.max(0.0, difficulty - 0.03);
                stability = stability + 1.3;
            }
            case "easy" -> {
                difficulty = Math.max(0.0, difficulty - 0.06);
                stability = stability + 2.1;
            }
        }

        double retrievability = Math.max(0.0, Math.min(1.0, 1.0 - difficulty * 0.55));
        card.put("difficulty", difficulty);
        card.put("stability", stability);
        card.put("retrievability", retrievability);

        long base = switch (feedback) {
            case "again" -> 12 * HOUR;
            case "hard" -> DAY;
            case "good" -> 2 * DAY;
            default -> 3 * DAY;
        };
        long bonus = (long) (stability * 0.7 * DAY);
        return now + Math.max(base, base + bonus);
    }
}
