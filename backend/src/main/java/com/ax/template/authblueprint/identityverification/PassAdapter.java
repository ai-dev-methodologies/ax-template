package com.ax.template.authblueprint.identityverification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PASS provider adapter — IDV-PROVIDER-001.
 *
 * <p>Maps PASS native payload (snake_case {@code ci_value}, {@code di_value},
 * {@code user_name}, {@code birth_date}) to the canonical
 * {@link VerifiedIdentityData}. PASS-specific {@code carrier} goes to
 * {@code metadata}.
 *
 * <p>The field names match the synthetic payload shape used by
 * IdentityVerificationFlowIT — the catalog promises only the canonical output,
 * so a real PASS integration may need a thin field-name map at the adapter
 * boundary. The point is the controller stays provider-agnostic.
 */
@Component
public class PassAdapter implements IdentityVerificationProvider {

    private final ObjectMapper objectMapper;

    public PassAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() { return "pass"; }

    @Override
    public VerifiedIdentityData extract(byte[] rawBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (JsonProcessingException ex) {
            throw new IdentityVerificationException(
                IdentityVerificationException.Reason.EXTRACTION_FAIL,
                "PASS payload not valid JSON", ex);
        } catch (java.io.IOException ex) {
            throw new IdentityVerificationException(
                IdentityVerificationException.Reason.EXTRACTION_FAIL,
                "PASS payload read failure", ex);
        }
        String ci = textOrThrow(root, "ci_value");
        String di = textOrThrow(root, "di_value");
        String name = textOr(root, "user_name", "");
        String dob = textOr(root, "birth_date", "");
        Instant verifiedAt = parseTimestamp(root, "verified_at");

        Map<String, String> metadata = new LinkedHashMap<>();
        addIfPresent(root, "carrier", metadata);
        addIfPresent(root, "request_id", metadata);

        return new VerifiedIdentityData(ci, di, name, dob, verifiedAt,
                providerName(), metadata);
    }

    private static String textOrThrow(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull() || n.asText().isBlank()) {
            throw new IdentityVerificationException(
                IdentityVerificationException.Reason.EXTRACTION_FAIL,
                "PASS payload missing field: " + field);
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
