package com.ax.template.authblueprint.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ASVS-V2.2.1 — locale-safe email key folding. {@link LoginRateLimiter} keys its per-user
 * attempt map by {@code email.toLowerCase(Locale.ROOT)}. Under the Turkish locale, a
 * default-locale {@code toLowerCase()} folds {@code 'I'} to {@code 'ı'} (dotless i) instead
 * of {@code 'i'}, so an email containing an uppercase {@code I} (e.g. {@code "USER@Inbox.com"})
 * would key differently across attempts and silently defeat the rate limiter — a legitimate
 * attacker could bypass the 5-attempts/15-min lockout simply by running under a Turkish
 * default locale. Pinning {@link Locale#ROOT} makes the fold — and therefore the lockout —
 * locale-independent.
 */
class LoginRateLimiterLocaleTest {

    private static final String EMAIL_WITH_TURKISH_SENSITIVE_I = "USER@Inbox.com";

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.2.1")
    void recordFailedAttempt_sameEmailDifferentCasing_locksOutUnderTurkishLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            LoginRateLimiter limiter = new LoginRateLimiter();

            for (int i = 0; i < 5; i++) {
                limiter.recordFailedAttempt(EMAIL_WITH_TURKISH_SENSITIVE_I);
            }

            assertThat(limiter.isRateLimited(EMAIL_WITH_TURKISH_SENSITIVE_I.toLowerCase(Locale.ROOT)))
                .as("recordFailedAttempt and isRateLimited must fold the same email to the same "
                  + "key regardless of the ambient default locale (Locale.ROOT), or the lockout "
                  + "silently fails to trigger under a Turkish default locale")
                .isTrue();
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    @Tag("ASVS")
    @Tag("ASVS-V2.2.1")
    void clearAttempts_locksOutThenClears_sameKeyUnderTurkishLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            LoginRateLimiter limiter = new LoginRateLimiter();

            for (int i = 0; i < 5; i++) {
                limiter.recordFailedAttempt(EMAIL_WITH_TURKISH_SENSITIVE_I);
            }
            limiter.clearAttempts(EMAIL_WITH_TURKISH_SENSITIVE_I);

            assertThat(limiter.isRateLimited(EMAIL_WITH_TURKISH_SENSITIVE_I))
                .as("clearAttempts must evict the same key it was recorded under, even under "
                  + "the Turkish default locale")
                .isFalse();
        } finally {
            Locale.setDefault(previous);
        }
    }
}
