package com.ax.template.authblueprint.auditlog;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit-log configuration bound from {@code audit.*} properties.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-RETENTION-002 — per-resource-type tier mapping</li>
 *   <li>AUDIT-PII-001 — PII redaction switch ({@code audit.pii.store-full})</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "audit")
public class AuditLogProperties {

    private final Retention retention = new Retention();
    private final Pii pii = new Pii();

    public Retention getRetention() { return retention; }
    public Pii getPii() { return pii; }

    public RetentionTier tierFor(String resourceType) {
        if (resourceType == null) return retention.getDefaultTier();
        return retention.getResource().getOrDefault(resourceType, retention.getDefaultTier());
    }

    public static class Retention {
        /** Tier applied when the resource type is not explicitly mapped. */
        private RetentionTier defaultTier = RetentionTier.STANDARD;
        /** Resource-type → tier mapping. */
        private Map<String, RetentionTier> resource = new HashMap<>();
        /** WARN threshold for the purge job (AUDIT-RETENTION-003). */
        private int warningThreshold = 10_000;

        public RetentionTier getDefaultTier() { return defaultTier; }
        public void setDefaultTier(RetentionTier v) { this.defaultTier = v; }

        public Map<String, RetentionTier> getResource() { return resource; }
        public void setResource(Map<String, RetentionTier> v) {
            this.resource = v == null ? new HashMap<>() : new HashMap<>(v);
        }

        public int getWarningThreshold() { return warningThreshold; }
        public void setWarningThreshold(int v) { this.warningThreshold = v; }

        public Map<String, RetentionTier> resourceTiersView() {
            return Collections.unmodifiableMap(resource);
        }
    }

    public static class Pii {
        /** When {@code false} (default), PII fields are stored masked. */
        private boolean storeFull = false;

        public boolean isStoreFull() { return storeFull; }
        public void setStoreFull(boolean v) { this.storeFull = v; }
    }
}
