package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-LANG-002")
class LangSealedResultHierarchyTest {

    @Test
    void practices_LANG_002_paymentResultIsSealedAndPermitsAreRecords() {
        Class<?> root = PaymentResult.class;
        assertThat(root.isSealed())
                .as("PaymentResult must be sealed so the compiler enforces exhaustive switch")
                .isTrue();
        Class<?>[] permitted = root.getPermittedSubclasses();
        assertThat(permitted)
                .as("PaymentResult must permit at least two terminal outcomes")
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(Arrays.stream(permitted).allMatch(Class::isRecord))
                .as("every permitted subclass of a sealed result hierarchy must be a record")
                .isTrue();
    }

    @Test
    void practices_LANG_002_exhaustiveSwitchCompiles() {
        // The act of this switch compiling is the proof — drop a branch and the compiler
        // refuses. The actual call exercises the runtime side of the contract.
        PaymentResult r = new PaymentResult.PaymentSuccess("tx-1", 100L);
        String label = switch (r) {
            case PaymentResult.PaymentSuccess s -> "ok:" + s.transactionId();
            case PaymentResult.PaymentFailure f -> "fail:" + f.errorCode();
        };
        assertThat(label).isEqualTo("ok:tx-1");
    }
}
