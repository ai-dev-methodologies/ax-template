package com.ax.template.importer;

import com.ax.template.authblueprint.AuthBlueprintBackendApplication;
import com.ax.template.authblueprint.importer.CsvImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: CsvImportService chunked processing.
 *
 * <p>RED phase: tests FAIL until {@code CsvImportService} is registered in the
 * application context and wired to an in-memory {@code ItemRepository}.
 *
 * <p>GREEN phase: passes after:
 * <ol>
 *   <li>{@code CsvImportService} bean is present in the application context.
 *   <li>The service processes rows in chunks of {@code CHUNK_SIZE} (streaming, not readAll).
 *   <li>10 000-row import completes without OutOfMemoryError.
 *   <li>Individual row errors are collected without aborting the batch.
 * </ol>
 *
 * <p>Rule protected: {@code chunked-import-required-when-rowcount-gt-1000} (PRACTICES-INTEG-002).
 *
 * @see com.ax.template.authblueprint.importer.CsvImportService
 */
@Tag("INTEGRATION")
@SpringBootTest(classes = AuthBlueprintBackendApplication.class)
class CsvImportChunkedIT {

    @Autowired
    CsvImportService csvImportService;

    @Test
    @DisplayName("10 000-row CSV import completes without OutOfMemoryError")
    void import_10kRows_noOom() throws Exception {
        // Arrange — generate a 10 000-row CSV in memory (small enough for test setup)
        byte[] csvBytes = buildCsv(10_000);
        MockMultipartFile file = new MockMultipartFile(
                "file", "large-import.csv", "text/csv", csvBytes);

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapBefore = memoryBean.getHeapMemoryUsage().getUsed();

        // Act
        CsvImportService.ImportResult result = csvImportService.importFile(file);

        // Assert — import completed and heap growth is bounded (chunked, not readAll)
        assertThat(result.importedCount()).isEqualTo(10_000);
        assertThat(result.errorCount()).isEqualTo(0);

        long heapAfter = memoryBean.getHeapMemoryUsage().getUsed();
        long heapGrowthMb = (heapAfter - heapBefore) / (1024 * 1024);

        // Chunked processing: heap growth should stay well below total-rows × row-size.
        // At ~100 bytes/row × 10 000 rows = ~1 MB total data; chunked peak << 50 MB.
        assertThat(heapGrowthMb)
                .as("Heap growth should be bounded by chunk size, not total row count")
                .isLessThan(50);
    }

    @Test
    @DisplayName("CSV import with 5% invalid rows reports errors without aborting batch")
    void import_withInvalidRows_reportsErrorsWithoutAbortingBatch() throws Exception {
        // Arrange — 1000 rows, every 20th row has an empty name (invalid)
        byte[] csvBytes = buildCsvWithErrors(1_000, 20);
        MockMultipartFile file = new MockMultipartFile(
                "file", "partial-errors.csv", "text/csv", csvBytes);

        // Act
        CsvImportService.ImportResult result = csvImportService.importFile(file);

        // Assert — valid rows imported, invalid rows reported
        int expectedErrors = 1_000 / 20;     // every 20th row = 50 errors
        int expectedImported = 1_000 - expectedErrors;

        assertThat(result.errorCount()).isEqualTo(expectedErrors);
        assertThat(result.importedCount()).isEqualTo(expectedImported);
        assertThat(result.errors()).hasSize(expectedErrors);
        assertThat(result.errors().get(0).rowNumber()).isGreaterThan(1);
    }

    @Test
    @DisplayName("CHUNK_SIZE constant is declared and is in the 100-1000 range")
    void chunkSize_isInAcceptableRange() {
        int chunkSize = CsvImportService.CHUNK_SIZE;
        assertThat(chunkSize)
                .as("CHUNK_SIZE should be between 100 and 1000 rows")
                .isBetween(100, 1_000);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static byte[] buildCsv(int rowCount) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            w.println("name,description");
            for (int i = 1; i <= rowCount; i++) {
                w.printf("Item-%d,Description for item %d%n", i, i);
            }
        }
        return baos.toByteArray();
    }

    private static byte[] buildCsvWithErrors(int rowCount, int errorEveryN) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            w.println("name,description");
            for (int i = 1; i <= rowCount; i++) {
                if (i % errorEveryN == 0) {
                    w.printf(",Description for invalid row %d%n", i); // empty name = invalid
                } else {
                    w.printf("Item-%d,Description for item %d%n", i, i);
                }
            }
        }
        return baos.toByteArray();
    }
}
