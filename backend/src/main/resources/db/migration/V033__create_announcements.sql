-- announcement-l0 reference workload (specs/announcement-l0.yaml). Time-boxed admin
-- notices: DRAFT -> PUBLISHED -> ARCHIVED lifecycle, half-open [starts_at, ends_at)
-- active window (visibility DERIVED at read time, never stored). version = JPA @Version.
CREATE TABLE announcements (
    id          UUID         NOT NULL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    body        VARCHAR(5000) NOT NULL,
    state       VARCHAR(16)  NOT NULL,
    starts_at   TIMESTAMP    NOT NULL,
    ends_at     TIMESTAMP    NOT NULL,
    created_by  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_announcement_state_starts ON announcements (state, starts_at);
