package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@Tag("PRACTICES")
@Tag("PRACTICES-TX-004")
class TransactionPropagationRequiresNewTest {

    @Autowired
    private PropagationService svc;

    @Test
    @Transactional
    void practices_TX_004_requiresNewRunsInDifferentTransactionThanOuter() {
        // Inside the outer @Transactional set up by this test method.
        String outerName = TransactionSynchronizationManager.getCurrentTransactionName();
        assertThat(outerName).isNotNull();

        // REQUIRES_NEW suspends the outer transaction and creates a new one.
        String innerName = svc.requiresNewTxName();
        assertThat(innerName)
                .as("REQUIRES_NEW must run in a different transaction than the outer caller")
                .isNotEqualTo(outerName);

        // REQUIRED joins the existing transaction — same name as outer.
        String joinedName = svc.requiredTxName();
        assertThat(joinedName)
                .as("REQUIRED joins the caller's transaction — same name as outer")
                .isEqualTo(outerName);
    }
}
