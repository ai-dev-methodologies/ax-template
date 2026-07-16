package com.ax.template.authblueprint.idempotency;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * IDEMPOTENCY-PAYLOAD-001 — request fingerprint = SHA-256 (FIPS 180-4) of the canonicalized
 * request: {@code method + path + sorted query params + body}. Headers are EXCLUDED (User-Agent /
 * Date / tracing ids vary). JSON bodies are canonicalized to sorted-keys + minified so two
 * semantically-identical payloads with different key order produce the SAME fingerprint (no
 * false IDEMPOTENCY_KEY_REUSED mismatch); a non-JSON body is hashed as-is.
 *
 * <p>Anchored to FIPS 180-4 (SHA-256). Spec: specs/idempotency-l0.yaml#IDEMPOTENCY-PAYLOAD-001.
 */
public final class RequestFingerprint {

    /**
     * Pin the FULL {@link StreamReadConstraints} envelope on this hand-built standalone mapper — the
     * Boot property {@code spring.jackson.factory.constraints.read.*} binds only the auto-configured
     * mapper and does NOT reach a mapper built by hand. Jackson 3 leaves several knobs UNBOUNDED by
     * default (verified against jackson-core-3.1.4: {@code DEFAULT_MAX_TOKEN_COUNT=-1},
     * {@code DEFAULT_MAX_DOC_LEN=-1}; string default raised 20M→100M), so without these pins a sub-20MB
     * token-dense body like {@code {"x":[{},{},…]}} would canonicalize into millions of maps → heap
     * exhaustion. These literals mirror the application.yml properties for the auto-config mapper.
     */
    static final int MAX_STRING_LEN = 20_000_000;
    /** 1M tokens — Jackson default is -1 (unbounded). A token-dense body is aborted before materializing. */
    static final long MAX_TOKEN_COUNT = 1_000_000L;
    /** 20M bytes — Jackson default is -1 (unbounded). Matches the transport-level body cap. */
    static final long MAX_DOCUMENT_LEN = 20_000_000L;

    // SORT_KEYS so {"a":1,"b":2} and {"b":2,"a":1} canonicalize identically.
    private static final ObjectMapper CANONICAL = JsonMapper.builder(
            JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxStringLength(MAX_STRING_LEN)
                            .maxTokenCount(MAX_TOKEN_COUNT)
                            .maxDocumentLength(MAX_DOCUMENT_LEN)
                            .build())
                    .build())
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private RequestFingerprint() {}

    public static String of(String method, String path, String sortedQuery, String body) {
        String canonicalBody = canonicalizeJson(body);
        String canonical = method.toUpperCase(Locale.ROOT) + '\n'
                + path + '\n'
                + (sortedQuery == null ? "" : sortedQuery) + '\n'
                + canonicalBody;
        return sha256Hex(canonical);
    }

    /** Re-emit JSON with keys sorted + minified; leave non-JSON (or blank) untouched. */
    private static String canonicalizeJson(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            // Read into generic Maps/Lists (NOT JsonNode) so ORDER_MAP_ENTRIES_BY_KEYS actually
            // sorts object keys on re-serialization; array element order is preserved (significant).
            Object tree = CANONICAL.readValue(body, Object.class);
            return CANONICAL.writeValueAsString(tree);
        } catch (Exception notJson) {
            return body; // binary / non-JSON: hash the raw bytes
        }
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e); // FIPS 180-4 mandated on every JRE
        }
    }
}
