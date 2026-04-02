package com.flashback.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record ToggleDeckPublicRequest(
        @NotNull(message = "发布开关不能为空")
        Boolean value
) {}
