package com.ax.template.authblueprint.practices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Correct fixture for PRACTICES-CORE-002: non-final bean that Spring can CGLIB-proxy.
 * The class and its public methods are non-final so @Transactional advice is applied.
 * A `final class @Service` placed in the application context would silently fail to
 * proxy (Spring Boot 3.x logs a warning but the advice is dropped), so we demonstrate
 * the prohibition via reflection on the rule's test rather than instantiating a broken bean.
 */
@Service
public class ProxiedService {

    @Transactional
    public boolean isInsideTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }
}
