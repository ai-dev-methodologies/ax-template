package com.ax.template.authblueprint.dispatch;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof tests for exclusive-assignment-l0 + timed-offer-l0. Structural + behavioral
 * invariants that would re-open a double-dispatch / lost-work hole if relaxed. Plain reflection /
 * pure logic / classpath read — no Spring context.
 */
@Tag("DISPATCH")
class DispatchViolationProofTest {

    // ── AVAIL-FSM-001 — sole-mutator status + FSM rejects illegal edges ──────────
    @Test @Tag("AVAIL-FSM-001")
    void violation_noPublicStatusSetters_andFsmRejectsIllegalEdges() {
        for (Class<?> entity : new Class<?>[]{Provider.class, ServiceRequest.class, Offer.class}) {
            boolean hasPublicSetStatus = false;
            for (Method m : entity.getMethods()) {   // public only
                if (m.getName().equals("setStatus")) hasPublicSetStatus = true;
            }
            assertThat(hasPublicSetStatus)
                .as(entity.getSimpleName() + ".status must have no PUBLIC setter (sole-mutator state machine)")
                .isFalse();
        }
        // the contended AVAILABLE->ASSIGNED claim is deliberately absent from the FSM; an illegal
        // provider edge throws (proves the FSM is a real guard, not a passthrough).
        Instant t = Instant.parse("2026-06-07T00:00:00Z");
        Provider offline = new Provider(UUID.randomUUID(), "h", ProviderStatus.OFFLINE, t, t);
        assertThatThrownBy(() -> new ProviderStateMachine().goOffline(offline))   // OFFLINE->OFFLINE illegal
            .isInstanceOf(DispatchException.class);
        Provider available = new Provider(UUID.randomUUID(), "h", ProviderStatus.AVAILABLE, t, t);
        assertThatThrownBy(() -> new ProviderStateMachine().release(available))   // AVAILABLE->AVAILABLE illegal
            .isInstanceOf(DispatchException.class);
    }

    // ── OFFER-FSM-001 — terminal offer states reject re-transition ───────────────
    @Test @Tag("OFFER-FSM-001")
    void violation_offerTerminalStatesAreFinal() {
        Instant t = Instant.parse("2026-06-07T00:00:00Z");
        Offer o = new Offer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), t.plusSeconds(60), 1, t);
        new OfferStateMachine().accept(o);                                        // PENDING -> ACCEPTED
        assertThatThrownBy(() -> new OfferStateMachine().expire(o))               // ACCEPTED -> EXPIRED illegal
            .isInstanceOf(DispatchException.class);
        assertThatThrownBy(() -> new OfferStateMachine().decline(o))              // ACCEPTED -> DECLINED illegal
            .isInstanceOf(DispatchException.class);
    }

    // ── EXCL-INDEX-003 — the migration declares the partial-unique-index backstops ──
    @Test @Tag("EXCL-INDEX-003")
    void violation_migrationDeclaresPartialUniqueIndexes() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V034__create_dispatch.sql")) {
            assertThat(in).as("V034 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // a provider holds at most one ACTIVE (ASSIGNED) request
        assertThat(sql).contains("uq_dispatch_request_active_provider");
        assertThat(sql).contains("WHERE status = 'ASSIGNED'");
        // a request has at most one PENDING offer
        assertThat(sql).contains("uq_dispatch_offer_pending_per_request");
        assertThat(sql).contains("WHERE status = 'PENDING'");
        // the backstop CHECK constraints
        assertThat(sql).contains("chk_dispatch_offer_ordinal");
    }

    // ── AVAIL-FRESH-002 — freshness predicate excludes a stale heartbeat ─────────
    @Test @Tag("AVAIL-FRESH-002")
    void violation_isFreshExcludesStaleHeartbeat() {
        Instant base = Instant.parse("2026-06-07T00:00:00Z");
        Provider p = new Provider(UUID.randomUUID(), "h", ProviderStatus.AVAILABLE, base, base);
        Duration window = Duration.ofSeconds(60);
        assertThat(p.isFresh(base.plusSeconds(30), window)).as("within window -> fresh").isTrue();
        assertThat(p.isFresh(base.plusSeconds(60), window)).as("exactly at window edge -> stale").isFalse();
        assertThat(p.isFresh(base.plusSeconds(90), window)).as("past window -> stale").isFalse();
    }

    // ── OFFER-TOCTOU-003 — acceptability is re-derived from the deadline ─────────
    @Test @Tag("OFFER-TOCTOU-003")
    void violation_offerAcceptableOnlyWhilePendingAndBeforeDeadline() {
        Instant t = Instant.parse("2026-06-07T00:00:00Z");
        Offer o = new Offer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), t.plusSeconds(60), 1, t);
        assertThat(o.isAcceptableAt(t.plusSeconds(30))).as("before deadline -> acceptable").isTrue();
        assertThat(o.isAcceptableAt(t.plusSeconds(60))).as("at deadline (exclusive) -> not acceptable").isFalse();
        assertThat(o.isAcceptableAt(t.plusSeconds(90))).as("after deadline -> not acceptable").isFalse();
    }

    // ── @Version + immutable columns across all three entities ───────────────────
    @Test @Tag("EXCL-CLAIM-001")
    void violation_versionAndImmutableColumns() throws Exception {
        assertVersion(Provider.class);
        assertVersion(ServiceRequest.class);
        assertVersion(Offer.class);
        assertImmutable(Provider.class, "id", "handle", "createdAt");
        assertImmutable(ServiceRequest.class, "id", "description", "createdBy", "createdAt");
        assertImmutable(Offer.class, "id", "requestId", "providerId", "expiresAt", "ordinal", "createdAt");
    }

    private static void assertVersion(Class<?> entity) throws Exception {
        Field v = entity.getDeclaredField("version");
        assertThat(v.isAnnotationPresent(Version.class))
            .as(entity.getSimpleName() + ".version must carry @Version (sweep-loses-race optimistic lock)")
            .isTrue();
    }

    private static void assertImmutable(Class<?> entity, String... fields) throws Exception {
        for (String name : fields) {
            Field f = entity.getDeclaredField(name);
            Column col = f.getAnnotation(Column.class);
            assertThat(col).as(entity.getSimpleName() + "." + name + " must carry @Column").isNotNull();
            assertThat(col.updatable())
                .as(entity.getSimpleName() + "." + name + " must be immutable (@Column updatable=false)")
                .isFalse();
        }
    }
}
