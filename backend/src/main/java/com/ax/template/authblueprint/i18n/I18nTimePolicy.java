package com.ax.template.authblueprint.i18n;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * I18N-TIMEZONE-001 — UTC {@link Instant} storage + display-at-boundary policy
 * (specs/i18n-policy-l0.yaml).
 *
 * <p>Persistent time values are UTC {@link Instant}; the display layer converts to the
 * caller's {@link ZoneId} only at the API boundary. Inbound caller-supplied ISO 8601
 * date-time strings MUST carry a time-offset (trailing {@code Z} or {@code ±HH:MM})
 * per RFC 3339 §5.6 — a naive string is rejected, never silently coerced to UTC.
 *
 * <p>This is a stateless policy helper, not a bean; the {@link InvalidDateTimeException}
 * it throws is mapped to {@code 400 INVALID_DATETIME} by {@link I18nProblemAdvice}.
 */
public final class I18nTimePolicy {

    private I18nTimePolicy() {}

    /** Stable machine error code for a naive (offset-less) inbound date-time. */
    public static final String INVALID_DATETIME_CODE = "INVALID_DATETIME";

    /**
     * Parse a caller-supplied ISO 8601 date-time, REQUIRING an explicit offset.
     *
     * <p>Accepts {@code 2026-05-27T03:42:18Z} and {@code 2026-05-27T12:42:18+09:00};
     * rejects the naive {@code 2026-05-27T03:42:18} (no offset) with
     * {@link InvalidDateTimeException}. RFC 3339 §5.6 requires the time-offset; a naive
     * string is ambiguous (Seoul? New York?) and silent coercion corrupts the Instant.
     */
    public static Instant parseStrictOffset(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            throw new InvalidDateTimeException("A date-time value is required.");
        }
        try {
            // OffsetDateTime parsing rejects offset-less input — exactly the strictness
            // RFC 3339 §5.6 mandates (mirrors Jackson's default Instant deserializer).
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .parse(isoDateTime, java.time.OffsetDateTime::from)
                    .toInstant();
        } catch (DateTimeException ex) {
            throw new InvalidDateTimeException(
                    "Date-time must carry an explicit time-offset (trailing 'Z' or '±HH:MM'); "
                            + "naive values are rejected per RFC 3339 §5.6.");
        }
    }

    /** Convert a stored UTC {@link Instant} to wall-clock time in the caller's zone. */
    public static ZonedDateTime displayIn(Instant instant, ZoneId zone) {
        return instant.atZone(zone);
    }

    /**
     * Thrown when an inbound date-time string lacks a time-offset. Mapped to
     * {@code 400 INVALID_DATETIME} by {@link I18nProblemAdvice}.
     */
    public static final class InvalidDateTimeException extends RuntimeException {
        public InvalidDateTimeException(String message) {
            super(message);
        }
    }
}
