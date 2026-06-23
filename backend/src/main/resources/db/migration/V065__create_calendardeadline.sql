-- business-day-deadline-arithmetic reference workload — realizes specs/business-day-deadline-arithmetic-l0.yaml
-- (P1-51: a statutory deadline computed by CALENDAR-vs-BUSINESS-day arithmetic with a RECORDED
-- reconstructible basis, an applied/recorded roll convention, a RECOMPUTED overdue predicate, and
-- a VERSIONED holiday calendar so a later edit does not silently move already-computed deadlines).

-- CALDLINE-CALVER-001 — a versioned holiday calendar; the @Version column is the recorded input a
-- computed deadline pins, monotonically incremented on every edit (never mutated in place).
CREATE TABLE holiday_calendars (
    id                UUID         NOT NULL PRIMARY KEY,
    calendar_name     VARCHAR(100) NOT NULL,
    published_version BIGINT       NOT NULL DEFAULT 0,   -- the domain version a deadline pins
    version           BIGINT       NOT NULL DEFAULT 0,   -- JPA optimistic-lock counter
    created_at        TIMESTAMP    NOT NULL,
    CONSTRAINT chk_holiday_calendar CHECK (published_version >= 0)
);

-- The holiday set (an @ElementCollection of LocalDate owned by holiday_calendars — no member repo).
CREATE TABLE holiday_calendar_dates (
    calendar_id  UUID NOT NULL REFERENCES holiday_calendars(id),
    holiday_date DATE NOT NULL,
    PRIMARY KEY (calendar_id, holiday_date)
);

-- CALDLINE-BASIS-001 — the computed deadline records its FULL reconstructible basis; all basis
-- columns are immutable. There is deliberately NO stored late/overdue boolean column — overdue is
-- recomputed on read (CALDLINE-OVERDUE-001). @Check backstops: non-negative N/version; NONE roll => adjusted==raw.
CREATE TABLE calendar_deadlines (
    id                       UUID         NOT NULL PRIMARY KEY,
    obligation_ref           VARCHAR(200) NOT NULL,
    start_date               DATE         NOT NULL,
    period_count             INTEGER      NOT NULL,
    mode                     VARCHAR(20)  NOT NULL,    -- CALENDAR | BUSINESS
    holiday_calendar_id      UUID         NOT NULL REFERENCES holiday_calendars(id),
    holiday_calendar_version BIGINT       NOT NULL,    -- the version pinned at compute time
    raw_deadline             DATE         NOT NULL,    -- before any roll
    roll_convention          VARCHAR(20)  NOT NULL,    -- FOLLOWING | NONE
    adjusted_deadline        DATE         NOT NULL,    -- after the recorded roll
    version                  BIGINT       NOT NULL DEFAULT 0,
    created_at               TIMESTAMP    NOT NULL,
    CONSTRAINT chk_calendar_deadline CHECK (
        period_count >= 0 AND holiday_calendar_version >= 0
        AND (roll_convention <> 'NONE' OR adjusted_deadline = raw_deadline)
    )
);
