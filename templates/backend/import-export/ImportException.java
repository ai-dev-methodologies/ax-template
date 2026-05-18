/**
 * @ax-template-meta
 * template_id: backend/import-export/ImportException
 * layer: backend-application
 * domain: import-export
 * provenance_class: internal_design
 * usage: Replace 'com.example.app' with your base package.
 */
package com.example.app.importexport;

/**
 * Thrown when a CSV or Excel import fails at the file level
 * (empty file, oversized, unreadable structure).
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
