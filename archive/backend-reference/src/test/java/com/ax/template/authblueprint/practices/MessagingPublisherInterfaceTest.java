package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-MESSAGING-001")
class MessagingPublisherInterfaceTest {

    @Test
    void practices_MESSAGING_001_servicePublisherFieldsDeclareInterfaceTypeNotImplementation() throws Exception {
        // Service-layer publishers must depend on an abstract MessagePublisher interface,
        // never a concrete KafkaTemplate / RabbitTemplate / InMemoryMessagePublisher. The
        // service must not know which broker (or no broker) is wired in — that's the
        // entire reason the abstraction exists. A concrete-typed field couples the
        // domain to the broker SDK and makes broker swaps a refactor, not a config flip.
        Field f = OrderEventPublisher.class.getDeclaredField("publisher");
        Class<?> declared = f.getType();
        assertThat(declared)
                .as("OrderEventPublisher.publisher must be typed as the abstract MessagePublisher interface, not a concrete impl")
                .isEqualTo(MessagePublisher.class);
        assertThat(declared.isInterface())
                .as("the declared type must be an interface so the broker is swappable")
                .isTrue();
    }
}
