package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Tag("PRACTICES")
@Tag("PRACTICES-TX-003")
class TransactionRollbackForCheckedTest {

    @Test
    void practices_TX_003_correctMethodDeclaresRollbackForCheckedExceptions() throws Exception {
        Method correct = CheckedRollbackService.class.getDeclaredMethod("rollbackForChecked");
        Transactional ann = correct.getAnnotation(Transactional.class);
        assertThat(ann).isNotNull();
        assertThat(ann.rollbackFor())
                .as("rollbackFor must include Exception.class so checked failures roll back")
                .contains(Exception.class);
    }

    @Test
    void practices_TX_003_naiveMethodOmitsRollbackFor_documentingTheAntiPattern() throws Exception {
        Method naive = CheckedRollbackService.class.getDeclaredMethod("naiveCheckedThrow");
        Transactional ann = naive.getAnnotation(Transactional.class);
        assertThat(ann).isNotNull();
        // The default rollbackFor is empty — checked exceptions slip through and the
        // transaction commits despite the apparent failure.
        assertThat(ann.rollbackFor())
                .as("default @Transactional has no rollbackFor — anti-pattern signal")
                .isEmpty();
    }
}
