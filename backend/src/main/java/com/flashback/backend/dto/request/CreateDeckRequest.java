package com.flashback.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateDeckRequest(
        @NotBlank(message = "名称不能为空")
        String name
) {}
