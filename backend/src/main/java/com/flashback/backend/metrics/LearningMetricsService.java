package com.flashback.backend.metrics;

import com.flashback.backend.config.FeatureFlagsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class LearningMetricsService {
    private static final Logger log = LoggerFactory.getLogger("flashback.metrics");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate redis;
    private final FeatureFlagsProperties flags;

    public LearningMetricsService(StringRedisTemplate redis, FeatureFlagsProperties flags) {
        this.redis = redis;
        this.flags = flags;
    }

    public void onReview(String userId, String feedback, long dueBefore, long dueAfter) {
        if (!flags.enableMetrics()) return;

        String day = LocalDate.now().format(DAY_FMT);
        String dailyKey = "fb:metrics:review:daily:" + userId + ":" + day;
        Long dailyReviewed = redis.opsForValue().increment(dailyKey);
        redis.expire(dailyKey, 30, TimeUnit.DAYS);

        String totalKey = "fb:metrics:review:total:" + userId;
        String doneKey = "fb:metrics:review:done:" + userId;
        Long total = redis.opsForValue().increment(totalKey);
        Long done = redis.opsForValue().increment(doneKey, "master".equals(feedback) ? 1 : 0);

        double completionRate = (total == null || total == 0 || done == null)
                ? 0
                : (done * 1.0 / total);

        log.info("event=review_metrics user={} feedback={} daily_reviewed={} due_before={} due_after={} completion_rate={}",
                userId, feedback, dailyReviewed == null ? 0 : dailyReviewed, dueBefore, dueAfter, String.format("%.4f", completionRate));
    }
}
