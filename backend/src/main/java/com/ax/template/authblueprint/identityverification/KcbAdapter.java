package com.ax.template.authblueprint.identityverification;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KCB provider adapter — IDV-PROVIDER-001.
 *
 * <p>Maps KCB native payload ({@code connecting_info}, {@code duplicate_info},
 * {@code user_name}, {@code birth_day}) to the canonical
 * {@link VerifiedIdentityData}. KCB-specific {@code response_seq} and
 * {@code site_code} go to {@code metadata}.
 *
 * <p>IDV-PROVIDER-001 invariant: the canonical fields produced here MUST
 * match {@link PassAdapter} exactly — same field names, same types. No KCB-
 * specific field bleeds into named slots.
 */
@Component
public class KcbAdapter implements IdentityVerificationProvider {

    private final ObjectMapper objectMapper;

    public KcbAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() { return "kcb"; }

    @Override
    public VerifiedIdentityData extract(byte[] rawBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (JacksonException ex) {
            throw new IdentityVerificationException(
                IdentityVerificationException.Reason.EXTRACTION_FAIL,
                "KCB payload not valid JSON", ex);
        }
        String ci = textOrThrow(root, "connecting_info");
        String di = textOrThrow(root, "duplicate_info");
        String name = textOr(root, "user_name", "");
        String dob = textOr(root, "birth_day", "");
        Instant verifiedAt = parseTimestamp(root, "verified_at");

        Map<String, String> metadata = new LinkedHashMap<>();
        addIfPresent(root, "response_seq", metadata);
        addIfPresent(root, "site_code", metadata);
        addIfPresent(root, "auth_type", metadata);

        return new VerifiedIdentityData(ci, di, name, dob, verifiedAt,
                providerName(), metadata);
    }

    private static String textOrThrow(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull() || n.asText().isBlank()) {
            throw new IdentityVerificationException(
                IdentityVerificationException.Reason.EXTRACTION_FAIL,
                "KCB payload missing field: " + field);
        }
        return n.asText();
    }

    private static String textOr(JsonNode root, String field, String fallback) {
        JsonNode n = root.get(field);
        return (n == null || n.isNull()) ? fallback : n.asText();
    }

    private static Instant parseTimestamp(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull() || n.asText().isBlank()) return Instant.now();
        try {
            return Instant.parse(n.asText());
        } catch (Exception ex) {
            return Instant.now();
        }
    }

    private static void addIfPresent(JsonNode root, String key, Map<String, String> out) {
        JsonNode n = root.get(key);
        if (n != null && !n.isNull() && !n.asText().isBlank()) {
            out.put(key, n.asText());
        }
    }
}
