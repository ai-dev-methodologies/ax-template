package com.ax.template.authblueprint.filestorage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Storage backend SPI — catalog default is {@link LocalFilesystemStorage}; a
 * fork-receiver swaps to S3 / GCS / Azure Blob by providing an alternative
 * {@link org.springframework.context.annotation.Primary @Primary} bean.
 * <p>
 * Trace: FILE-SEC-001 — keys are opaque to API consumers; only the backend
 * knows how to resolve them. The {@code key} param is the {@code storageKey}
 * from {@link StoredFile} (a server-generated UUID), never the user-provided
 * filename.
 */
public interface StorageBackend {

    /**
     * Persist the byte stream under {@code key}. The stream is consumed; the
     * backend is responsible for closing internal resources but not the caller's
     * {@link InputStream}.
     */
    void put(String key, InputStream content, String contentType, long sizeBytes) throws IOException;

    /** Open a read stream for the previously-stored {@code key}. */
    InputStream get(String key) throws IOException;

    /** Best-effort delete; no-op if key does not exist. */
    void delete(String key) throws IOException;
}
