package com.ax.template.authblueprint.rangeownership;

import jakarta.persistence.Column;
import jakarta.persistence.LockModeType;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for range-ownership-l0. Structural assertions a deliberate break cannot pass
 * silently: every identity/config column is immutable, NO currentOwner field exists anywhere
 * (current owner is always derived-on-read), the registry lock uses PESSIMISTIC_WRITE, NO delete
 * path exists anywhere in the domain, and the half-open overlap/containment predicates are
 * mathematically correct at the boundary (adjacency is not overlap).
 */
@Tag("RANGEOWNERSHIP")
class RangeOwnershipViolationProofTest {

    // ── RNG-NONOVERLAP-002 — half-open overlap predicate: adjacency is NOT overlap ──
    @Test @Tag("RNG-NONOVERLAP-002")
    void violation_halfOpenOverlapPredicate_adjacencyIsNotOverlap() {
        RangeBlock block = new RangeBlock(UUID.randomUUID(), "owner", 1000L, 2000L, Instant.now());
        assertThat(block.overlaps(1500L, 2500L)).as("a genuinely overlapping range must be detected").isTrue();
        assertThat(block.overlaps(500L, 1000L)).as("touching at the START boundary is NOT overlap (half-open)").isFalse();
        assertThat(block.overlaps(2000L, 3000L)).as("touching at the END boundary is NOT overlap (half-open)").isFalse();
        assertThat(block.overlaps(0L, 500L)).as("a disjoint range must not be flagged").isFalse();
    }

    // ── RNG-CONTAINMENT-001 — half-open containment predicate: inclusive start, exclusive end ──
    @Test @Tag("RNG-CONTAINMENT-001")
    void violation_halfOpenContainmentPredicate() {
        RangeBlock block = new RangeBlock(UUID.randomUUID(), "owner", 1000L, 2000L, Instant.now());
        assertThat(block.contains(1000L)).as("the inclusive lower bound must be contained").isTrue();
        assertThat(block.contains(1999L)).isTrue();
        assertThat(block.contains(2000L)).as("the exclusive upper bound must NOT be contained").isFalse();
        assertThat(block.contains(999L)).isFalse();
    }

    // ── RNG-CONTAINMENT/NONOVERLAP-001/002 — RangeBlock config is fully immutable; @Check backstops ──
    @Test @Tag("RNG-NONOVERLAP-002")
    void violation_rangeBlockImmutable_checked() throws Exception {
        for (String f : new String[]{"id", "ownerRef", "rangeStart", "rangeEnd", "createdAt"}) {
            Column col = RangeBlock.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("RangeBlock." + f + " must be immutable").isFalse();
        }
        Check check = RangeBlock.class.getAnnotation(Check.class);
        assertThat(check.constraints()).contains("range_start < range_end");
    }

    // ── RNG-PORT-003 — NO currentOwner field exists anywhere; assignment identity is immutable ──
    @Test @Tag("RNG-PORT-003")
    void violation_noCurrentOwnerFieldAnywhere_assignmentImmutable() throws Exception {
        for (Class<?> type : new Class<?>[]{IdentifierAssignment.class, RangeBlock.class, OwnershipEvent.class}) {
            for (Field f : type.getDeclaredFields()) {
                assertThat(f.getName().toLowerCase())
                    .as(type.getSimpleName() + " must have no currentOwner field — derive-on-read only")
                    .doesNotContain("currentowner");
            }
        }
        for (String f : new String[]{"id", "identifierValue", "createdAt"}) {
            Column col = IdentifierAssignment.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("IdentifierAssignment." + f + " must be immutable").isFalse();
        }
        assertThat(IdentifierAssignment.class.getDeclaredField("identifierValue").getAnnotation(Column.class).unique())
            .as("one assignment root per identifier").isTrue();
    }

    // ── RNG-PORT-003 — OwnershipEvent is fully immutable and append-only ──
    @Test @Tag("RNG-PORT-003")
    void violation_ownershipEventImmutable() throws Exception {
        for (Method m : OwnershipEvent.class.getMethods()) {
            assertThat(m.getName()).as("OwnershipEvent must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "assignmentId", "fromOwner", "toOwner", "reason", "occurredAt"}) {
            Column col = OwnershipEvent.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("OwnershipEvent." + f + " must be immutable").isFalse();
        }
    }

    // ── RNG-NONOVERLAP-002 — the registry lock uses PESSIMISTIC_WRITE; NO delete path anywhere ──
    @Test @Tag("RNG-NONOVERLAP-002")
    void violation_registryLockPessimisticWrite_noDeletePath() throws Exception {
        Method locked = RangeRegistryLockRepository.class.getMethod("lockForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);

        for (Class<?> repo : new Class<?>[]{RangeBlockRepository.class, IdentifierAssignmentRepository.class, RangeRegistryLockRepository.class}) {
            for (Method m : repo.getDeclaredMethods()) {
                assertThat(m.getName()).doesNotContain("delete");
            }
        }
        for (String src : new String[]{"RangeOwnershipService", "RangeOwnershipController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "rangeownership", src + ".java"));
            assertThat(text).as(src + " must contain no delete call").doesNotContain(".delete(").doesNotContain("deleteBy");
        }

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "rangeownership", "RangeOwnershipService.java"));
        int idx = svc.indexOf("public RangeBlock registerBlock(");
        assertThat(idx).as("registerBlock must exist").isPositive();
        String body = svc.substring(idx, svc.indexOf("\n    }", idx));
        assertThat(body).as("registerBlock must acquire the registry lock BEFORE the overlap check")
            .contains("lock.lockForUpdate");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("RNG-NONOVERLAP-002")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V115__create_range_ownership.sql")) {
            assertThat(in).as("V115__create_range_ownership.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("range_start < range_end");
            assertThat(sql).contains("INSERT INTO range_registry_lock");
            assertThat(sql).contains("identifier_value BIGINT NOT NULL UNIQUE");
            assertThat(sql).contains("CREATE TABLE ownership_events");
        }
    }
}
