package com.ax.template.authblueprint.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, List<Instant>> attempts = new ConcurrentHashMap<>();

    public synchronized boolean isRateLimited(String email) {
        List<Instant> list = attempts.getOrDefault(email.toLowerCase(Locale.ROOT), List.of());
        Instant cutoff = Instant.now().minus(WINDOW);
        long recentAttempts = list.stream().filter(t -> t.isAfter(cutoff)).count();
        return recentAttempts >= MAX_ATTEMPTS;
    }

    public synchronized void recordFailedAttempt(String email) {
        attempts.computeIfAbsent(email.toLowerCase(Locale.ROOT), k -> new ArrayList<>())
                .add(Instant.now());
    }

    public synchronized void clearAttempts(String email) {
        attempts.remove(email.toLowerCase(Locale.ROOT));
    }
}
