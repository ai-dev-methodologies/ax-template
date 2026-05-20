package com.ax.template.authblueprint.auditlog;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PII family (1 item).
 * <ul>
 *   <li>AUDIT-PII-001 — PII redaction by default; opt-in to full storage via audit.pii.store-full</li>
 * </ul>
 */
class AuditLogPiiTest {

    @Test
    @Tag("AUDIT_LOG")
    @Tag("AUDIT-PII-001")
    void pii_001_ipMaskedByDefaultAndFullWhenOptedIn() {
        // Default: store-full=false → IP last octet masked.
        AuditLogProperties masked = new AuditLogProperties();
        masked.getPii().setStoreFull(false);
        AuditLogPiiRedactor maskedRedactor = new AuditLogPiiRedactor(masked);

        assertThat(maskedRedactor.redactIp("192.168.1.100"))
            .as("IPv4 last octet must be masked when store-full=false")
            .isEqualTo("192.168.1.xxx");

        // Opt-in: store-full=true → IP stored verbatim.
        AuditLogProperties full = new AuditLogProperties();
        full.getPii().setStoreFull(true);
        AuditLogPiiRedactor fullRedactor = new AuditLogPiiRedactor(full);

        assertThat(fullRedactor.redactIp("192.168.1.100"))
            .as("IPv4 must be stored verbatim when store-full=true")
            .isEqualTo("192.168.1.100");

        // Email + username redaction also follow the manifest mask patterns.
        assertThat(maskedRedactor.redactEmail("john.doe@example.com"))
            .isEqualTo("j***@example.com");
        assertThat(maskedRedactor.redactUsername("jdoe123"))
            .isEqualTo("jdo***");
    }
}
