package com.ax.template.authblueprint.rangeownership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * RNG-NONOVERLAP-002 — a singleton lock row. This catalog's H2 test database cannot express a
 * true database range-exclusion constraint (unlike PostgreSQL's EXCLUDE USING gist +
 * btree_gist), so the authoritative backstop for the block-registration check-then-insert race
 * is a PESSIMISTIC_WRITE lock on this ONE known row, acquired BEFORE the overlap check and held
 * through the insert in the same transaction (mirrors PLAUSIBILITY-CONCURRENT-001 /
 * LINK-CONCURRENT-001's row-lock keystone pattern). The seed row is inserted by the migration
 * for a real deployment (Flyway-managed schema); {@code RangeOwnershipService} additionally
 * lazily find-or-creates it on first use, since integration tests run on
 * {@code ddl-auto=create-drop} (the entity-derived schema, NOT the Flyway migration — see
 * docs/NEW-DOMAIN-CHECKLIST.md item 7) where the migration's seed INSERT never executes. There
 * is exactly one row, with a fixed id, and it is never mutated after creation.
 */
@AggregateRoot
@Entity
@Table(name = "range_registry_lock")
public class RangeRegistryLock {

    public static final String GLOBAL_ID = "GLOBAL";

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 20)
    private String id;

    protected RangeRegistryLock() {}

    public RangeRegistryLock(String id) {
        this.id = id;
    }

    public String getId() { return id; }
}
