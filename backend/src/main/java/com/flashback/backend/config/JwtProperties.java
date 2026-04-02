package com.flashback.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flashback.jwt")
public record JwtProperties(String secret, long expireSeconds) {
}
