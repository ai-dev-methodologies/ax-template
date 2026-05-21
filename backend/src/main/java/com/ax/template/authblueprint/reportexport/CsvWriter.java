package com.ax.template.authblueprint.reportexport;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RFC 4180 CSV writer.
 *
 * <p>Trace:
 * <ul>
 *   <li>EXPORT-FORMAT-001 — UTF-8 BOM + CRLF line endings + double-quote escaping</li>
 *   <li>EXPORT-INJECT-001 — every cell routed through {@link FormulaInjectionGuard}</li>
 * </ul>
 *
 * <p>Manifest: {@code blueprints/report-export-manifest.yaml#csv}.
 */
@Component
public class CsvWriter {

    /** UTF-8 BOM: EF BB BF. Excel uses this to auto-detect UTF-8 on open. */
    private static final byte[] BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    /** Render the report. Returns the complete file bytes ready for storage / download. */
    public byte[] write(List<String> header, List<List<String>> rows) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(BOM);
            try (Writer w = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
                writeRecord(w, header);
                for (List<String> row : rows) {
                    writeRecord(w, row);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("CSV write failure", ex);
        }
        return baos.toByteArray();
    }

    private static void writeRecord(Writer w, List<String> cells) throws IOException {
        boolean first = true;
        for (String cell : cells) {
            if (!first) {
                w.write(',');
            }
            w.write(escape(cell));
            first = false;
        }
        // RFC 4180 §2.1 — CRLF between records.
        w.write("\r\n");
    }

    /**
     * RFC 4180 §2.5–§2.7 escaping:
     * <ul>
     *   <li>Run {@link FormulaInjectionGuard#neutralize} first (EXPORT-INJECT-001).</li>
     *   <li>Wrap in double quotes if the value contains {@code ,}, {@code "}, CR or LF.</li>
     *   <li>Double any embedded {@code "} character.</li>
     * </ul>
     */
    static String escape(String raw) {
        String neutralized = FormulaInjectionGuard.neutralize(raw);
        boolean needsQuoting =
            neutralized.indexOf(',') >= 0
            || neutralized.indexOf('"') >= 0
            || neutralized.indexOf('\n') >= 0
            || neutralized.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return neutralized;
        }
        return '"' + neutralized.replace("\"", "\"\"") + '"';
    }
}
