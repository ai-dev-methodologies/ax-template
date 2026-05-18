/**
 * @ax-template-meta
 * template_id: backend/import-export/ExcelImportService
 * layer: backend-application
 * domain: import-export
 * anchors_rule: chunked-import-required-when-rowcount-gt-1000.md (PRACTICES-INTEG-002)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Apache POI SXSSF — for large Excel files use SXSSFWorkbook (streaming variant); XSSFWorkbook loads the entire file into memory and causes OOM errors for files with 10k+ rows"
 *     url: "https://poi.apache.org/components/spreadsheet/how-to.html#sxssf"
 *   - source_type: external
 *     citation: "Apache POI DataFormatter — converts cell values to String regardless of cell type (numeric, date, boolean, formula result); avoids ClassCastException from raw Cell.getStringCellValue()"
 *     url: "https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/DataFormatter.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Add poi-ooxml dependency: implementation("org.apache.poi:poi-ooxml:5.3.0")
 *   Override mapRow() to map cell arrays to your domain entity.
 *   Replace the `saved++` stub in persistChunk() with repository.save(entity).
 */
package com.example.app.importexport;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunked, transactional Excel (.xlsx) import service — implements PRACTICES-INTEG-002.
 */
@Service
public class ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    public static final int  CHUNK_SIZE          = 500;
    public static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L;  // 20 MB

    private final DataFormatter formatter = new DataFormatter();

    public CsvImportService.ImportResult importFile(MultipartFile file) {
        validateFile(file);

        int importedCount = 0;
        List<CsvImportService.ImportError> errors = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                return new CsvImportService.ImportResult(0, 0, List.of());
            }

            // Row 0 = header
            Row headerRow = sheet.getRow(0);
            String[] header = rowToStrings(headerRow);

            List<String[]> chunk = new ArrayList<>(CHUNK_SIZE);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                chunk.add(rowToStrings(row));

                if (chunk.size() >= CHUNK_SIZE) {
                    int chunkStart = rowIdx - chunk.size() + 2; // 1-based, skip header
                    importedCount += persistChunk(chunk, header, chunkStart, errors);
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                int chunkStart = sheet.getLastRowNum() - chunk.size() + 2;
                importedCount += persistChunk(chunk, header, chunkStart, errors);
            }

        } catch (IOException ex) {
            log.error("Excel import failed: {}", ex.getMessage());
            throw new ImportException("Failed to read Excel file", ex);
        }

        log.info("Excel import complete: imported={} errors={}", importedCount, errors.size());
        return new CsvImportService.ImportResult(importedCount, errors.size(), List.copyOf(errors));
    }

    @Transactional
    public int persistChunk(List<String[]> rows, String[] header, int chunkStartRow,
                            List<CsvImportService.ImportError> errors) {
        int saved = 0;
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = chunkStartRow + i;
            try {
                Object entity = mapRow(rows.get(i), header);
                // TODO: replace with: repository.save(entity)
                saved++;
            } catch (Exception ex) {
                errors.add(new CsvImportService.ImportError(rowNum, "Parse error: " + ex.getMessage()));
            }
        }
        return saved;
    }

    protected Object mapRow(String[] row, String[] header) {
        return row;
    }

    private String[] rowToStrings(Row row) {
        int cols = row.getLastCellNum();
        String[] result = new String[cols];
        for (int c = 0; c < cols; c++) {
            result[c] = formatter.formatCellValue(row.getCell(c));
        }
        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ImportException("Upload file is empty");
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImportException("File too large: max=" + MAX_FILE_SIZE_BYTES / 1024 / 1024 + " MB");
        }
    }
}
