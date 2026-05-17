package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("PRACTICES")
@Tag("PRACTICES-TX-001")
class TransactionSelfInvocationTest {

    @Autowired
    private TransactionalService service;

    @Test
    void practices_TX_001_selfInvocationBypassesTransactional() {
        // Self-invocation (this.transactionalMethod()) bypasses the proxy.
        // No advice runs → no real transaction is opened.
        boolean activeViaSelf = service.callViaSelfInvocation();
        assertThat(activeViaSelf)
                .as("self-invoked @Transactional must NOT have an active transaction")
                .isFalse();

        // External invocation (through the injected proxy bean) goes through the AOP advice.
        // A real transaction is opened.
        boolean activeViaProxy = service.transactionalMethod();
        assertThat(activeViaProxy)
                .as("proxy-invoked @Transactional must have an active transaction")
                .isTrue();
    }
}
