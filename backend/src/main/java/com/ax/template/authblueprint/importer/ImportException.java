package com.ax.template.authblueprint.importer;

/**
 * Thrown when a CSV or Excel import fails due to file-level errors
 * (empty file, file too large, unreadable CSV structure).
 *
 * <p>Row-level validation errors are collected into
 * {@link CsvImportService.ImportError} and do NOT throw this exception.
 */
public class ImportException extends RuntimeException {

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
