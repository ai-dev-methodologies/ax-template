package com.ax.template.authblueprint.practices;

import java.util.regex.Pattern;

/**
 * Stateless utility for redacting common PII patterns from strings before they reach a
 * log sink. Use at the boundary where untrusted payloads enter log statements:
 * <pre>{@code log.info("inbound payload: {}", PiiRedactor.redact(payload));}</pre>
 * Patterns covered: email, US-style phone, US SSN. Add new patterns by appending a
 * Pattern + replacement pair; never silently broaden the regex (it could over-redact).
 */
public final class PiiRedactor {

    private PiiRedactor() { /* utility */ }

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}\\b");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    public static String redact(String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        String s = EMAIL.matcher(input).replaceAll("[redacted-email]");
        s = SSN.matcher(s).replaceAll("[redacted-ssn]");
        s = PHONE.matcher(s).replaceAll("[redacted-phone]");
        return s;
    }
}
