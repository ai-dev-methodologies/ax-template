package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("PRACTICES")
@Tag("PRACTICES-TX-002")
class TransactionReadOnlyTest {

    @Autowired
    private ReadOnlyTransactionService svc;

    @Test
    void practices_TX_002_readOnlyAnnotationPropagatesToTransactionManager() {
        assertThat(svc.inReadOnlyTx())
                .as("@Transactional(readOnly = true) must surface in TransactionSynchronizationManager")
                .isTrue();
        assertThat(svc.inReadWriteTx())
                .as("default @Transactional must NOT be marked read-only")
                .isFalse();
    }
}
