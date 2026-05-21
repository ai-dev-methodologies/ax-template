package com.ax.template.authblueprint.reportexport;

import java.util.List;

/**
 * SPI for the data feed that backs an export job.
 *
 * <p>Fork-receivers replace {@link DemoReportRowSource} with a domain-specific
 * implementation that pulls rows from real entities (orders, customers, audit logs,
 * etc.) and adapts them into the {@code List<String>} cells the writers consume.
 *
 * <p>Contract:
 * <ul>
 *   <li>{@link #header(CreateExportRequest)} — column titles, never null/empty.</li>
 *   <li>{@link #rows(CreateExportRequest, String)} — data rows for the caller; the
 *       implementation MUST scope to {@code ownerUserId} (no cross-user leakage).</li>
 * </ul>
 */
public interface ReportRowSource {

    List<String> header(CreateExportRequest request);

    List<List<String>> rows(CreateExportRequest request, String ownerUserId);
}
