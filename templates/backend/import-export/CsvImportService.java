/**
 * @ax-template-meta
 * template_id: backend/import-export/CsvImportService
 * layer: backend-application
 * domain: import-export
 * anchors_rule: chunked-import-required-when-rowcount-gt-1000.md (PRACTICES-INTEG-002)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "OpenCSV — CSVReader.readNext() streams one row at a time; never use readAll() for large files as it loads the entire file into a List<String[]>, causing OutOfMemoryError at scale"
 *     url: "https://opencsv.sourceforge.net/#reading_into_beans_by_name"
 *   - source_type: external
 *     citation: "Spring @Transactional — applying @Transactional to the chunk-persist method (not the outer loop) bounds rollback scope to CHUNK_SIZE rows and prevents a single bad row from rolling back thousands of good rows"
 *     url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Add opencsv dependency: implementation("com.opencsv:opencsv:5.9")
 *   Override mapRow() to map raw CSV columns to your domain entity.
 *   Replace the `saved++` stub in persistChunk() with repository.save(entity).
 */
package com.example.app.importexport;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunked, transactional CSV import service — implements PRACTICES-INTEG-002.
 *
 * <p>Rows are processed in batches of {@link #CHUNK_SIZE}; each batch runs in its
 * own transaction via {@link org.springframework.transaction.support.TransactionTemplate}
 * (a self-invoked {@code @Transactional} method would bypass the proxy) to bound heap usage and rollback scope.
 * At most {@code CHUNK_SIZE} rows are held in memory at any time.
 */
@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    /** Rows per transaction — satisfies PRACTICES-INTEG-002 (must chunk above 1000). */
    public static final int CHUNK_SIZE = 500;

    /** Maximum accepted file size (10 MB). */
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;

    /**
     * Per-chunk transaction boundary. A programmatic {@link TransactionTemplate} is used
     * deliberately instead of an {@code @Transactional} method on this same bean: a
     * self-invoked {@code @Transactional} method bypasses Spring's proxy and runs with NO
     * transaction at all, silently defeating the per-chunk rollback scope. The template
     * makes each chunk a real unit — committed on success, rolled back on failure.
     */
    private final TransactionTemplate transactionTemplate;

    public CsvImportService(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public ImportResult importFile(MultipartFile file) {
        validateFile(file);

        int importedCount = 0;
        List<ImportError> errors = new ArrayList<>();
        int rowNumber = 0;

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();  // skip header row
            if (header == null) return new ImportResult(0, 0, List.of());

            List<String[]> chunk = new ArrayList<>(CHUNK_SIZE);
            String[] row;
            rowNumber = 1;

            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                chunk.add(row);

                if (chunk.size() >= CHUNK_SIZE) {
                    importedCount += persistChunkInTx(chunk, header, rowNumber - chunk.size() + 1, errors);
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                importedCount += persistChunkInTx(chunk, header, rowNumber - chunk.size() + 1, errors);
            }

        } catch (IOException | CsvValidationException ex) {
            log.error("CSV import failed at row {}: {}", rowNumber, ex.getMessage());
            throw new ImportException("Failed to read CSV at row " + rowNumber, ex);
        }

        log.info("CSV import complete: imported={} errors={}", importedCount, errors.size());
        return new ImportResult(importedCount, errors.size(), List.copyOf(errors));
    }

    // ── Chunk persistence — each chunk is its own transaction ─────────────────

    /** Wraps a chunk in a real transaction via the template — proxy-self-invocation safe. */
    private int persistChunkInTx(List<String[]> rows, String[] header, int chunkStartRow,
                                 List<ImportError> errors) {
        return transactionTemplate.execute(status -> persistChunk(rows, header, chunkStartRow, errors));
    }

    int persistChunk(List<String[]> rows, String[] header, int chunkStartRow,
                     List<ImportError> errors) {
        int saved = 0;
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = chunkStartRow + i;
            try {
                Object entity = mapRow(rows.get(i), header);
                // TODO: replace with: repository.save(entity)
                saved++;
            } catch (Exception ex) {
                errors.add(new ImportError(rowNum, "Parse error: " + ex.getMessage()));
            }
        }
        return saved;
    }

    // ── Row mapping — override for your domain entity ──────────────────────────

    protected Object mapRow(String[] row, String[] header) {
        // Default: return the raw row. Override to return a domain entity.
        return row;
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ImportException("Upload file is empty");
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImportException("File too large: max=" + MAX_FILE_SIZE_BYTES / 1024 + " KB");
        }
    }

    // ── Value types ─────────────────────────────────────────────────────────────

    public record ImportResult(int importedCount, int errorCount, List<ImportError> errors) {}
    public record ImportError(int rowNumber, String message) {}
}
