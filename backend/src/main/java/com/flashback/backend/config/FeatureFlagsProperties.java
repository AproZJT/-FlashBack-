package com.flashback.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flashback.feature")
public record FeatureFlagsProperties(
        boolean enableV2Schedule,
        boolean enableOfflineSync,
        boolean enableMediaFields,
        boolean enableMetrics
) {}
