package com.ax.template.authblueprint.reportexport;

/** EXPORT-FORMAT-002 — mapped to HTTP 400 with errorCode UNSUPPORTED_FORMAT. */
public class UnsupportedFormatException extends RuntimeException {
    public UnsupportedFormatException(String detail) {
        super(detail);
    }
}
