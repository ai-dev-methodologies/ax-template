package com.ax.template.authblueprint.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link BreakGlass} — closes the zero-code gap of the audited
 * emergency-override contract IDW4 (EMR-lite dogfood, 2026-05-30) found all three
 * personas hand-rolled identically (and got the safety-critical pieces wrong:
 * a missing reason, a low-visibility log).
 *
 * <p>Framework-light: the {@link BreakGlass.AuditSink} is stubbed with a plain
 * in-test recorder (no Mockito, no Spring context), so it runs under the default
 * {@code test} task. The {@code @Tag("COMMON_BREAK_GLASS")} is UPPERCASE per the
 * test_tag_naming_convention_guard contract.
 */
@Tag("COMMON_BREAK_GLASS")
class BreakGlassTest {

    /** In-test audit sink recording every break-glass signal it receives. */
    private static final class RecordingSink implements BreakGlass.AuditSink {
        private final List<String> reasons = new ArrayList<>();
        private final List<String> callers = new ArrayList<>();

        @Override
        public void recordBreakGlass(String reason, String caller) {
            reasons.add(reason);
            callers.add(caller);
        }

        int count() {
            return reasons.size();
        }
    }

    // ─── happy path: audit recorded, action result returned ───────────────

    @Test
    void invoke_validReason_recordsAuditAndReturnsActionResult() {
        RecordingSink sink = new RecordingSink();

        String result = BreakGlass.invoke(
                "ER: unconscious patient, treating physician off shift",
                "dr-house",
                sink,
                () -> "phi-payload");

        assertThat(result).isEqualTo("phi-payload");
        // Audit was invoked exactly once with the reason + caller.
        assertThat(sink.count()).isEqualTo(1);
        assertThat(sink.reasons).containsExactly("ER: unconscious patient, treating physician off shift");
        assertThat(sink.callers).containsExactly("dr-house");
    }

    @Test
    void invoke_recordsAuditBeforeRunningAction() {
        // The audit signal must exist even if the privileged action later throws,
        // so the sink MUST be called before the action.
        RecordingSink sink = new RecordingSink();
        List<String> order = new ArrayList<>();

        BreakGlass.AuditSink orderingSink = (reason, caller) -> order.add("audit");

        assertThatThrownBy(() -> BreakGlass.invoke(
                "emergency",
                "dr-house",
                orderingSink,
                () -> {
                    order.add("action");
                    throw new IllegalStateException("read blew up");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(order).containsExactly("audit", "action");
        assertThat(sink.count()).isZero(); // unrelated sink untouched
    }

    @Test
    void canonicalAuditActionConstantIsStable() {
        // A domain keys its alert/review rule on this single stable value.
        assertThat(BreakGlass.AUDIT_ACTION).isEqualTo("BREAK_GLASS");
    }

    // ─── reason-required contract: action MUST NOT run without a reason ───

    @Test
    void invoke_nullReason_rejectedAndActionNotRun() {
        RecordingSink sink = new RecordingSink();
        AtomicBoolean ran = new AtomicBoolean(false);

        assertThatThrownBy(() -> BreakGlass.invoke(null, "dr-house", sink, () -> {
            ran.set(true);
            return "x";
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        assertThat(ran).isFalse();   // no unaudited bypass
        assertThat(sink.count()).isZero();
    }

    @Test
    void invoke_blankReason_rejectedAndActionNotRun() {
        RecordingSink sink = new RecordingSink();
        AtomicBoolean ran = new AtomicBoolean(false);

        assertThatThrownBy(() -> BreakGlass.invoke("   ", "dr-house", sink, () -> {
            ran.set(true);
            return "x";
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        assertThat(ran).isFalse();
        assertThat(sink.count()).isZero();
    }

    // ─── other guarded arguments ──────────────────────────────────────────

    @Test
    void invoke_blankCaller_rejectedAndActionNotRun() {
        RecordingSink sink = new RecordingSink();
        AtomicBoolean ran = new AtomicBoolean(false);

        assertThatThrownBy(() -> BreakGlass.invoke("emergency", " ", sink, () -> {
            ran.set(true);
            return "x";
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("caller");

        assertThat(ran).isFalse();
        assertThat(sink.count()).isZero();
    }

    @Test
    void invoke_nullSink_rejected() {
        assertThatThrownBy(() -> BreakGlass.invoke("emergency", "dr-house", null, () -> "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sink");
    }

    @Test
    void invoke_nullAction_rejected() {
        RecordingSink sink = new RecordingSink();
        assertThatThrownBy(() -> BreakGlass.invoke("emergency", "dr-house", sink, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");
        // Reason/caller validated first; sink/action checks happen before any audit emit.
        assertThat(sink.count()).isZero();
    }
}
