package com.ax.template.authblueprint.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for reserve-settle-balance-l0. Structural immutability + one-terminal-transition +
 * exact-decimal conservation + migration solvency/overspend backstops — no Spring context.
 */
@Tag("RESERVATION")
class ReservationViolationProofTest {

    // ── RSV-RESERVE-001 / RSV-CONSERVE-001 — balance has no public setter; immutable funding columns ──
    @Test @Tag("RSV-CONSERVE-001")
    void violation_balanceNoPublicSetter_immutableFunding_versioned() throws Exception {
        for (Method m : ReservableBalance.class.getMethods()) {
            assertThat(m.getName())
                .as("ReservableBalance must expose no public setter (committed/reserved move only via the service)")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "scopeKey", "funded", "createdAt"}) {
            Column col = ReservableBalance.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ReservableBalance." + f + " must be immutable").isFalse();
        }
        assertThat(ReservableBalance.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("ReservableBalance.version must carry @Version").isTrue();

        // exact-decimal conservation: available == funded − committed − reserved
        ReservableBalance b = new ReservableBalance(UUID.randomUUID(), "s",
            new BigDecimal("100.0000"), new BigDecimal("30.0000"), new BigDecimal("20.0000"), Instant.EPOCH);
        assertThat(b.available()).isEqualByComparingTo("50.0000");
    }

    // ── RSV-RELEASE-001 — hold is append-immutable except its single terminal transition; no public setter ──
    @Test @Tag("RSV-RELEASE-001")
    void violation_holdImmutableExceptTerminalTransition() throws Exception {
        for (Method m : Reservation.class.getMethods()) {
            assertThat(m.getName())
                .as("Reservation must expose no public setter (status moves only via the service)")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "balanceId", "amount", "expiresAt", "createdAt"}) {
            Column col = Reservation.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Reservation." + f + " must be immutable").isFalse();
        }
        assertThat(Reservation.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("Reservation.version must carry @Version").isTrue();

        // exactly one terminal state class: OUTSTANDING is the only non-terminal status
        assertThat(ReservationStatus.OUTSTANDING.isTerminal()).isFalse();
        assertThat(ReservationStatus.SETTLED.isTerminal()).isTrue();
        assertThat(ReservationStatus.RELEASED.isTerminal()).isTrue();
        assertThat(ReservationStatus.EXPIRED.isTerminal()).isTrue();
    }

    // ── RSV-SETTLE-001 — migration declares the solvency + overspend CHECK backstops ──
    @Test @Tag("RSV-SETTLE-001")
    void violation_migrationDeclaresSolvencyAndOverSettleChecks() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V038__create_reservations.sql")) {
            assertThat(in).as("V038 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("chk_reservable_solvency");
        assertThat(sql).contains("committed_amount + reserved_amount <= funded_amount");
        assertThat(sql).contains("chk_reservation_settled");
        assertThat(sql).contains("settled_amount <= amount");
    }
}
