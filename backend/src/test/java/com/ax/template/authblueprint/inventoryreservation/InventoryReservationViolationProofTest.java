package com.ax.template.authblueprint.inventoryreservation;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for two-axis-inventory-reservation-l0. Structural assertions a deliberate break
 * cannot pass silently: AVAILABLE is DERIVED (no stored 'available' column — it cannot drift), the
 * reservation quantity + item are immutable, the item carries @Version + the @Check reserved >= 0
 * AND reserved <= on_hand backstop, the status moves HELD → (COMMITTED|RELEASED) exactly once via
 * the sole-mutator state machine, NO delete path exists anywhere in the domain, axis mutators are
 * package-sealed, the write path uses the PESSIMISTIC_WRITE finder, and the migration carries the
 * same backstops.
 */
@Tag("INVENTORYRESERVATION")
class InventoryReservationViolationProofTest {

    // ── INVRES-RESERVE/CONSERVE-001 — AVAILABLE is DERIVED, never a stored column ──
    @Test @Tag("INVRES-RESERVE-001")
    void violation_availableIsDerived_neverStored() throws Exception {
        // the entity persists exactly two quantity axes; there is NO 'available' field/column
        for (Field f : InventoryItem.class.getDeclaredFields()) {
            assertThat(f.getName()).as("no stored 'available' field — it must be derived").isNotEqualTo("available");
        }
        // on_hand and reserved are the two backing axes
        assertThat(InventoryItem.class.getDeclaredField("onHand").getAnnotation(Column.class).name()).isEqualTo("on_hand");
        assertThat(InventoryItem.class.getDeclaredField("reserved").getAnnotation(Column.class).name()).isEqualTo("reserved");
        // available() is a derived METHOD (onHand − reserved), present and public
        Method available = InventoryItem.class.getMethod("available");
        assertThat(available.getReturnType()).isEqualTo(long.class);
        // and never name a column 'value'/'order'/'limit' (reserved words) — on_hand is the chosen name
        String entity = Files.readString(srcPath("InventoryItem.java"));
        assertThat(entity).doesNotContain("name = \"value\"").doesNotContain("name = \"order\"").doesNotContain("name = \"limit\"");
    }

    // ── INVRES-COMMIT/RELEASE-001 — HELD → (COMMITTED|RELEASED) exactly once; terminals are sinks ──
    @Test @Tag("INVRES-COMMIT-001") @Tag("INVRES-RELEASE-001")
    void violation_lifecycleExactlyOnce_terminalsAreSinks() throws Exception {
        // the status enum is exactly the three lifecycle states
        assertThat(ReservationStatus.values())
            .containsExactly(ReservationStatus.HELD, ReservationStatus.COMMITTED, ReservationStatus.RELEASED);

        // the state machine is the sole status mutator: InventoryReservation.setStatus is package-private
        Method setStatus = InventoryReservation.class.getDeclaredMethod("setStatus", ReservationStatus.class);
        assertThat(Modifier.isPublic(setStatus.getModifiers())).as("InventoryReservation.setStatus must be package-private").isFalse();

        // the state machine declares commit + release and gates on HELD (the exactly-once edge)
        String sm = Files.readString(srcPath("ReservationStateMachine.java"));
        assertThat(sm).contains("ReservationStatus.HELD,").contains("EnumSet.of(ReservationStatus.COMMITTED, ReservationStatus.RELEASED)");
        assertThat(sm).contains("ReservationStatus.COMMITTED, EnumSet.noneOf(ReservationStatus.class)");
        assertThat(sm).contains("ReservationStatus.RELEASED, EnumSet.noneOf(ReservationStatus.class)");
        assertThat(sm).as("an illegal edge is the exactly-once 409").contains("reservationNotHeld()");

        // the reservation's quantity + item are immutable
        for (String f : new String[]{"id", "itemId", "quantity", "actor", "createdAt"}) {
            Column col = InventoryReservation.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("InventoryReservation." + f + " must be immutable").isFalse();
        }
        // no public setter on the reservation
        for (Method m : InventoryReservation.class.getMethods()) {
            assertThat(m.getName()).as("InventoryReservation must have no public setter").doesNotStartWith("set");
        }
    }

    // ── INVRES-CONSERVE-001 — @Check backstop; @Version; axis mutators sealed; NO delete path ──
    @Test @Tag("INVRES-CONSERVE-001")
    void violation_checkBackstop_version_mutatorsSealed_noDelete() throws Exception {
        Check check = InventoryItem.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand");

        // the axis mutators are package-private (the entity is its own sole mutator)
        for (String hook : new String[]{"reserve", "commitReservation", "releaseReservation"}) {
            Method m = java.util.Arrays.stream(InventoryItem.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("InventoryItem." + hook + " must be package-private").isFalse();
        }
        // immutable identity columns + @Version on the item
        for (String f : new String[]{"id", "sku", "createdAt"}) {
            Column col = InventoryItem.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("InventoryItem." + f + " must be immutable").isFalse();
        }
        assertThat(InventoryItem.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        // NO delete path anywhere in the domain
        for (Method m : InventoryItemRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"InventoryReservationService", "InventoryReservationController"}) {
            String text = Files.readString(srcPath(src + ".java"));
            assertThat(text).as(src + " must contain no delete call — items/reservations are never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── INVRES-CONCURRENT-001 — the write path uses the PESSIMISTIC_WRITE finder ──
    @Test @Tag("INVRES-CONCURRENT-001")
    void violation_lockedFinder_andGatedReserve() throws Exception {
        Method locked = InventoryItemRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(srcPath("InventoryReservationService.java"));
        for (String method : new String[]{"public InventoryReservation reserve(", "public InventoryReservation commit(", "public InventoryReservation release("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).as(method + " must take the item row lock").contains("findByIdForUpdate");
        }
        assertThat(svc).as("the reserve precondition gates on DERIVED available")
            .contains("item.available() < quantity");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("INVRES-CONSERVE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V059__create_inventory_reservation.sql")) {
            assertThat(in).as("V059__create_inventory_reservation.sql must exist").isNotNull();
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // strip SQL line comments so prose mentioning 'available' is not mistaken for a column
            String ddl = java.util.Arrays.stream(raw.split("\n"))
                .map(line -> { int c = line.indexOf("--"); return c >= 0 ? line.substring(0, c) : line; })
                .reduce("", (a, b) -> a + " " + b).replaceAll("\\s+", " ");
            assertThat(ddl).contains("on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand");
            // no stored 'available' column in the table DDL (only the two backing axes)
            assertThat(ddl.toLowerCase()).as("available must be DERIVED, never a stored column").doesNotContain("available");
            assertThat(ddl).contains("status VARCHAR(20) NOT NULL");
            assertThat(ddl).contains("on_hand BIGINT NOT NULL");
            assertThat(ddl).contains("reserved BIGINT NOT NULL");
        }
    }

    private static Path srcPath(String fileName) {
        return Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "inventoryreservation", fileName);
    }
}
