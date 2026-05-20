package com.ax.template.authblueprint.filestorage;

/**
 * Trace: FILE-UPLOAD-002 — file > {@code max_file_size_mb} → HTTP 413.
 */
public class FileSizeExceededException extends RuntimeException {
    public FileSizeExceededException(long actual, long max) {
        super("File size " + actual + " exceeds max " + max);
    }
}
