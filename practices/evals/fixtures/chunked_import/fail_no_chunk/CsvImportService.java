package com.example.app;

import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * FIXTURE: FAIL — violates PRACTICES-INTEG-002 (chunked-import-required-when-rowcount-gt-1000).
 *
 * <p>Uses readAll() to load entire file into heap AND wraps the whole import in a
 * single @Transactional, causing OOM on large files and preventing partial rollback.
 */
@Service
public class CsvImportService {

    // VIOLATION: no CHUNK_SIZE declared — no chunking

    @Transactional  // VIOLATION: outer @Transactional wraps entire import
    public ImportResult importFile(MultipartFile file) throws Exception {
        Reader reader = new InputStreamReader(file.getInputStream());
        CSVReader csvReader = new CSVReader(reader);

        // VIOLATION: readAll() loads entire file into heap
        List<String[]> allRows = csvReader.readAll();
        csvReader.close();

        // VIOLATION: single saveAll for all rows — no chunking
        for (String[] row : allRows) {
            // repository.save(mapRow(row));
        }

        return new ImportResult(allRows.size(), 0, List.of());
    }

    public record ImportResult(int importedCount, int errorCount, List<Object> errors) {}
}
