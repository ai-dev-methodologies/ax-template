package com.ax.template.authblueprint.filestorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Catalog default {@link StorageBackend} — files are written under
 * {@code ${STORAGE_BASE_DIR}/files/<key>}.
 * <p>
 * Trace: FILE-SEC-001 / FILE-SEC-002 — the {@code key} is an opaque server
 * UUID; the absolute filesystem path is NEVER exposed to API consumers.
 * Manifest: {@code blueprints/file-storage-manifest.yaml#storage.type=local}.
 *
 * <p>This default lives entirely under a temp directory in tests
 * ({@code java.io.tmpdir}/ax-file-storage) so the build does not require any
 * environment configuration. Production replaces this bean with an S3-backed
 * implementation marked {@link org.springframework.context.annotation.Primary @Primary}.
 */
@Component
public class LocalFilesystemStorage implements StorageBackend {

    private final Path baseDir;

    public LocalFilesystemStorage(
        @Value("${ax.file-storage.local.base-dir:#{systemProperties['java.io.tmpdir']}/ax-file-storage}")
        String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, InputStream content, String contentType, long sizeBytes) throws IOException {
        Path target = resolveSafe(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream get(String key) throws IOException {
        Path target = resolveSafe(key);
        if (!Files.exists(target)) {
            throw new java.nio.file.NoSuchFileException(key);
        }
        return Files.newInputStream(target);
    }

    @Override
    public void delete(String key) throws IOException {
        Path target = resolveSafe(key);
        Files.deleteIfExists(target);
    }

    /**
     * FILE-SEC-001 defense in depth — even though {@code key} should already be
     * a server-generated UUID, refuse to resolve any path that escapes
     * {@link #baseDir}. This prevents a future bug in the upstream caller from
     * turning a directory traversal into a filesystem write outside the storage
     * root.
     */
    private Path resolveSafe(String key) {
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException(
                "Storage key escapes base directory: " + key);
        }
        return resolved;
    }
}
