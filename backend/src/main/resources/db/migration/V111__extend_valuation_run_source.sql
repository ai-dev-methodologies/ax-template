-- valuation-run-projection extension — VALRUN-FALLBACK-001 (backlog wave 2026-07-14, P3-36):
-- tag each run with the source that computed it, so an as-of read can fall back through a
-- CONFIGURED priority order of sources when the primary has no qualifying point, recording
-- provenance (which source served the read) rather than silently defaulting.

ALTER TABLE valuation_runs ADD COLUMN source_ref VARCHAR(200) NOT NULL DEFAULT 'PRIMARY';
