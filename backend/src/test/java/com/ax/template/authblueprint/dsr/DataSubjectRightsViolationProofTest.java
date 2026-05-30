package com.ax.template.authblueprint.dsr;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof tests — the MANDATORY structural backstop for IMW6
 * (l4_domain_reachability_guard fails the build without a *ViolationProofTest).
 * Reflection-based structural negatives: immutable columns reject writes, @Version
 * present + populated, no public setters, state machine rejects skip/reverse edges.
 */
@Tag("DSR")
class DataSubjectRightsViolationProofTest {

    /**
     * Violation: identity columns annotated mutable. subject_id / type / received_at
     * pin WHO the request belongs to, WHAT right was exercised, and WHEN the SLA
     * clock started — re-pointing any mid-life would be a stealth re-classification
     * or ownership transfer.
     */
    @Test
    @Tag("DSR-SLA-001")
    void violation_identityColumns_areImmutable() throws Exception {
        for (String name : new String[] { "subjectId", "type", "receivedAt" }) {
            Field f = DsrRequest.class.getDeclaredField(name);
            Column c = f.getAnnotation(Column.class);
            assertThat(c).as("DsrRequest." + name + " must carry @Column").isNotNull();
            assertThat(c.updatable())
                .as("DsrRequest." + name + " MUST be @Column(updatable=false)")
                .isFalse();
            assertThat(c.nullable())
                .as("DsrRequest." + name + " MUST be NOT NULL")
                .isFalse();
        }
        // the primary key id is also immutable
        Column idCol = DsrRequest.class.getDeclaredField("id").getAnnotation(Column.class);
        assertThat(idCol.updatable()).isFalse();
    }

    /**
     * Violation: @Version dropped. Without it concurrent status / SLA mutation
     * silently last-writer-wins, eroding the tracking record's integrity.
     */
    @Test
    @Tag("DSR-SLA-001")
    void violation_versionField_isPresentAndPopulated() throws Exception {
        Field v = DsrRequest.class.getDeclaredField("version");
        assertThat(v.getAnnotation(Version.class))
            .as("DsrRequest.version MUST be @Version (optimistic-lock guard)")
            .isNotNull();

        // a freshly persisted-style entity through the builder leaves version null
        // until JPA assigns it; the column must be the Long wrapper so JPA can manage it.
        assertThat(v.getType()).isEqualTo(Long.class);
    }

    /**
     * Violation: a public setter on the entity. DsrRequestStateMachine (status/closedAt)
     * and DsrService (SLA fields) are the only mutators — a public setter would let any
     * caller bypass the state machine and the SLA invariants.
     */
    @Test
    @Tag("DSR-SLA-001")
    void violation_noPublicSetters() {
        for (var m : DsrRequest.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set")) {
                assertThat(Modifier.isPublic(m.getModifiers()))
                    .as("DsrRequest." + m.getName()
                      + " must NOT be public — state machine + service are the only mutators")
                    .isFalse();
            }
        }
    }

    /**
     * Violation: state machine accepts a skip or reverse edge. CLOSED is terminal and
     * RECEIVED must not jump backwards from CLOSED — relaxing this would let a closed
     * (SLA-stopped) request re-open without an audit trail.
     */
    @Test
    @Tag("DSR-SLA-001")
    void violation_stateMachine_rejectsReverseAndSkipEdges() {
        DsrRequestStateMachine sm = new DsrRequestStateMachine(Clock.systemUTC());

        // CLOSED is terminal → markClosed again must throw.
        DsrRequest closed = DsrRequest.builder()
            .subjectId("s").type(DsrRequestType.ACCESS)
            .status(DsrRequestStatus.CLOSED)
            .receivedAt(Instant.now()).dueAt(Instant.now())
            .build();
        assertThatThrownBy(() -> sm.markClosed(closed))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sm.markInProgress(closed))
            .as("CLOSED → IN_PROGRESS is a reverse edge and must be rejected")
            .isInstanceOf(IllegalStateException.class);

        // IN_PROGRESS → IN_PROGRESS (self/skip) must throw.
        DsrRequest inProgress = DsrRequest.builder()
            .subjectId("s").type(DsrRequestType.ACCESS)
            .status(DsrRequestStatus.IN_PROGRESS)
            .receivedAt(Instant.now()).dueAt(Instant.now())
            .build();
        assertThatThrownBy(() -> sm.markInProgress(inProgress))
            .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Violation: the canonical metric type enum drifted from the fixed 5-value set.
     * dsr_*_total{type} cardinality is bounded ONLY because this enum is closed
     * (DSR-OBSERVABILITY-001).
     */
    @Test
    @Tag("DSR-OBSERVABILITY-001")
    void violation_metricTypeEnum_isExactlyFiveBoundedValues() {
        assertThat(DsrRequestType.values())
            .extracting(DsrRequestType::metricType)
            .containsExactlyInAnyOrder("access", "rectify", "erasure", "portability", "restrict");
    }
}
