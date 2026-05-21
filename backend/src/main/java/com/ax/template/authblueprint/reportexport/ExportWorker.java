package com.ax.template.authblueprint.reportexport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Background worker that drains {@link ExportJobStatus#PENDING} jobs.
 *
 * <p>Each tick (fixedDelay 1s):
 * <ol>
 *   <li>Pick up at most {@code worker.batch-size} PENDING jobs ordered by createdAt.</li>
 *   <li>For each, transition PENDING → RUNNING, generate the file via the writers,
 *       then transition RUNNING → COMPLETED (or FAILED on error).</li>
 *   <li>Each job processed in its own transaction so a single failure does not poison
 *       the batch.</li>
 * </ol>
 *
 * <p>Trace: EXPORT-LIFECYCLE-001..003 closure (PENDING flows to a terminal state).
 * Manifest: {@code blueprints/report-export-manifest.yaml#worker}.
 */
@Component
public class ExportWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ExportWorker.class);

    private static final TypeReference<Map<String, Object>> QUERY_MAP_TYPE = new TypeReference<>() {};

    private final ExportJobRepository repository;
    private final ExportJobStateMachine stateMachine;
    private final CsvWriter csvWriter;
    private final XlsxWriter xlsxWriter;
    private final ReportRowSource rowSource;
    private final ReportExportProperties properties;
    private final ObjectMapper objectMapper;

    public ExportWorker(ExportJobRepository repository,
                        ExportJobStateMachine stateMachine,
                        CsvWriter csvWriter,
                        XlsxWriter xlsxWriter,
                        ReportRowSource rowSource,
                        ReportExportProperties properties,
                        ObjectMapper objectMapper) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.csvWriter = csvWriter;
        this.xlsxWriter = xlsxWriter;
        this.rowSource = rowSource;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${report-export.worker-poll-interval-ms:1000}")
    public void drainPending() {
        int batch = Math.max(1, properties.getWorkerBatchSize());
        List<ExportJob> ready =
            repository.findByStatusOrderByCreatedAtAsc(ExportJobStatus.PENDING, PageRequest.of(0, batch));
        for (ExportJob job : ready) {
            try {
                processOne(job.getId());
            } catch (RuntimeException ex) {
                LOG.warn("export-worker: unexpected failure for job {}: {}", job.getId(), ex.getMessage());
            }
        }
    }

    /**
     * Process exactly one job by id. Used directly by tests so they don't need to
     * wait for the @Scheduled tick. Each invocation runs in its own transaction so
     * a poison job doesn't corrupt the worker thread state.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID jobId) {
        ExportJob job = repository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != ExportJobStatus.PENDING) {
            return;
        }
        stateMachine.markRunning(job);
        repository.saveAndFlush(job);

        try {
            CreateExportRequest req = new CreateExportRequest(
                job.getFormat().name().toLowerCase(Locale.ROOT),
                job.getName(),
                deserializeQuery(job.getQueryJson())
            );
            List<String> header = rowSource.header(req);
            List<List<String>> rows = rowSource.rows(req, job.getOwnerUserId());

            byte[] bytes = switch (job.getFormat()) {
                case CSV -> csvWriter.write(header, rows);
                case XLSX -> xlsxWriter.write(header, rows);
            };

            stateMachine.markCompleted(job, bytes, rows.size());
            repository.save(job);
        } catch (RuntimeException ex) {
            stateMachine.markFailed(job, truncate(ex.getMessage()));
            repository.save(job);
            LOG.info("export-worker: job {} FAILED — {}", job.getId(), ex.getMessage());
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "unknown error";
        }
        return s.length() <= 1024 ? s : s.substring(0, 1024);
    }

    private Map<String, Object> deserializeQuery(String queryJson) {
        if (queryJson == null || queryJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(queryJson, QUERY_MAP_TYPE);
        } catch (Exception ex) {
            // Defensive: if the persisted JSON is corrupted, fail the job rather than
            // silently dropping the query. The state-machine treats this as RUNNING→FAILED.
            throw new IllegalStateException("queryJson is not a JSON object", ex);
        }
    }
}
