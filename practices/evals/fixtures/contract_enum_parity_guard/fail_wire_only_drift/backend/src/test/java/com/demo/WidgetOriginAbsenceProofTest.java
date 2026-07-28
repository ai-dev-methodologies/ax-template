package com.demo;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Fixture stand-in for a runtime/bytecode proof. The real ones
 * (PaymentContractAbsenceProofTest, RateLimitPingWireVocabularyTest) read the compiled
 * classes via ArchUnit, the live Spring bean graph, and real HTTP responses; here only the
 * BINDING is exercised — the parity guard checks this file exists, carries the tag its
 * manifest entry names, contains the declared anchors (contracts/demo-openapi.yaml plus
 * WidgetOriginResolver / WidgetLabels), and that the tag is wired into the gradle task the
 * entry names, so a "runtime proof" cannot be a test nothing runs.
 */
class WidgetOriginAbsenceProofTest {

    @Test
    @Tag("DEMO")
    void noWidgetOriginResolverIsRegisteredAndWidgetLabelsAreTheWholeVocabulary() {
        // stand-in body — see the class javadoc
    }
}
