/**
 * @ax-template-meta
 * template_id: backend/import-export/ExportJobService
 * layer: backend-application
 * domain: import-export
 * anchors_rule: chunked-import-required-when-rowcount-gt-1000.md (PRACTICES-INTEG-002)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Transactional Outbox Pattern (microservices.io) — background export jobs are dispatched inside the domain transaction and executed asynchronously by the JobWorker"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Inject this service into your export controller.
 *   Call dispatchCsvExport / dispatchExcelExport inside an @Transactional method.
 *   The caller receives a jobId immediately and polls /api/jobs/{jobId}/status.
 */
package com.example.app.importexport;

import com.example.app.jobs.JobDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Dispatches background export jobs via {@link JobDispatcher} (Transactional Outbox).
 *
 * <p>Export jobs are dispatched within the caller's transaction so the job row
 * is committed atomically with the domain write. The {@code JobWorker} polls
 * and executes asynchronously; callers receive a {@code jobId} for polling.
 */
@Service
public class ExportJobService {

    private static final Logger log = LoggerFactory.getLogger(ExportJobService.class);

    public static final String JOB_TYPE_CSV_EXPORT   = "csv-export";
    public static final String JOB_TYPE_EXCEL_EXPORT = "excel-export";

    private final JobDispatcher jobDispatcher;

    public ExportJobService(JobDispatcher jobDispatcher) {
        this.jobDispatcher = jobDispatcher;
    }

    @Transactional
    public UUID dispatchCsvExport(String entity, Map<String, Object> filters) {
        var payload = buildPayload(entity, "csv", filters);
        UUID jobId = jobDispatcher.dispatch(JOB_TYPE_CSV_EXPORT, payload);
        log.info("CSV export job dispatched: entity={} jobId={}", entity, jobId);
        return jobId;
    }

    @Transactional
    public UUID dispatchExcelExport(String entity, Map<String, Object> filters) {
        var payload = buildPayload(entity, "xlsx", filters);
        UUID jobId = jobDispatcher.dispatch(JOB_TYPE_EXCEL_EXPORT, payload);
        log.info("Excel export job dispatched: entity={} jobId={}", entity, jobId);
        return jobId;
    }

    private static Map<String, Object> buildPayload(String entity, String format,
                                                     Map<String, Object> filters) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("entity", entity);
        payload.put("format", format);
        payload.putAll(filters);
        return Map.copyOf(payload);
    }
}
