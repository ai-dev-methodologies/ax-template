package com.ax.template.authblueprint.auditlog;

import org.springframework.stereotype.Component;

/**
 * Masks PII fields before persistence.
 * <p>
 * Trace: AUDIT-PII-001.
 * Manifest: {@code blueprints/audit-log-manifest.yaml#pii_redaction}.
 *
 * <ul>
 *   <li>IPv4: replace the last octet with {@code "xxx"} — e.g.
 *       {@code 192.168.1.100} → {@code 192.168.1.xxx}</li>
 *   <li>IPv6: keep the first 4 hextets, mask the rest</li>
 *   <li>Email: keep first char + {@code "***"} + {@code "@" + domain}</li>
 *   <li>Username: keep first 3 chars + {@code "***"}</li>
 * </ul>
 */
@Component
public class AuditLogPiiRedactor {

    private final AuditLogProperties properties;

    public AuditLogPiiRedactor(AuditLogProperties properties) {
        this.properties = properties;
    }

    public String redactIp(String ip) {
        if (properties.getPii().isStoreFull() || ip == null || ip.isBlank()) {
            return ip;
        }
        if (ip.contains(".")) {
            int lastDot = ip.lastIndexOf('.');
            return ip.substring(0, lastDot) + ".xxx";
        }
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(i < 4 ? parts[i] : "xxxx");
            }
            return sb.toString();
        }
        return ip;
    }

    public String redactEmail(String email) {
        if (properties.getPii().isStoreFull() || email == null) return email;
        int at = email.indexOf('@');
        if (at <= 0) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    public String redactUsername(String username) {
        if (properties.getPii().isStoreFull() || username == null) return username;
        if (username.length() <= 3) return username + "***";
        return username.substring(0, 3) + "***";
    }
}
