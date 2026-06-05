package com.ax.template.authblueprint.webhooksigning;

import java.util.ArrayList;
import java.util.List;

/**
 * WHSIGN-HEADER-001 — the structured, versioned signature header parsed into discrete fields:
 * {@code Webhook-Signature: t=<unix_ts>,v1=<hex(HMAC-SHA256)>}. Multiple {@code v1=} values MAY appear
 * (rotation overlap, WHSIGN-SECRET-001). The {@code v1} version token gates future algorithm migration
 * (a {@code v2} field is parsed-but-ignored so it can coexist; only an UNKNOWN scheme token or a
 * missing {@code t}/{@code v1} is fatal).
 *
 * <p>An unparseable or version-unknown header is rejected by {@link #parse} with a
 * {@link WebhookSigningException.Kind#MALFORMED} (→ 400 WEBHOOK_SIGNATURE_MALFORMED). Spec:
 * specs/webhook-signing-l0.yaml#WHSIGN-HEADER-001.
 */
public record SignatureHeader(long timestamp, List<String> v1Signatures) {

    /** Recognised scheme tokens; anything else → MALFORMED so a future migration is explicit. */
    private static final String KEY_TIMESTAMP = "t";
    private static final String KEY_V1 = "v1";
    private static final String KEY_V2 = "v2"; // reserved for future algo migration (parsed, ignored)

    public SignatureHeader {
        v1Signatures = List.copyOf(v1Signatures);
    }

    static SignatureHeader parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw malformed();
        }
        Long ts = null;
        List<String> v1 = new ArrayList<>();
        for (String part : raw.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int eq = token.indexOf('=');
            if (eq <= 0 || eq == token.length() - 1) {
                throw malformed(); // no key, no value, or empty side
            }
            String key = token.substring(0, eq).trim();
            String value = token.substring(eq + 1).trim();
            switch (key) {
                case KEY_TIMESTAMP -> {
                    try {
                        ts = Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        throw malformed();
                    }
                }
                case KEY_V1 -> v1.add(value);
                case KEY_V2 -> { /* reserved future scheme — coexists, not yet verified */ }
                default -> throw malformed(); // unknown scheme token
            }
        }
        if (ts == null || v1.isEmpty()) {
            throw malformed(); // both 't' and at least one 'v1' are required
        }
        return new SignatureHeader(ts, v1);
    }

    private static WebhookSigningException malformed() {
        return new WebhookSigningException(WebhookSigningException.Kind.MALFORMED,
                "The webhook signature header could not be parsed.");
    }
}
