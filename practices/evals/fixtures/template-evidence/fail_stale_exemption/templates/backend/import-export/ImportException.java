/**
 * @ax-template-meta
 * template_id: backend/import-export/ImportException
 * layer: backend-application
 * provenance_class: internal_design
 * evidence:
 *   - source_type: internal
 *     rationale: "This file IS listed in JAVA_NO_EVIDENCE_EXEMPT but carries evidence, so
 *       the exemption is stale and the guard must say so instead of skipping the file."
 * usage: Replace 'com.example.app' with your base package.
 */
package com.example.app.importexport;

public class ImportException extends RuntimeException {

    public ImportException(String message) {
        super(message);
    }
}
