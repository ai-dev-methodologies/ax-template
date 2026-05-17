package com.ax.template.authblueprint.practices;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Test-only MessagePublisher impl. Production wiring uses SpringEventMessagePublisher
// (@Primary). This class is intentionally NOT a @Component — tests instantiate it
// directly to inspect what would have been published.
public class InMemoryMessagePublisher implements MessagePublisher {

    public record Published(String topic, Object payload) {}

    private final List<Published> log = new CopyOnWriteArrayList<>();

    @Override
    public void publish(String topic, Object payload) {
        log.add(new Published(topic, payload));
    }

    public List<Published> published() {
        return List.copyOf(log);
    }

    public void clear() {
        log.clear();
    }
}
