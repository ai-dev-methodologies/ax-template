-- NETM-RATE-001 extension (P3-24) — realizes specs/signed-dual-register-l0.yaml's rate-asymmetric
-- billing addendum: a net meter's IMPORT and EXPORT registers may carry DIFFERENT per-unit rates, and a
-- billing-period close derives the billed monetary amount from the SAME per-direction cumulatives that
-- already satisfy quantity conservation (never an independently-drifting monetary total).

ALTER TABLE net_meters
    ADD COLUMN rate_import NUMERIC(19,4) NOT NULL DEFAULT 1,
    ADD COLUMN rate_export NUMERIC(19,4) NOT NULL DEFAULT 1,
    ADD COLUMN import_cumulative_at_period_start NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN export_cumulative_at_period_start NUMERIC(19,4) NOT NULL DEFAULT 0;

-- NETM-RATE-001 — rates must be strictly positive (drop + recreate the cumulatives check to fold the
-- rate constraint into the SAME @Check the entity declares — Hibernate maps one @Check per entity).
ALTER TABLE net_meters DROP CONSTRAINT chk_net_meter_cumulatives;
ALTER TABLE net_meters ADD CONSTRAINT chk_net_meter_cumulatives
    CHECK (cumulative_import >= 0 AND cumulative_export >= 0 AND rate_import > 0 AND rate_export > 0);

ALTER TABLE net_meter_periods
    ADD COLUMN import_delta   NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN export_delta   NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN rate_import    NUMERIC(19,4) NOT NULL DEFAULT 1,
    ADD COLUMN rate_export    NUMERIC(19,4) NOT NULL DEFAULT 1,
    ADD COLUMN billed_amount  NUMERIC(19,4) NOT NULL DEFAULT 0;
