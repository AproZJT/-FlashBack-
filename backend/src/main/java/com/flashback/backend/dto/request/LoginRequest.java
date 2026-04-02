package com.flashback.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "userId 不能为空")
        String userId
) {}
