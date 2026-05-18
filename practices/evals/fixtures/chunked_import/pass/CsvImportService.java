package com.example.app;

import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * FIXTURE: PASS — satisfies PRACTICES-INTEG-002 (chunked-import-required-when-rowcount-gt-1000).
 *
 * <p>Streams rows one at a time with readNext(), accumulates in chunks of CHUNK_SIZE,
 * and commits each chunk in its own @Transactional method.
 */
@Service
public class CsvImportService {

    /** PASS: CHUNK_SIZE declared — chunking is in use. */
    public static final int CHUNK_SIZE = 500;

    /** PASS: no @Transactional on the outer method — it does NOT span all rows. */
    public ImportResult importFile(MultipartFile file) throws Exception {
        int importedCount = 0;
        List<ImportError> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) return new ImportResult(0, 0, List.of());

            List<String[]> chunk = new ArrayList<>(CHUNK_SIZE);
            String[] row;
            int rowNumber = 1;

            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                chunk.add(row);

                if (chunk.size() >= CHUNK_SIZE) {
                    // PASS: persistChunk has @Transactional — each chunk is its own transaction
                    importedCount += persistChunk(chunk, rowNumber - chunk.size() + 1, errors);
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                importedCount += persistChunk(chunk, rowNumber - chunk.size() + 1, errors);
            }
        }

        return new ImportResult(importedCount, errors.size(), List.copyOf(errors));
    }

    /** PASS: @Transactional scoped to the chunk only — not the entire import. */
    @Transactional
    public int persistChunk(List<String[]> rows, int chunkStartRow, List<ImportError> errors) {
        int saved = 0;
        for (int i = 0; i < rows.size(); i++) {
            // repository.save(mapRow(rows.get(i)));
            saved++;
        }
        return saved;
    }

    public record ImportResult(int importedCount, int errorCount, List<ImportError> errors) {}
    public record ImportError(int rowNumber, String message) {}
}
