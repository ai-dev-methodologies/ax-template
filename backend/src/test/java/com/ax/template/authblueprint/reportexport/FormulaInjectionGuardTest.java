package com.ax.template.authblueprint.reportexport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for the shared {@link FormulaInjectionGuard} helper that both
 * {@link CsvWriter} and {@link XlsxWriter} delegate to.
 *
 * <p>Trace: EXPORT-INJECT-001 / EXPORT-INJECT-002 — anchors the helper-level
 * invariant that backs the higher-level black-box assertions in
 * {@link ReportExportComplianceTest}.
 */
@Tag("REPORT_EXPORT")
class FormulaInjectionGuardTest {

    @ParameterizedTest
    @ValueSource(strings = {"=", "+", "-", "@"})
    @Tag("EXPORT-INJECT-001")
    void neutralize_prefixesAllFormulaTriggers(String trigger) {
        String input = trigger + "cmd|' /C calc'!A0";

        String result = FormulaInjectionGuard.neutralize(input);

        assertThat(result).startsWith("'");
        assertThat(result).isEqualTo("'" + input);
    }

    @Test
    @Tag("EXPORT-INJECT-001")
    void neutralize_prefixesTabAndCr() {
        assertThat(FormulaInjectionGuard.neutralize("\tlikely-evil")).isEqualTo("'\tlikely-evil");
        assertThat(FormulaInjectionGuard.neutralize("\revil-too")).isEqualTo("'\revil-too");
    }

    @Test
    @Tag("EXPORT-INJECT-001")
    void neutralize_leavesSafeValuesUnchanged() {
        assertThat(FormulaInjectionGuard.neutralize("alice")).isEqualTo("alice");
        assertThat(FormulaInjectionGuard.neutralize("100.00")).isEqualTo("100.00");
        // A leading space is not a trigger character — the value is passed through.
        assertThat(FormulaInjectionGuard.neutralize(" =not-leading")).isEqualTo(" =not-leading");
    }

    @Test
    @Tag("EXPORT-INJECT-001")
    void neutralize_nullAndEmptyMapToEmpty() {
        assertThat(FormulaInjectionGuard.neutralize(null)).isEmpty();
        assertThat(FormulaInjectionGuard.neutralize("")).isEmpty();
    }
}
