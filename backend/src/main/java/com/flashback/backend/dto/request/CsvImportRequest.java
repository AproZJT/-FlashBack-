package com.flashback.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CsvImportRequest(
        @NotBlank(message = "csvContent 不能为空")
        String csvContent,
        String defaultDeckName
) {}
