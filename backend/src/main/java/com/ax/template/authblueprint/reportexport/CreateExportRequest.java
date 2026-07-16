package com.ax.template.authblueprint.reportexport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Body for {@code POST /api/exports}.
 *
 * <p>{@code format} is validated by {@link ExportFormat#parse} (EXPORT-FORMAT-002).
 * {@code query} is application-defined; the catalog passes it through to the
 * {@link ReportRowSource} SPI without inspection.
 */
public record CreateExportRequest(
    @NotBlank @Size(max = 32) String format,
    @Size(max = 128) String name,
    @Size(max = 50) Map<String, Object> query
) {}
