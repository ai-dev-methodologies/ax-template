package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-OBS-003")
class ObservabilityNoPiiInLogsTest {

    @Test
    void practices_OBS_003_redactsEmailAddresses() {
        String input = "support contacted user alice@example.com about order 99";
        String redacted = PiiRedactor.redact(input);
        assertThat(redacted)
                .doesNotContain("alice@example.com")
                .contains("[redacted-email]");
    }

    @Test
    void practices_OBS_003_redactsPhoneNumbers() {
        String input = "callback 415-555-0100 scheduled";
        String redacted = PiiRedactor.redact(input);
        assertThat(redacted)
                .doesNotContain("415-555-0100")
                .contains("[redacted-phone]");
    }

    @Test
    void practices_OBS_003_redactsSsnAndKeepsSafeContent() {
        String input = "verified id 123-45-6789 for customer";
        String redacted = PiiRedactor.redact(input);
        assertThat(redacted)
                .doesNotContain("123-45-6789")
                .contains("[redacted-ssn]")
                .contains("verified id")
                .contains("for customer");
    }

    @Test
    void practices_OBS_003_passThroughForCleanStrings() {
        String input = "no sensitive data here";
        assertThat(PiiRedactor.redact(input)).isEqualTo(input);
    }
}
