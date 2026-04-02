package com.flashback.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateCardRequest(
        @NotBlank(message = "问题不能为空")
        String front,
        @NotBlank(message = "答案不能为空")
        String back,
        String frontImageUrl,
        String backImageUrl,
        String audioUrl,
        @Min(value = 0, message = "version 不能小于 0")
        long version
) {}
