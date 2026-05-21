package com.ax.template.authblueprint.reportexport;

import java.util.UUID;

/** EXPORT-AUTHZ-002 / 003 — mapped to HTTP 404 by the controller. */
public class ExportJobNotFoundException extends RuntimeException {
    public ExportJobNotFoundException(UUID id) {
        super("export job not found: " + id);
    }
}
