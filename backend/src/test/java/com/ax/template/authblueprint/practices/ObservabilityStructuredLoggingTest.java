package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

@Tag("PRACTICES")
@Tag("PRACTICES-OBS-001")
class ObservabilityStructuredLoggingTest {

    private Logger log;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        log = (Logger) LoggerFactory.getLogger(getClass());
        appender = new ListAppender<>();
        appender.start();
        log.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(appender);
    }

    @Test
    void practices_OBS_001_loggerEmitsKeyValuePairs() {
        log.atInfo()
                .addKeyValue("order_id", "ord-123")
                .addKeyValue("amount", 99)
                .setMessage("order processed")
                .log();

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        List<KeyValuePair> kvps = event.getKeyValuePairs();
        assertThat(kvps).isNotNull();
        assertThat(kvps).extracting("key").contains("order_id", "amount");
        // Free-form message stays as text; the structured fields ride along separately so
        // a JSON encoder can serialise them into top-level keys for log search.
        assertThat(event.getMessage()).isEqualTo("order processed");
    }

    @Test
    void practices_OBS_001_concatenatedLogIsAntiPattern() {
        // The anti-pattern: building one big string with no machine-readable keys.
        log.info("order processed order_id=ord-456 amount=42");

        ILoggingEvent event = appender.list.get(appender.list.size() - 1);
        // No key/value pairs were emitted — the structured-logging contract is not met.
        List<KeyValuePair> kvps = event.getKeyValuePairs();
        assertThat(kvps == null || kvps.isEmpty())
                .as("string-concatenated logs carry NO key-value pairs — exactly why this is anti-pattern")
                .isTrue();
    }
}
