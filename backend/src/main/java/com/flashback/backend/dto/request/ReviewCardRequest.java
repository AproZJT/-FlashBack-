package com.flashback.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewCardRequest(
        @NotBlank(message = "反馈不能为空")
        @Pattern(regexp = "forget|blur|master", message = "feedback 仅支持 forget/blur/master")
        String feedback
) {}
