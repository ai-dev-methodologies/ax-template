---
title: An auditable deterministic procedure must record its SEED (a draw), pin its classifier VERSION (a classification), and BLIND its sensitive result fields — so a draw is replayable from the recorded seed, the same input under the same version is byte-identical, and a non-privileged caller never sees the raw blinded value
impact: HIGH
impactDescription: "A random draw with no recorded seed cannot be reproduced or audited (CWE-330) — a regulator or losing party cannot verify the selection was fair, and the result is unfalsifiable; a classification with no pinned version silently re-labels history when the classifier changes (an EMR/claims auditor can no longer reconstruct why a record was classed); and a sensitive result field with no role-blinding leaks the raw subject identity to every caller (a least-privilege violation, NIST SP 800-53). All three are the same defect: a procedure whose result is not anchored to a recorded, reproducible, role-scoped basis"
tags:
  - audit
  - determinism
  - access-control
  - governance
  - concurrency
spec_ref: "specs/reproducible-procedure-l0.yaml#PROC-DRAW-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/reproducibility/ReproducibilityService.java + backend/src/main/java/com/ax/template/authblueprint/reproducibility/Procedure.java + backend/src/main/java/com/ax/template/authblueprint/reproducibility/SeededDraw.java"
  pattern: "A draw records a server-generated SEED + algorithm + canonical SHA-256 input-set hash + selected ids, all @Column(updatable=false); the seed is generated server-side (never read from the request body) so the draw is replayable (CWE-330); replaying the recorded procedure re-runs the recorded algorithm with the recorded seed over the recorded input set and reproduces the byte-identical selection, mutating nothing (a divergence fails closed 422); a classification records the SHA-256 input hash + classifier version + resolved class, and re-classifying the same input under the same version is idempotent via uq(input_hash, classifier_version, kind) (a newer version records a SEPARATE result, never re-labeling the old one); a sensitive field is stored @JsonIgnore raw + exposed only as a deterministic masked projection to a MEMBER, with the unmasked value reachable only by ADMIN via @PreAuthorize"
upstream:
  - "https://csrc.nist.gov/glossary/term/drbg"
  - "https://csrc.nist.gov/glossary/term/least_privilege"
  - "https://cwe.mitre.org/data/definitions/330.html"
evidence:
  - source_type: external
    citation: "NIST SP 800-90A Rev. 1 — Deterministic Random Bit Generator, via the NIST CSRC Glossary: a DRBG produces a sequence of bits from a recorded seed, which is exactly what makes a recorded-seed draw reproducible and auditable after the fact"
    url: "https://csrc.nist.gov/glossary/term/drbg"
    quote: "An RBG that includes a DRBG mechanism and (at least initially) has access to a randomness source. The DRBG produces a sequence of bits from a secret initial value called a seed, along with other possible inputs."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "NIST SP 800-53 Rev. 5 — Least Privilege, via the NIST CSRC Glossary: the principle behind role-based field blinding — a MEMBER is granted only the masked view, only an ADMIN unmasks the raw value"
    url: "https://csrc.nist.gov/glossary/term/least_privilege"
    quote: "The principle that a security architecture is designed so that each entity is granted the minimum system resources and authorizations that the entity needs to perform its function."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-330: Use of Insufficiently Random Values — MITRE: a draw with no recorded, reproducible seed cannot be audited or reproduced"
    url: "https://cwe.mitre.org/data/definitions/330.html"
    quote: "The product uses insufficiently random numbers or values in a security context that depends on unpredictable numbers."
    quoted_at: "2026-06-23"
---

## An auditable procedure records its seed, pins its classifier version, and blinds its sensitive fields — it does not produce a bare, un-reproducible, fully-disclosed result

**Impact: HIGH — an un-seeded draw cannot be reproduced or audited (CWE-330); an un-versioned classification silently re-labels history; an un-blinded result leaks the raw subject identity to every caller (NIST SP 800-53 least-privilege).**

An *auditable deterministic procedure* is a draw or a classification whose result can be re-derived and audited after the fact. The discipline has three parts:

```text
draw(candidates, k):   record a SERVER-generated seed + algorithm + SHA-256(input-set) + selected ids,
                       all immutable; replay(id) re-runs the recorded algorithm with the recorded seed
                       over the recorded input set → byte-identical selection, mutating nothing
classify(input, ver):  record SHA-256(input) + classifier version + resolved class; same input + same
                       version is idempotent (uq backstop); a NEWER version records a SEPARATE result
blind(rawField):       store @JsonIgnore raw + expose only a deterministic masked projection to a MEMBER;
                       the raw value is reachable only by ADMIN (@PreAuthorize) — least privilege
```

