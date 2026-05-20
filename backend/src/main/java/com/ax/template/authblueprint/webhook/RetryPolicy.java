package com.ax.template.authblueprint.webhook;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Exponential-backoff retry policy.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-RETRY-001 — {@code 30s × 2^(attemptCount-1)} schedule, max 5 attempts.</li>
 *   <li>blueprints/webhook-manifest.yaml#retry.retry_classification — 4xx (except 408/429) are
 *       permanent; 5xx + network errors are retriable.</li>
 * </ul>
 */
@Component
public class RetryPolicy {

    /** Initial backoff window before attempt 2 — 30 seconds. */
    public static final Duration INITIAL_DELAY = Duration.ofSeconds(30);
    /** Multiplier between attempts — doubles each retry. */
    public static final double MULTIPLIER = 2.0;
    /** First attempt counts as 1; permanent transition fires after this many. */
    public static final int MAX_ATTEMPTS = WebhookDelivery.MAX_ATTEMPTS;

    /**
     * 408 / 429 — caller can succeed by waiting; otherwise 4xx is treated as
     * permanent (caller-side fix needed) per manifest.
     */
    private static final Set<Integer> RETRIABLE_4XX = Set.of(408, 429);

    /**
     * Schedule for the {@code nextAttempt}-th attempt — i.e. after a failure on
     * attempt {@code attemptCount}, the next attempt index is {@code attemptCount + 1}
     * and is delayed by {@code INITIAL_DELAY × MULTIPLIER^(nextAttempt - 2)}.
     *
     * <p>So for attempts {@code 1 → 2 → 3 → 4 → 5}, delays are 30s, 60s, 120s, 240s.
     */
    public Instant nextAttemptAt(int attemptCount, Instant now) {
        if (attemptCount < 1) {
            throw new IllegalArgumentException("attemptCount must be >= 1");
        }
        long seconds = (long) (INITIAL_DELAY.toSeconds() * Math.pow(MULTIPLIER, attemptCount - 1));
        return now.plusSeconds(seconds);
    }

    /**
     * @return {@code true} if a response code or network failure should be retried,
     *     {@code false} for permanent classifications (4xx caller-side errors).
     */
    public boolean isRetriable(Integer responseCode) {
        if (responseCode == null) {
            // network timeout / connection refused — treated as retriable per manifest
            return true;
        }
        if (responseCode >= 500 && responseCode < 600) {
            return true;
        }
        if (responseCode >= 400 && responseCode < 500) {
            return RETRIABLE_4XX.contains(responseCode);
        }
        return false;
    }

    /**
     * @return {@code true} once the retry budget is exhausted — caller transitions
     *     the delivery to {@link WebhookDeliveryStatus#FAILED_PERMANENT}.
     */
    public boolean isExhausted(int attemptCount) {
        return attemptCount >= MAX_ATTEMPTS;
    }
}
