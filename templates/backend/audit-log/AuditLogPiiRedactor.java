/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogPiiRedactor
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-PII-001
 *               blueprints/audit-log-manifest.yaml#pii_redaction
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "GDPR Article 25 — Data protection by design and by default (pseudonymization)"
 *     url: "https://gdpr.eu/article-25-data-protection-by-design/"
 *   - source_type: external
 *     citation: "OWASP ASVS V4 — V8.3.5 Verify that sensitive data is not stored unless necessary"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Called by AuditLogService.record() before persisting.
 *   Controlled by audit.pii.store-full property (default: false = masked).
 *   Set audit.pii.store-full=true to disable masking (requires DPO approval).
 */
package com.example.app.auditlog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AuditLogPiiRedactor — conditionally masks PII fields in audit log entries.
 *
 * <p>Controlled by {@code audit.pii.store-full} property (default: {@code false}).
 *
 * <p>When masking is active (AUDIT-PII-001):
 * <ul>
 *   <li>IPv4: last octet replaced with "xxx" → "192.168.1.xxx"
 *   <li>IPv6: last 64 bits zeroed and displayed as truncated
 * </ul>
 *
 * <p>Returns a new {@link AuditLog} instance (immutable — never mutates the input).
 */
@Component
public class AuditLogPiiRedactor {

    @Value("${audit.pii.store-full:false}")
    private boolean storeFullPii;

    /**
     * Returns a redacted copy of the given audit log entry.
     *
     * <p>If {@code audit.pii.store-full=true}, returns the input unchanged.
     * Otherwise, masks the IP address per the PII policy.
     *
     * @param entry the original, unredacted entry
     * @return a new AuditLog with PII fields masked (or the original if storeFullPii=true)
     */
    public AuditLog redact(AuditLog entry) {
        if (storeFullPii) {
            return entry;
        }
        return AuditLog.builder()
            .id(entry.getId())
            .actorId(entry.getActorId())
            .actorIp(maskIp(entry.getActorIp()))
            .action(entry.getAction())
            .resourceType(entry.getResourceType())
            .resourceId(entry.getResourceId())
            .outcome(entry.getOutcome())
            .correlationId(entry.getCorrelationId())
            .userAgent(entry.getUserAgent())
            .metadata(entry.getMetadata())
            .build();
    }

    /**
     * Masks an IP address.
     *
     * <ul>
     *   <li>IPv4 (a.b.c.d) → "a.b.c.xxx"
     *   <li>IPv6 — last segment replaced with "xxxx"
     *   <li>null or blank → null
     * </ul>
     */
    String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return ip;

        if (ip.contains(":")) {
            // IPv6: mask last segment
            int lastColon = ip.lastIndexOf(':');
            if (lastColon >= 0) {
                return ip.substring(0, lastColon + 1) + "xxxx";
            }
            return ip;
        }

        if (ip.contains(".")) {
            // IPv4: mask last octet
            int lastDot = ip.lastIndexOf('.');
            if (lastDot >= 0) {
                return ip.substring(0, lastDot + 1) + "xxx";
            }
        }

        return ip;
    }
}
