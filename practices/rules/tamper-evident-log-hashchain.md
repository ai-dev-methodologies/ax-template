---
title: Tamper-EVIDENT logs MUST hash-chain each entry to its predecessor — app-immutability alone is not tamper-evidence
impact: HIGH
impactDescription: "A log marked @Column(updatable=false) only stops the APPLICATION from rewriting rows; a DBA, a compromised service account, or anyone with raw table access can still UPDATE a historical row and nobody can tell. Chain-of-custody, audit attestation, and compliance trails require that a retroactive edit be DETECTABLE, not merely 'not done by our code'."
tags:
  - audit
  - integrity
  - tamper-evidence
  - chain-of-custody
  - hash-chain
  - append-only
spec_ref: "specs/tamper-evident-log-l0.yaml#TAMPER-HASHCHAIN-001"
verification:
  type: review
  source: "backend tamper-evident log writer (TamperEvidentLogService) + entity mapping"
  pattern: "Each entry persists content_hash + prev_entry_hash (SHA-256+), chain_hash = H(prev_entry_hash || content_hash); hash columns @Column(updatable=false); a verify routine recomputes the chain from genesis and detects content-mismatch / gap / reorder; the head is anchored externally."
upstream:
  - "https://datatracker.ietf.org/doc/html/rfc6962"
  - "https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-92.pdf"
evidence:
  - source_type: external
    citation: "RFC 6962 — Certificate Transparency, §3 Log Format and Operation"
    url: "https://datatracker.ietf.org/doc/html/rfc6962"
    quote: "A log is a single, ever-growing, append-only Merkle Tree of such certificates."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "RFC 6962 — Certificate Transparency, §1 Informal Introduction"
    url: "https://datatracker.ietf.org/doc/html/rfc6962"
    quote: "The append-only property of each log is technically achieved using Merkle Trees, which can be used to show that any particular version of the log is a superset of any particular previous version."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "NIST SP 800-92 — Guide to Computer Security Log Management, §5.2 Log File Integrity Checking"
    url: "https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-92.pdf"
    quote: "If the log file is modified and its message digest is recalculated, it will not match the original message digest, indicating that the file has been altered."
    quoted_at: "2026-06-01"
---

## Tamper-EVIDENT logs MUST hash-chain each entry to its predecessor — app-immutability alone is not tamper-evidence

**Impact: HIGH — `@Column(updatable=false)` is APP-immutability, not tamper-EVIDENCE. It stops your code from rewriting a row; it does nothing against a DBA, a compromised service account, or anyone with raw table access who runs a plain `UPDATE`. For chain-of-custody, audit attestation, and compliance trails, the requirement is that a retroactive edit be *detectable* — not merely *not performed by the application*.**

Teams reach for an immutable audit table and assume they have a tamper-evident log. They do not. Marking every column `updatable=false` and exposing only `save()` removes the application's ability to mutate history — that is `audit-log-l0`'s `AUDIT-RECORD-002`. But the database row is still a plain row. Anyone with direct write access (a privileged DBA, a leaked service credential, a backup-restore operator, an attacker who reached the database) can `UPDATE audit_log SET payload=... WHERE id=...` and the application is none the wiser. The auditor reading the table later sees a clean, internally-consistent record of a history that was silently rewritten.

Tamper-EVIDENCE is a stronger property: any retroactive edit, deletion, or reorder must *break something verifiable*. Achieve it by hash-chaining. Each entry stores a content hash over its own canonical payload **and** the chain hash of the entry before it, with `chain_hash = H(prev_entry_hash || content_hash)` using SHA-256 or stronger. Entry N now cryptographically commits to entry N−1 and transitively to all prior entries — exactly the append-only Merkle property RFC 6962 relies on. Edit any past row and its recomputed content hash no longer matches what the next entry committed to: the chain breaks at that point and a verify pass detects it. (Add an external anchor of the head — `TAMPER-ANCHOR-001` — so even the log owner cannot rewrite-and-re-chain undetectably.)

**Incorrect — "immutable" audit table mistaken for a tamper-evident log; a raw-DB edit is undetectable:**

```java
@Entity
public class AuditEntry {
  @Id @GeneratedValue Long id;
  @Column(updatable = false) String payload;   // app cannot rewrite...
  @Column(updatable = false) Instant at;
  // ...but no link to the previous entry. A DBA UPDATE on a historical
  //    row leaves a perfectly consistent table. Nothing detects the edit.
}
```

**Correct — each entry commits to its predecessor; a retro edit breaks the chain and is detectable:**

```java
@Entity
public class TamperEvidentEntry {
  @Id @GeneratedValue Long id;
  @Column(updatable = false) String payload;
  @Column(updatable = false) Instant at;
  @Column(updatable = false, length = 64) String contentHash;     // H(canonical payload)
  @Column(updatable = false, length = 64) String prevEntryHash;   // predecessor's chainHash
  @Column(updatable = false, length = 64) String chainHash;       // H(prevEntryHash || contentHash)
}

// Sole writer reads the current head, computes the chain hash, persists once.
String contentHash = sha256(canonical(payload));
String chainHash   = sha256(prevEntryHash + contentHash);   // commits to all prior entries
// verify(): recompute from genesis; entry N.prevEntryHash MUST equal
//   recomputed chainHash of N-1, and each contentHash MUST match its payload —
//   else CONTENT_MISMATCH / CHAIN_GAP / REORDER. Compare head to the external anchor.
```

This is generic and cross-cutting: it applies to any chain-of-custody, attestation, or compliance trail, regardless of domain or jurisdiction. Keep it layered *under* an existing audit/event store — the audit table keeps recording; the hash chain plus external anchor add detectability on top.

Verification: review the entry mapping and the sole-writer/verify routine — confirm `content_hash` + `prev_entry_hash` are persisted SHA-256+, `chain_hash = H(prev || content)`, hash columns are `updatable=false`, and a genesis-rooted verify routine surfaces content-mismatch / gap / reorder rather than silently passing.

Reference: [RFC 6962 — Certificate Transparency (append-only Merkle/hash-chained log)](https://datatracker.ietf.org/doc/html/rfc6962)

Reference: [NIST SP 800-92 — Guide to Computer Security Log Management §5.2 (message-digest detection of log alteration)](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-92.pdf)
