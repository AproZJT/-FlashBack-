package com.flashback.backend.metrics;

import com.flashback.backend.config.FeatureFlagsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestMetricsInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger("flashback.metrics");
    private static final String START_NANOS = "flashback.req.start_nanos";

    private final FeatureFlagsProperties flags;

    public RequestMetricsInterceptor(FeatureFlagsProperties flags) {
        this.flags = flags;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!flags.enableMetrics()) return true;
        request.setAttribute(START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!flags.enableMetrics()) return;
        Object startObj = request.getAttribute(START_NANOS);
        if (!(startObj instanceof Long start)) return;
        long costMs = (System.nanoTime() - start) / 1_000_000;
        String userId = request.getHeader("X-User-Id");
        log.info("event=request_latency path={} method={} status={} cost_ms={} user={}",
                request.getRequestURI(), request.getMethod(), response.getStatus(), costMs, userId == null ? "-" : userId);
    }
}
