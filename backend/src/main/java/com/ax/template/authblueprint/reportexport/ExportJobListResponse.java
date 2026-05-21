package com.ax.template.authblueprint.reportexport;

import java.util.List;

/** Paginated list envelope for {@code GET /api/exports}. */
public record ExportJobListResponse(
    List<ExportJobResponse> items,
    int page,
    int size,
    long totalElements
) {}
