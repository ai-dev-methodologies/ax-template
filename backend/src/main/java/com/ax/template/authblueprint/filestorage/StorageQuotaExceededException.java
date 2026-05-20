package com.ax.template.authblueprint.filestorage;

/**
 * Trace: FILE-QUOTA-001 — per-user quota exceeded → HTTP 413 ProblemDetail.
 */
public class StorageQuotaExceededException extends RuntimeException {

    private final long currentBytes;
    private final long requestedBytes;
    private final long maxBytes;

    public StorageQuotaExceededException(long currentBytes, long requestedBytes, long maxBytes) {
        super("Storage quota exceeded: current=" + currentBytes
            + " requested=" + requestedBytes + " max=" + maxBytes);
        this.currentBytes = currentBytes;
        this.requestedBytes = requestedBytes;
        this.maxBytes = maxBytes;
    }

    public long getCurrentBytes() { return currentBytes; }
    public long getRequestedBytes() { return requestedBytes; }
    public long getMaxBytes() { return maxBytes; }
}
