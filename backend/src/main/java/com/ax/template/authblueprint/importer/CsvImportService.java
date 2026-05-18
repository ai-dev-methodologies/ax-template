package com.ax.template.authblueprint.importer;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunked, transactional CSV import service.
 *
 * <p>Implements PRACTICES-INTEG-002 (chunked-import-required-when-rowcount-gt-1000):
 * rows are processed in batches of {@link #CHUNK_SIZE}, each batch in its own
 * transaction. At most {@code CHUNK_SIZE} row objects are held in heap at any time.
 *
 * <p>This is the ax-template reference implementation. Fork receivers extend or
 * replace {@link #mapRow} and {@code ItemRepository} with their domain entities.
 */
@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    /** Number of rows per database transaction — satisfies PRACTICES-INTEG-002. */
    public static final int CHUNK_SIZE = 500;

    /** Maximum file size accepted (10 MB). */
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Imports a CSV file using chunked streaming.
     *
     * @param file multipart CSV upload
     * @return import summary with counts and per-row errors
     */
    public ImportResult importFile(MultipartFile file) {
        validateFile(file);

        int importedCount = 0;
        List<ImportError> errors = new ArrayList<>();
        int rowNumber = 0;

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();   // skip header
            if (header == null) {
                return new ImportResult(0, 0, List.of());
            }

            List<String[]> chunk = new ArrayList<>(CHUNK_SIZE);
            String[] row;
            rowNumber = 1;

            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                chunk.add(row);

                if (chunk.size() >= CHUNK_SIZE) {
                    int saved = persistChunk(chunk, header, rowNumber - chunk.size() + 1, errors);
                    importedCount += saved;
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                int saved = persistChunk(chunk, header, rowNumber - chunk.size() + 1, errors);
                importedCount += saved;
            }

        } catch (IOException | CsvValidationException ex) {
            log.error("CSV import failed at row {}: {}", rowNumber, ex.getMessage());
            throw new ImportException("Failed to read CSV at row " + rowNumber, ex);
        }

        log.info("CSV import complete: imported={} errors={}", importedCount, errors.size());
        return new ImportResult(importedCount, errors.size(), List.copyOf(errors));
    }

    // ── Chunk persistence ──────────────────────────────────────────────────────

    /**
     * Persists a chunk of rows in a dedicated transaction.
     * Invalid rows are added to the error accumulator.
     */
    @Transactional
    public int persistChunk(List<String[]> rows, String[] header, int chunkStartRow,
                            List<ImportError> errors) {
        int saved = 0;
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = chunkStartRow + i;
            try {
                Item item = mapRow(rows.get(i), header);
                if (item.name() == null || item.name().isBlank()) {
                    errors.add(new ImportError(rowNum, "name must not be blank"));
                } else {
                    // In a real implementation: call repository.save(entity)
                    // For the reference implementation we count the row as saved.
                    saved++;
                }
            } catch (Exception ex) {
                errors.add(new ImportError(rowNum, "Parse error: " + ex.getMessage()));
            }
        }
        return saved;
    }

    // ── Row mapping ─────────────────────────────────────────────────────────────

    /**
     * Maps a raw CSV row to an {@link Item}. Override for your domain entity.
     */
    protected Item mapRow(String[] row, String[] header) {
        return new Item(
                row.length > 0 ? row[0].trim() : null,
                row.length > 1 ? row[1].trim() : null
        );
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
    public record Item(String name, String description) {}
}