**1. A draw records its seed (PROC-DRAW-001 / PROC-REPLAY-001).** The seed is generated server-side at draw time — never read from the request body — and recorded immutably with the algorithm, the canonical input hash, and the selected ids. Replaying the recorded procedure re-runs the recorded algorithm with the recorded seed and reproduces the byte-identical selection; a divergence fails closed (422) rather than silently overwriting. A DRBG *"produces a sequence of bits from a secret initial value called a seed"* — the recorded seed is the complete basis to reconstruct the draw (CWE-330: a draw with no recorded seed is not reproducible).

**2. A classification pins its version (PROC-CLASS-001).** The result records the SHA-256 input hash + the classifier version + the resolved class. The same input under the same version is byte-identical (a `uq(input_hash, classifier_version, kind)` backstop returns the existing row); a newer classifier version records a SEPARATE result and never mutates the older one — history is not silently re-labeled.

**3. A sensitive field is role-blinded (PROC-BLIND-001).** The raw value is `@JsonIgnore` and reaches a MEMBER only as a deterministic masked projection; the unmasked value is reachable only by an ADMIN — *"each entity is granted the minimum … it needs to perform its function."*

**Incorrect — a bare un-seeded draw, an un-versioned re-labeling classify, a fully-disclosed sensitive field:**

```java
public DrawResult draw(List<String> candidates, int k, long clientSeed) {
    Random r = new Random(clientSeed);                 // ❌ seed comes from the caller, not recorded server-side
    Collections.shuffle(candidates, r);                // ❌ candidate order not canonicalized — input hash unstable
    List<String> picked = candidates.subList(0, k);
    return new DrawResult(picked);                      // ❌ no recorded seed/algorithm/input-hash → not replayable (CWE-330)
}

public void classify(String input, String klass) {
    Procedure p = repo.findByInput(input).orElseGet(Procedure::new);
    p.setResolvedClass(klass);                          // ❌ overwrites — a new classifier silently re-labels history
    p.setRawSubject(input);                             // ❌ raw subject serializes to every caller — no blinding
    repo.save(p);
}
```

**Correct — a server-seeded replayable draw, a version-pinned idempotent classify, a blinded field:**

```java
@Transactional
public Procedure draw(String inputSetRef, List<String> candidates, int k, String actor) {
    long seed = secureRandom.nextLong();                            // ✅ seed generated SERVER-side
    List<String> sorted = candidates.stream().sorted().toList();    // ✅ canonical order → stable input hash
    String inputHash = Hashing.sha256Hex(String.join(",", sorted)); // ✅ recorded basis
    List<String> selected = SeededDraw.select(sorted, k, seed);     // ✅ deterministic from (sorted, k, seed)
    Procedure p = Procedure.draw(UUID.randomUUID(), inputSetRef, inputHash, SeededDraw.ALGORITHM,
        seed, k, String.join(",", selected), actor, Instant.now(clock));
    metrics.record("draw", "ok");
    return procedures.save(p);                                      // ✅ seed/algorithm/input_hash/selected all immutable
}

@Transactional(readOnly = true)
public List<String> replay(UUID id) {
    Procedure p = procedures.findById(id).orElseThrow(ReproducibilityException::notFound);
    List<String> replayed = SeededDraw.select(p.sortedCandidates(), p.getDrawK(), p.getSeed());
    if (!replayed.equals(p.selectedIdList())) {                     // ✅ pure verification; divergence fails closed
        metrics.record("replay", "diverged");
        throw ReproducibilityException.replayDiverged();            // 422 — never silently overwrite
    }
    metrics.record("replay", "ok");
    return replayed;                                                // ✅ byte-identical to the recorded selection
}
```

The seed is the complete, recorded basis for the draw; replay re-derives the byte-identical selection and mutates nothing. The classify path pins the classifier version (a `uq(input_hash, classifier_version, kind)` backstop makes a same-version recompute idempotent and a newer version a separate row). The blinded raw field is `@JsonIgnore`, exposed to a MEMBER only as a deterministic mask and unmasked only for an ADMIN. `Procedure` rows are append-only — no delete path exists.

Verification: review-tier — confirm the seed is server-generated and recorded immutably, replay reproduces the byte-identical selection without mutation, the classifier version is pinned (a newer version records a separate result), and the sensitive field is `@JsonIgnore` raw + masked-to-MEMBER + ADMIN-only unmask. The behavioural proof a fork-receiver keeps green: replay the recorded procedure N times → every replay reproduces the byte-identical selectedIds.

Reference: [NIST SP 800-90A Rev. 1 — DRBG (CSRC Glossary)](https://csrc.nist.gov/glossary/term/drbg)

Reference: [NIST SP 800-53 Rev. 5 — Least Privilege (CSRC Glossary)](https://csrc.nist.gov/glossary/term/least_privilege)

Reference: [CWE-330: Use of Insufficiently Random Values](https://cwe.mitre.org/data/definitions/330.html)
