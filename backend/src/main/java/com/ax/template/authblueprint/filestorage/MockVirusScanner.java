package com.ax.template.authblueprint.filestorage;

import org.springframework.stereotype.Component;

/**
 * Catalog default {@link VirusScanner} — flags any filename containing
 * {@code "EICAR"} as INFECTED, everything else as CLEAN.
 * <p>
 * Trace: FILE-SCAN-001 — matches the mock_rules in
 * {@code blueprints/file-storage-manifest.yaml#virus_scan.mock_rules}.
 * Fork-receivers replace with a ClamAV or vendor-API client.
 *
 * <p>The EICAR marker is the industry-standard antivirus test pattern:
 * {@code https://www.eicar.org/download-anti-malware-testfile/}. Using only
 * the filename keeps this SPI side-effect-free (no need to re-read the byte
 * stream after it has been persisted to {@link StorageBackend}).
 */
@Component
public class MockVirusScanner implements VirusScanner {

    @Override
    public FileScanResult scan(String fileName, String contentType, long sizeBytes) {
        if (fileName != null && fileName.toUpperCase(java.util.Locale.ROOT).contains("EICAR")) {
            return FileScanResult.INFECTED;
        }
        return FileScanResult.CLEAN;
    }
}
