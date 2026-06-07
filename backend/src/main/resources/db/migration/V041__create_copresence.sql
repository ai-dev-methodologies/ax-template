-- negative-copresence-gate reference workload — realizes specs/negative-copresence-gate-l0.yaml
-- (IDW16 EMR dogfood flagship: a candidate write is evaluated against the SET of the subject's other
-- active members via set-intersection on a normalized concept; graded ABSOLUTE/RELATIVE; fail-closed
-- on an unassessable concept; in-transaction set re-read under the subject lock).

CREATE TABLE copresence_subjects (
    id          UUID         NOT NULL PRIMARY KEY,
    subject_key VARCHAR(200) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_copresence_subject_key ON copresence_subjects (subject_key);

-- knowledge-base vocabulary: a candidate concept absent here is UNASSESSABLE → gate fails closed.
CREATE TABLE copresence_known_concepts (
    id      UUID         NOT NULL PRIMARY KEY,
    concept VARCHAR(200) NOT NULL
);
CREATE UNIQUE INDEX uq_copresence_known_concept ON copresence_known_concepts (concept);

-- knowledge-base: an UNORDERED conflicting concept pair with graded severity.
CREATE TABLE copresence_conflict_rules (
    id        UUID         NOT NULL PRIMARY KEY,
    concept_a VARCHAR(200) NOT NULL,
    concept_b VARCHAR(200) NOT NULL,
    severity  VARCHAR(20)  NOT NULL,
    reason    VARCHAR(500) NOT NULL,
    CONSTRAINT chk_copresence_conflict_distinct CHECK (concept_a <> concept_b)
);
CREATE UNIQUE INDEX uq_copresence_conflict_pair ON copresence_conflict_rules (concept_a, concept_b);

-- the subject's members; ACTIVE rows form the set the gate intersects the candidate against.
CREATE TABLE copresence_members (
    id                  UUID          NOT NULL PRIMARY KEY,
    subject_id          UUID          NOT NULL,
    concept             VARCHAR(200)  NOT NULL,
    label               VARCHAR(400)  NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    override_reason     VARCHAR(1000),
    overridden_findings VARCHAR(2000),
    created_at          TIMESTAMP     NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX ix_copresence_member_subject ON copresence_members (subject_id, status);
