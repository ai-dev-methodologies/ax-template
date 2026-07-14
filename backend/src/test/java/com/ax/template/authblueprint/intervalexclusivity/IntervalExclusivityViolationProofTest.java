package com.ax.template.authblueprint.intervalexclusivity;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for interval-exclusivity-l0. Structural assertions that a deliberate break cannot
 * pass silently: no public setter, the interval @Check, the resource/booking identity columns
 * immutable, both write-paths (book/resize/cancel) go through the PESSIMISTIC_WRITE finder, the
 * state machine defines no edge out of CANCELLED, and the migration carries the same backstop — no
 * Spring context.
 */
@Tag("INTERVAL_EXCLUSIVITY")
class IntervalExclusivityViolationProofTest {

    // ── no public setter on either entity ──
    @Test @Tag("IVX-MUTATE-003")
    void violation_noPublicSetterOnEitherEntity() {
        for (Class<?> entity : new Class<?>[]{BookingResource.class, Booking.class}) {
            for (Method m : entity.getMethods()) {
                assertThat(m.getName()).as(entity.getSimpleName() + " must expose no public setter").doesNotStartWith("set");
            }
        }
        // resize/markCancelled must stay package-private — the sole-mutator seam
        for (var hook : new Object[][]{{"resize", new Class<?>[]{java.time.Instant.class, java.time.Instant.class}},
                {"markCancelled", new Class<?>[0]}}) {
            try {
                Method m = Booking.class.getDeclaredMethod((String) hook[0], (Class<?>[]) hook[1]);
                assertThat(Modifier.isPublic(m.getModifiers()))
                    .as("Booking." + hook[0] + " must be package-private").isFalse();
            } catch (NoSuchMethodException e) {
                throw new AssertionError("Booking must declare " + hook[0], e);
            }
        }
    }

    // ── IVX-OVERLAP-001 — the booking entity carries the start<end @Check ──
    @Test @Tag("IVX-OVERLAP-001")
    void violation_bookingCarriesIntervalCheck() {
        Check check = Booking.class.getAnnotation(Check.class);
        assertThat(check).as("Booking must carry @Check").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("start_at < end_at");
    }

    // ── identity columns immutable; @Version present on both entities ──
    @Test @Tag("IVX-MUTATE-003")
    void violation_identityColumnsImmutable_versioned() throws Exception {
        for (String f : new String[]{"id", "resourceKey", "createdAt"}) {
            Column col = BookingResource.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).isNotNull();
            assertThat(col.updatable()).as("BookingResource." + f + " must be immutable").isFalse();
        }
        for (String f : new String[]{"id", "resourceKey", "createdAt"}) {
            Column col = Booking.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).isNotNull();
            assertThat(col.updatable()).as("Booking." + f + " must be immutable").isFalse();
        }
        assertThat(BookingResource.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        assertThat(Booking.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── IVX-CONCURRENT-002 — every write-path locks the resource row via the PESSIMISTIC_WRITE finder ──
    @Test @Tag("IVX-CONCURRENT-002")
    void violation_everyWritePathLocksTheResourceRow() throws Exception {
        Method locked = BookingResourceRepository.class.getMethod("findByResourceKeyForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).as("the serialization point must be PESSIMISTIC_WRITE").isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String src = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("user.dir"),
            "src", "main", "java", "com", "ax", "template", "authblueprint", "intervalexclusivity", "BookingService.java"));
        for (String method : new String[]{"public Booking book(", "public Booking resize(", "public Booking cancel("}) {
            int start = src.indexOf(method);
            assertThat(start).as(method + " must exist in BookingService").isPositive();
            String body = src.substring(start, src.indexOf("\n    }", start));
            assertThat(body)
                .as(method + " must lock the resource row before writing (IVX-CONCURRENT-002)")
                .contains("findByResourceKeyForUpdate");
        }
    }

    // ── the state machine defines no edge OUT of CANCELLED (zero outgoing edges) ──
    @Test @Tag("IVX-MUTATE-003")
    void violation_stateMachineHasNoEdgeOutOfCancelled() {
        for (Method m : BookingStateMachine.class.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }
            assertThat(m.getName()).as("the ONLY transition is the one-way cancel edge").isEqualTo("cancel");
        }
    }

    // ── the migration carries the same interval @Check ──
    @Test @Tag("IVX-OVERLAP-001")
    void violation_migrationCarriesTheSameCheck() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V108__create_interval_exclusivity.sql")) {
            assertThat(in).as("V108__create_interval_exclusivity.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("start_at < end_at");
            assertThat(sql).contains("UNIQUE INDEX uq_ivx_resource_key");
        }
    }
}
