package com.ax.template.authblueprint.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateStore {

    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private final ConcurrentHashMap<String, Instant> stateMap = new ConcurrentHashMap<>();

    public String generateState() {
        cleanup();
        String state = UUID.randomUUID().toString();
        stateMap.put(state, Instant.now().plus(STATE_TTL));
        return state;
    }

    public boolean validateAndConsume(String state) {
        Instant expiry = stateMap.remove(state);
        return expiry != null && expiry.isAfter(Instant.now());
    }

    private void cleanup() {
        Instant now = Instant.now();
        stateMap.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
