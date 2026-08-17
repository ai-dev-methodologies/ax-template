package com.ax.template.authblueprint.common;

import io.restassured.RestAssured;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BACKLOG P2-120 — deliberate-break proof for {@link AxPort}, the single writer of
 * {@code RestAssured.port}.
 *
 * <p>This class declares <b>no</b> {@code @LocalServerPort} field and boots no Spring context, so
 * it also serves as the live proof of {@link AxPort}'s silence rule: the extension must leave the
 * process-global untouched here rather than publishing a guess. The live-wiring half — that the
 * extension really does publish a real {@code @LocalServerPort} — is
 * {@link AxPortLiveWiringTest}, which is where a regression would actually bite.
 *
 * <p>Run: {@code ./gradlew testCommonPrimitives}.
 */
@Tag("COMMON_HTTP_EXTRACT")
class AxPortProofTest {

    @Test
    void noLocalServerPortField_leavesTheGlobalUntouchedAndSaysSo() {
        // The extension ran beforeEach for this very method and found no field.
        assertThat(AxPort.diagnose())
            .as("silence must be reported as silence, never as a verified port")
            .contains("NOTHING PUBLISHED")
            .contains(getClass().getName())
            .contains("no @LocalServerPort was published")
            .contains("NOT ruled out");
    }

    @Test
    void overrideForStub_rejectsANonPositivePort_withoutPublishingIt() {
        int before = RestAssured.port;

        assertThatThrownBy(() -> AxPort.overrideForStub(0))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("non-positive port (0)");

        assertThat(RestAssured.port)
            .as("a rejected port must not reach the process-global RestAssured.port")
            .isEqualTo(before);
    }

    @Test
    void overrideForStub_thenRestore_returnsBothTheGlobalAndTheRecord() {
        int before = RestAssured.port;
        String recordBefore = AxPort.diagnose();

        AxPort.overrideForStub(45671);
        try {
            assertThat(RestAssured.port).isEqualTo(45671);
            assertThat(AxPort.diagnose())
                .contains("STUB OVERRIDE")
                .contains("45671")
                .contains("deliberately NOT this application");
        } finally {
            AxPort.restoreAfterStub();
        }

        assertThat(RestAssured.port)
            .as("an unrestored stub port would silently steer every later test in this JVM")
            .isEqualTo(before);
        assertThat(AxPort.diagnose()).isEqualTo(recordBefore);
    }

    @Test
    void restoreWithoutOverride_failsLoudly() {
        assertThatThrownBy(AxPort::restoreAfterStub)
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("without a matching overrideForStub()");
    }

    @Test
    void driftIsClassifiedBothWays_matchesAndClobbered() {
        // Exercised through the observation seam, not by assigning the global: a proof that had
        // to become a second writer to demonstrate single-writing would prove the opposite.
        AxPort.overrideForStub(45672);
        try {
            assertThat(AxPort.diagnoseAgainst(45672))
                .as("the published port still in place must be reported as published")
                .contains("as published")
                .doesNotContain("CLOBBERED");

            assertThat(AxPort.diagnoseAgainst(8080))
                .as("8080 is rest-assured's default and the measured P3-144 impostor")
                .contains("CLOBBERED");
        } finally {
            AxPort.restoreAfterStub();
        }
    }
}
