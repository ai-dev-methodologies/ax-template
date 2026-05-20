package com.ax.template.authblueprint.filestorage;

/**
 * Trace: FILE-UPLOAD-001 — contentType outside the manifest allowlist → HTTP 415.
 */
public class UnsupportedContentTypeException extends RuntimeException {
    public UnsupportedContentTypeException(String contentType) {
        super("Unsupported content type: " + contentType);
    }
}
