-- FAIL fixture: a migration EXISTS (so the guard does not exit 2 for a missing
-- migration dir) but it creates an UNRELATED table. The inline @Entity's table
-- "widget" is deliberately NOT backed, so the widened anchor reports the
-- entity↔migration drift and the guard exits 1.
CREATE TABLE other_table (
    id UUID PRIMARY KEY
);
