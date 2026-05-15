package com.ax.template.authblueprint.practices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransactionalService {

    /**
     * Self-invocation path. Calls the @Transactional method via this.method(), which bypasses
     * Spring's AOP proxy. Result: the inner @Transactional has NO effect — no transaction starts.
     */
    public boolean callViaSelfInvocation() {
        return this.transactionalMethod();
    }

    /**
     * Same logical method, intended to be called via the proxy (from another bean or via an
     * @Autowired reference to TransactionalService). When called through the proxy, the
     * @Transactional advice runs and a real transaction is opened.
     */
    @Transactional
    public boolean transactionalMethod() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }
}
