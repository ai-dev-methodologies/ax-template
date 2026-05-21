package com.ax.template.authblueprint.reportexport;

/**
 * Shared neutralization helper for CSV / XLSX formula injection (CWE-1236).
 *
 * <p>Trace: EXPORT-INJECT-001 (CSV) + EXPORT-INJECT-002 (XLSX). Manifest:
 * {@code blueprints/report-export-manifest.yaml#security.formula_injection_prevention}.
 *
 * <p>Cells whose first character is in the trigger set ({@code =, +, -, @, TAB, CR})
 * are prefixed with a single-quote ({@code '}) so that spreadsheet applications
 * (Excel, Google Sheets, LibreOffice) treat them as literal text rather than
 * formulas. Both CSV ({@link CsvWriter}) and XLSX ({@link XlsxWriter}) writers
 * route every cell through {@link #neutralize(String)}.
 *
 * <p>Reference: OWASP CSV Injection Prevention Cheat Sheet.
 */
public final class FormulaInjectionGuard {

    private FormulaInjectionGuard() {}

    /** Single-quote prefix (U+0027) used as the neutralization marker. */
    public static final char NEUTRALIZER = '\'';

    /**
     * If {@code raw} starts with a formula-trigger character, return a copy prefixed
     * with the neutralization marker; otherwise return {@code raw} unchanged.
     * Null inputs map to an empty string so writers never emit literal "null".
     */
    public static String neutralize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        char first = raw.charAt(0);
        if (isTrigger(first)) {
            return NEUTRALIZER + raw;
        }
        return raw;
    }

    /** Test-visible predicate: is {@code c} in the formula-trigger set? */
    public static boolean isTrigger(char c) {
        return c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r';
    }
}
