-- signed-artifact reference workload — realizes specs/signed-artifact-l0.yaml (backlog wave
-- 2026-07-14, P3-39 — spec-only latent gap closure; the spec predates this table by an earlier
-- wave and had zero backend realization until now). One append-only issuance row per signed
-- artifact: the detached asymmetric (ES256) JWS, the kid pinning the published verifying key,
-- and the SHA-256 content-hash the signature actually covers (SIGNED-ASYM-001).

CREATE TABLE signed_artifacts (
    id           UUID          NOT NULL PRIMARY KEY,
    subject_ref  VARCHAR(200)  NOT NULL,
    content_hash VARCHAR(64)   NOT NULL,
    kid          VARCHAR(100)  NOT NULL,
    alg          VARCHAR(20)   NOT NULL,
    jws          VARCHAR(4000) NOT NULL,
    issued_at    TIMESTAMP     NOT NULL
);
