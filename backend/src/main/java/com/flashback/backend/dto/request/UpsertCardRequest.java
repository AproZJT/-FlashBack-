package com.flashback.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpsertCardRequest(
        @NotBlank(message = "问题不能为空")
        String front,
        @NotBlank(message = "答案不能为空")
        String back,
        String frontImageUrl,
        String backImageUrl,
        String audioUrl
) {}
