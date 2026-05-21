package com.ax.template.authblueprint.apikey;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for {@link ApiKeyHasher}.
 *
 * <p>Trace: KEY-STORAGE-001 — SHA-256 hex digest + constant-time comparison.
 */
@Tag("API_KEY")
class ApiKeyHasherTest {

    @Test
    @Tag("KEY-STORAGE-001")
    void hash_isSha256LowercaseHex_matchingJdkDigest() throws Exception {
        String plaintext = "ak_test-value-123";

        String our = ApiKeyHasher.hash(plaintext);
        byte[] expected = MessageDigest.getInstance("SHA-256")
            .digest(plaintext.getBytes(StandardCharsets.UTF_8));
        String expectedHex = HexFormat.of().formatHex(expected);

        assertThat(our).isEqualTo(expectedHex);
        assertThat(our).hasSize(64);
        assertThat(our).matches("[0-9a-f]{64}");
    }

    @Test
    @Tag("KEY-STORAGE-001")
    void matches_returnsTrueForCorrectPlaintext() {
        String plaintext = ApiKeyHasher.newPlaintext();
        String hash = ApiKeyHasher.hash(plaintext);

        assertThat(ApiKeyHasher.matches(plaintext, hash)).isTrue();
    }

    @Test
    @Tag("KEY-STORAGE-001")
    void matches_returnsFalseForWrongPlaintext() {
        String plaintext = ApiKeyHasher.newPlaintext();
        String hash = ApiKeyHasher.hash(plaintext);

        assertThat(ApiKeyHasher.matches("ak_imposter", hash)).isFalse();
    }

    @Test
    @Tag("KEY-STORAGE-001")
    void matches_returnsFalseForMalformedHex() {
        String plaintext = "ak_anything";

        assertThat(ApiKeyHasher.matches(plaintext, "not-hex-at-all")).isFalse();
        assertThat(ApiKeyHasher.matches(plaintext, "")).isFalse();
        assertThat(ApiKeyHasher.matches(plaintext, null)).isFalse();
        assertThat(ApiKeyHasher.matches(null, ApiKeyHasher.hash(plaintext))).isFalse();
    }

    @Test
    @Tag("KEY-AUTHN-001")
    void newPlaintext_carriesPrefixAndHighEntropy() {
        String a = ApiKeyHasher.newPlaintext();
        String b = ApiKeyHasher.newPlaintext();

        assertThat(a).startsWith(ApiKeyHasher.VALUE_PREFIX);
        assertThat(a).hasSizeGreaterThanOrEqualTo(ApiKeyHasher.HASH_PREFIX_LENGTH);
        assertThat(a).isNotEqualTo(b);
    }
}
