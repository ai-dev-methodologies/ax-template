package com.ax.template.authblueprint.reportexport;

import java.util.Locale;

/**
 * Output formats supported by the report-export domain.
 *
 * <p>Trace: EXPORT-FORMAT-001 (csv), EXPORT-FORMAT-002 (reject unsupported).
 * Manifest: {@code blueprints/report-export-manifest.yaml#formats.allowed = [csv, xlsx]}.
 */
public enum ExportFormat {
    CSV("text/csv", ".csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String contentType;
    private final String fileExtension;

    ExportFormat(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String contentType() {
        return contentType;
    }

    public String fileExtension() {
        return fileExtension;
    }

    /**
     * Parse a case-insensitive client-supplied string. Throws {@link UnsupportedFormatException}
     * for any value outside the manifest allowlist (EXPORT-FORMAT-002).
     */
    public static ExportFormat parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnsupportedFormatException("format is required");
        }
        try {
            return ExportFormat.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedFormatException("format not supported: " + raw);
        }
    }
}
