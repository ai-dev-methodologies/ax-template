package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-MESSAGING-002")
class MessagingPayloadRecordTest {

    @Test
    void practices_MESSAGING_002_eventPayloadIsImmutableRecord() {
        // Event payloads cross process boundaries (eventually) and are retained in queue
        // logs, dead-letter topics, and audit trails — any mutation downstream would be
        // applied to a copy that disagrees with the one the broker actually delivered.
        // Records are by-construction immutable (all components are final, no setters),
        // which is exactly the contract a transport-layer payload needs.
        assertThat(OrderPlacedEvent.class.isRecord())
                .as("OrderPlacedEvent must be a Java record (immutable by construction)")
                .isTrue();
        for (Field f : OrderPlacedEvent.class.getDeclaredFields()) {
            assertThat(Modifier.isFinal(f.getModifiers()))
                    .as("record component %s must be final", f.getName())
                    .isTrue();
        }
    }
}
