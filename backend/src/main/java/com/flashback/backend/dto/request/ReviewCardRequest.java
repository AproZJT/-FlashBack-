package com.flashback.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewCardRequest(
        @NotBlank(message = "反馈不能为空")
        @Pattern(regexp = "again|hard|good|easy|forget|blur|master", message = "feedback 仅支持 again/hard/good/easy")
        String feedback,
        @jakarta.validation.constraints.Min(value = 0, message = "version 不能小于 0")
        long version
) {}
