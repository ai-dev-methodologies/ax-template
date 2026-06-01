---
title: Raw PII (IP, User-Agent, credentials) stored on entity for forensics but masked at DTO boundary
impact: HIGH
impactDescription: "Excessive Data Exposure (OWASP API3) is leaking what was collected for forensics directly into client responses"
tags:
  - privacy
  - dto
  - data-exposure
  - audit
spec_ref: "specs/session-management-l0.yaml#SESS-INTROSPECT-002"
verification:
  gradle_task: testSessionManagement
  tag: SESS-INTROSPECT-002
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
  - "https://gdpr-info.eu/art-25-gdpr/"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Level Authorization (replaces 2019's Excessive Data Exposure)"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
    quote: "Unauthorized access to private/sensitive object properties may result in data disclosure, data loss, or data corruption."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "GDPR Article 25 — Data protection by design and by default"
    url: "https://gdpr-info.eu/art-25-gdpr/"
    quote: "The controller shall, both at the time of the determination of the means for processing and at the time of the processing itself, implement appropriate technical and organisational measures, such as pseudonymisation."
    quoted_at: "2026-05-22"
---

## Raw PII stored on entity for forensics but masked at DTO boundary

**Impact: HIGH — OWASP API3 Excessive Data Exposure is leaking what forensics needed**

The honest forensic posture often requires storing more than the API should ever return: the full IP, the full User-Agent string, the storage key for a file blob, the hashed API key digest. These belong on the entity row for after-the-fact investigation. They MUST NOT belong on the DTO that a client sees. The catalog convention: the raw column carries `@JsonIgnore` so accidental entity serialization cannot leak it; the DTO carries only a masked or summarized form (`ipAddressMasked`, `userAgentSummary`, `prefix`, never the storage key).

Examples in the catalog:
- **session-management (R33)**: `SessionRecord.ipAddress` and `SessionRecord.userAgent` are `@JsonIgnore`; `SessionResponse` carries `ipAddressMasked` (last octet → "xxx") and `userAgentSummary` ("Chrome on Windows"). The full UA never leaves the server.
- **api-key (R30)**: `ApiKey.hashedValue` is `@JsonIgnore`; `ApiKeyResponse` carries `prefix` (first 8 chars of the plaintext) for display, never the hash. Plaintext is returned exactly once at creation.
- **file-storage**: `StoredFile.storageKey` is `@JsonIgnore` (the opaque internal UUID); the DTO uses only `id` for client references.

**Incorrect — raw fields reach DTO directly:**

```java
@Entity
public class SessionRecord {
    private String ipAddress;       // 203.0.113.42 — full IP visible in any response
    private String userAgent;       // full UA string — fingerprinting vector

    // Jackson serializes both fields verbatim
}
```

A single endpoint returning the entity, or a list endpoint forgetting to map to a DTO, leaks the full PII. The 2019 OWASP "Excessive Data Exposure" entry was renamed in 2023 to "Broken Object Property Level Authorization" — the root cause is the same: per-property authorization was skipped because the developer trusted that *some* entity-level check would catch it.

**Correct — entity carries raw + @JsonIgnore, DTO carries masked:**

```java
@Entity
public class SessionRecord {
    @JsonIgnore                                       // never serialized verbatim
    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @JsonIgnore
    @Column(name = "user_agent", updatable = false)
    private String userAgent;

    @JsonIgnore public String getIpAddress() { return ipAddress; }
    @JsonIgnore public String getUserAgent() { return userAgent; }
}

public record SessionResponse(
    UUID id,
    String ipAddressMasked,       // "203.0.113.xxx" — last octet redacted
    String userAgentSummary,      // "Chrome on Windows"
    // …
) {
    public static SessionResponse from(SessionRecord s, Clock clock) {
        return new SessionResponse(
            s.getId(),
            IpAddressMasker.mask(s.getIpAddress()),
            UserAgentSummarizer.summarize(s.getUserAgent()),
            // …
        );
    }
}
```

The structural defense — `@JsonIgnore` plus a separate DTO — means a developer cannot accidentally serialize the entity directly without first being forced to acknowledge the missing fields. The masker / summarizer encapsulates the redaction policy, so consistency across endpoints is mechanical rather than reviewer-dependent.

**Apply this pattern when**: storing IP / User-Agent / device fingerprint / credential digest / opaque internal key on an entity that may also produce read-side DTOs. The forensic value justifies keeping the raw column; the privacy posture forbids returning it.

Reference: [OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/)

Reference: [GDPR Article 25 — Data protection by design and by default](https://gdpr-info.eu/art-25-gdpr/)
