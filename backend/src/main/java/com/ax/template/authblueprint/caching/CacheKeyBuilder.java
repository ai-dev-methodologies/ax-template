package com.ax.template.authblueprint.caching;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * CACHE-KEY-001 — namespaced, tenant-prefixed cache keys.
 *
 * Key shape: {app}:{tenant}:{resourceType}:{resourceId}:v{version}. In multi-tenant mode a blank
 * tenant is a contract REJECT (cross-tenant cache bleed is a confidentiality defect). User-controlled
 * id segments longer than {@link #MAX_SEGMENT} bytes are SHA-256 hashed to bound the key space and
 * prevent key-space injection. Spec: specs/caching-l0.yaml#CACHE-KEY-001 (RFC 9111 §2 / §4.1).
 */
public final class CacheKeyBuilder {

    public static final String APP = "ax";
    public static final int MAX_SEGMENT = 256;

    private final boolean tenantIsolationRequired;

    public CacheKeyBuilder(boolean tenantIsolationRequired) {
        this.tenantIsolationRequired = tenantIsolationRequired;
    }

    public String build(String tenant, String resourceType, String resourceId, long version) {
        if (tenantIsolationRequired && (tenant == null || tenant.isBlank())) {
            throw new IllegalArgumentException(
                "CACHE-KEY-001: tenant component is required in multi-tenant mode — refusing to build a "
                    + "tenant-agnostic key (would allow cross-tenant cache bleed)");
        }
        String t = (tenant == null || tenant.isBlank()) ? "_" : bound(tenant);
        return String.join(":", APP, t, resourceType, bound(resourceId), "v" + version);
    }

    /** Segments over MAX_SEGMENT bytes are replaced by their SHA-256 hex digest (bounded, collision-free). */
    private static String bound(String segment) {
        byte[] raw = segment.getBytes(StandardCharsets.UTF_8);
        if (raw.length <= MAX_SEGMENT) {
            return segment;
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
