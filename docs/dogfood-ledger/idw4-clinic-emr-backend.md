# IDW4 — Hospital/EMR-lite (Spring Boot backend) 3-persona dogfood — NEW DOMAIN-CLASS RAMP

**Date:** 2026-05-29 · **Workflow:** wf_13e5b269-97d · 4 agents, 785K tokens, 1579s
**Method:** 3 personas built the SAME `clinic` API (Patient/Appointment+SM/EncounterRecord/Prescription/
Consent; PATIENT/PROVIDER/ADMIN RBAC; PHI access) in isolated worktrees on the fully-hardened catalog.
All 3 `complete`. First REGULATED / PHI domain class.

## Headline: structural axis CONVERGED, regulated axis is a FRESH ramp
- **Structural/CRUD axis ~90% + diminishing gaps:** all 3 reused 8 helpers/domains with near-zero friction
  (CallerScope, ResourceNotFoundException, GlobalProblemDetailAdvice, OptimisticLockingSupport, OffsetPageSupport,
  PageEnvelope, AuditPiiHelper, auditlog/AuditLogService) + the scaffold §0 business-role note. Every mechanical
  guard fired correctly. The CRUD skeleton is solved.
- **Regulated axis = NEW class of gap (not diminishing — a fresh convergence ramp):** the PHI domain forced
  regulatory invariants the 3 CRUD apps never did, and all 3 personas hand-rolled the SAME things (rule-of-three):
  consent subsystem, PROVIDER-relationship authz, audit-on-read wrapper, OptimisticLocking 428/412 mapping.
  This is exactly what a healthy self-reinforcing catalog looks like meeting a genuinely new domain class.

## 🔴 Most dangerous finding: the two safety-critical PHI invariants are enforced by NOTHING
Verified by 황태완's adversarial probes (both shipped a fully GREEN build — 54 guards + ArchUnit + testClinic):
- **audit-on-read NOT enforced:** deleting the `AuditLogService.record()` call on a PHI-read endpoint → green.
  An un-audited PHI disclosure passes every gate. No guard references audit-on-read.
- **no-raw-PHI-in-logs NOT enforced:** `log.info("name={} dob={} soap={} dx={}", ...)` → green. No guard scans
  log.*() statements; stored_error_column_sanitize_guard is anchored only to @Column stored-error columns.
"A catalog whose value prop is 'rules mechanically enforce AI output' has its single most dangerous unguarded
surface exactly where regulatory stakes are highest."

## IMW4 backlog (regulated-tier; the #1 is the highest-stakes guard work of the whole effort)
### IMW4 (highest value — ship now)
1. **audit_on_read_guard + phi_in_logs_guard** + a `common/@Phi` marker convention. Forward-enforcing
   (green-on-current — no PHI domain in main yet): a @Transactional read returning a @Phi-tagged type must
   reference AuditLogService.record; `log.*()` must not pass a @Phi/PHI-shaped getter. THE highest-value action.
2. **GlobalProblemDetailAdvice += OptimisticLockingSupport.PreconditionRequired→428 / PreconditionFailed→412 /
   ObjectOptimisticLockingFailureException→409** — verified absent; clinic was the first real consumer, so every
   adopter re-hand-rolls 3 ProblemDetail mappings. Easy, additive, probe-testable (mirror IMW3's ResourceNotFound→404).
### IMW5 (regulated completeness — larger lifts, deferred)
3. **common/ConsentSupport** (append-only ConsentRecord ledger + ConsentGate active-purpose check + 403-on-no-consent)
   — rule-of-three met (all 3 hand-rolled). + **reconcile consent-management-l0's CONTRADICTION**: CONSENT-RECORD-001
   mandates an append-only immutable ledger (GDPR Art 7 demonstrate-consent) but the natural shape is a mutable
   @Version'd row — all 3 chose mutable + flagged it. Spec needs a shippable shape.
4. **common/ participant-scope** authorization (PROVIDER-has-relationship-with-PATIENT; CallerScope is owner-only) —
   the dominant shape in any M:N-actor domain (clinic, marketplace, messaging); hand-rolled 3x.
5. **break-glass** primitive (audited emergency override) — core EMR/HIPAA pattern, zero catalog support.
6. **data-subject-rights-l0** lift (access/erase/rectify/portability against PHI — spec-only, uncomposed).
7. **NEW-DOMAIN-CHECKLIST regulated-domain section** (audit-on-read / consent-gate / break-glass / no-PHI-in-logs /
   no-store) — verified the checklist is CRUD-shaped (zero mentions of phi/consent/regulated); + sha sentinel anchor.
8. controller→repository SHELL guard (run-all-guards currently misses it; only ArchUnit catches it — coverage asymmetry).

## Verdict
4 industries (3 CRUD + 1 regulated): CRUD axis ~90% complete+enforced at diminishing gaps. Regulated axis at the
START of its ramp — neither complete (consent/DSR spec-only, no break-glass) nor enforced (audit-on-read +
no-PHI-in-logs pass every gate). Single highest-value next action: ship audit_on_read_guard + phi_in_logs_guard
(the most dangerous unguarded surface). Promote ConsentSupport second.
