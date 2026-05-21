package com.ax.template.authblueprint.reportexport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Office Open XML (XLSX) writer using Apache POI's streaming SXSSF workbook
 * to keep memory bounded on large exports (window size 1000 — manifest
 * {@code xlsx.window_size}).
 *
 * <p>Trace:
 * <ul>
 *   <li>EXPORT-FORMAT-001 partner — content-type
 *       {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}</li>
 *   <li>EXPORT-INJECT-002 — every cell forced to {@link CellType#STRING} and
 *       routed through {@link FormulaInjectionGuard} so spreadsheet apps never
 *       evaluate user-controlled values as formulas (CWE-1236).</li>
 * </ul>
 */
@Component
public class XlsxWriter {

    private static final int WINDOW_SIZE = 1000;
    private static final String SHEET_NAME = "data";

    public byte[] write(List<String> header, List<List<String>> rows) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(WINDOW_SIZE)) {
            Sheet sheet = wb.createSheet(SHEET_NAME);
            writeRow(sheet, 0, header);
            for (int i = 0; i < rows.size(); i++) {
                writeRow(sheet, i + 1, rows.get(i));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            wb.dispose();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("XLSX write failure", ex);
        }
    }

    private static void writeRow(Sheet sheet, int rowIndex, List<String> cells) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < cells.size(); i++) {
            // createCell(idx, STRING) sets the type at creation. setCellValue(String)
            // also keeps the type STRING — POI never escalates to FORMULA from
            // setCellValue(String), even when the value starts with '='. This is the
            // structural guarantee that backs EXPORT-INJECT-002.
            Cell c = row.createCell(i, CellType.STRING);
            c.setCellValue(FormulaInjectionGuard.neutralize(cells.get(i)));
        }
    }
}
