-- Migration dir is populated but deliberately does NOT create `widget_ghost`,
-- so entity_migration_guard detects the drift (dir-missing would be exit 2, not
-- the exit 1 drift we want to prove).
CREATE TABLE unrelated (
    id BIGINT PRIMARY KEY
);
