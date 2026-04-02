package com.flashback.backend.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 20, message = "昵称长度不能超过 20")
        String nickname,
        @Size(max = 80, message = "目标长度不能超过 80")
        String goal
) {}
