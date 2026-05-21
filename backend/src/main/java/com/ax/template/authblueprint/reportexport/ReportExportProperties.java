package com.ax.template.authblueprint.reportexport;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration binding for the report-export domain.
 *
 * <p>Defaults mirror {@code blueprints/report-export-manifest.yaml}. Fork-receivers
 * override via {@code application.yml} when the operating envelope differs.
 */
@ConfigurationProperties(prefix = "report-export")
public class ReportExportProperties {

    /** Soft cap; jobs requesting more rows are rejected at creation with 400 TOO_MANY_ROWS. */
    private int maxRowsPerJob = 100_000;

    /** Worker poll cadence in milliseconds. */
    private long workerPollIntervalMs = 1_000L;

    /** Maximum number of PENDING jobs the worker drains per poll tick. */
    private int workerBatchSize = 4;

    public int getMaxRowsPerJob() { return maxRowsPerJob; }
    public void setMaxRowsPerJob(int v) { this.maxRowsPerJob = v; }

    public long getWorkerPollIntervalMs() { return workerPollIntervalMs; }
    public void setWorkerPollIntervalMs(long v) { this.workerPollIntervalMs = v; }

    public int getWorkerBatchSize() { return workerBatchSize; }
    public void setWorkerBatchSize(int v) { this.workerBatchSize = v; }
}
