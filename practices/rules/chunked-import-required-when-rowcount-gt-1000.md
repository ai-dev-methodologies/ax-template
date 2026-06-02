---
title: CSV and Excel imports with potentially >1000 rows must use chunked streaming with per-chunk transactions
impact: HIGH
impactDescription: "Importing large files with readAll() loads the entire dataset into heap and wraps it in a single transaction, causing OOM errors and blocking rollback of earlier valid rows on late failures"
tags:
  - integration
  - performance
  - import
  - chunking
  - transaction
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-INTEG-002"
verification:
  gradle_task: testIntegration
  tag: INTEGRATION
failing_fixture_path: "practices/evals/fixtures/chunked_import/fail_no_chunk"
passing_fixture_path: "practices/evals/fixtures/chunked_import/pass"
evidence:
  - source_type: external
    citation: "OpenCSV — CSVReader.readNext() streams one row at a time from the underlying reader; CSVReader.readAll() materialises the entire file into a List<String[]> in heap memory"
    url: "https://opencsv.sourceforge.net/#reading_into_beans_by_name"
  - source_type: external
    citation: "Apache POI SXSSF API — for large Excel files, use SXSSFWorkbook (streaming read) or XSSFWorkbook with row-by-row iteration; loading all rows at once causes heap pressure above ~50k rows"
    url: "https://poi.apache.org/components/spreadsheet/how-to.html#sxssf"
  - source_type: external
    citation: "Spring Batch Reference — chunk-oriented processing: read N items, process, write, then commit; bounds memory usage to chunk size regardless of total input size"
    url: "https://docs.spring.io/spring-batch/reference/step/chunk-oriented-processing.html"
---

## CSV and Excel imports with potentially >1000 rows must use chunked streaming with per-chunk transactions

**Impact: HIGH — Importing large files with `readAll()` loads the entire dataset into heap and wraps it in a single transaction, causing OOM errors and blocking rollback of earlier valid rows on late failures**

Production CSV/Excel imports frequently exceed 10,000–100,000 rows. Two anti-patterns cause catastrophic failures at scale:

1. **`readAll()` / full-load** — `CSVReader.readAll()` and `XSSFWorkbook` sheet iteration into a `List` load all rows into heap simultaneously. A 100,000-row × 5-column file at ~200 bytes/row = 20 MB minimum; object overhead easily doubles this. Concurrent imports OOM the JVM.

2. **Single outer `@Transactional`** — wrapping the entire import in one transaction holds a DB connection open for its entire duration, blocks rollback at the row that fails (rolling back 50,000 already-saved rows), and degrades write performance due to lock accumulation.

**Required pattern:**
- Use `CSVReader.readNext()` (streaming, one row at a time) or Apache POI row-by-row iteration
- Accumulate rows into a `List<String[]>` chunk of `CHUNK_SIZE` (100–1000)
- Call a `@Transactional` method that persists the chunk and returns — this commits only those rows
- Collect row-level errors into an accumulator without aborting the batch

**Incorrect — `readAll()` + single outer `@Transactional`:**

```java
@Transactional          // VIOLATION: wraps entire import in one transaction
public ImportResult importFile(MultipartFile file) {
    List<String[]> allRows = new CSVReader(reader).readAll();  // VIOLATION: loads all rows into heap
    repository.saveAll(allRows.stream().map(this::toEntity).toList());
    return new ImportResult(allRows.size(), 0, List.of());
}
```

**Correct — streaming `readNext()` with per-chunk `@Transactional`:**

```java
public static final int CHUNK_SIZE = 500;

// CsvImportService — no @Transactional. persistChunk lives on a SEPARATE bean so the
// @Transactional proxy is actually crossed. A self-call (this.persistChunk(...)) to a
// @Transactional method in the SAME bean bypasses the proxy and silently runs with NO
// per-chunk transaction — defeating the whole point. Inject the collaborator instead.
private final ChunkPersister chunkPersister;

public ImportResult importFile(MultipartFile file) {          // no @Transactional here
    List<String[]> chunk = new ArrayList<>(CHUNK_SIZE);
    String[] row;
    while ((row = csvReader.readNext()) != null) {
        chunk.add(row);
        if (chunk.size() >= CHUNK_SIZE) {
            chunkPersister.persistChunk(chunk, ...);   // cross-bean call → real per-chunk tx
            chunk.clear();
        }
    }
    if (!chunk.isEmpty()) chunkPersister.persistChunk(chunk, ...);
}

@Component
class ChunkPersister {
    @Transactional                        // CORRECT: honored — call crosses the proxy boundary
    public int persistChunk(List<String[]> rows, ...) {
        // validate + save rows; collect errors without throwing
    }
}
```

See `templates/backend/import-export/CsvImportService.java` for the reference implementation.

Reference: [OpenCSV — Reading large CSV files with readNext()](https://opencsv.sourceforge.net/#reading_into_beans_by_name)

Reference: [Apache POI SXSSF — Streaming API for large Excel files](https://poi.apache.org/components/spreadsheet/how-to.html#sxssf)

Reference: [Spring Batch — Chunk-Oriented Processing](https://docs.spring.io/spring-batch/reference/step/chunk-oriented-processing.html)
