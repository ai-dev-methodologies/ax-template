package com.ax.template.authblueprint.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PasswordEncoderTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void encode_producesHashDifferentFromRaw() {
        String raw = "mySecurePassword123";
        String hash = passwordEncoder.encode(raw);
        assertThat(hash).isNotEqualTo(raw);
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void matches_returnsTrueForCorrectPassword() {
        String raw = "mySecurePassword123";
        String hash = passwordEncoder.encode(raw);
        assertThat(passwordEncoder.matches(raw, hash)).isTrue();
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String raw = "mySecurePassword123";
        String hash = passwordEncoder.encode(raw);
        assertThat(passwordEncoder.matches("wrongPassword", hash)).isFalse();
    }
}
