/**
 * FIXTURE: no-rrn-logging/pass
 * Demonstrates CORRECT pattern: RRN never appears in any log statement.
 * Identity is logged by an opaque masked token only.
 */
package com.example.fixture.no_rrn_logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void registerUser(String name, String rrn) {
        // CORRECT: log a non-sensitive identifier only
        log.info("registering user name={}", name);

        // RRN is processed in memory, never emitted to any log sink
        String rrnHash = hashForAudit(rrn);   // one-way hash — not reversible
        log.debug("identity check hash={}", rrnHash);
    }

    public void verifyIdentity(String rrn) {
        // CORRECT: side-effect of verification logged without exposing RRN
        log.debug("identity verification attempted");
        boolean result = doVerify(rrn);
        log.info("identity verification result={}", result);
    }

    private String hashForAudit(String rrn) {
        // one-way SHA-256 of rrn — safe to log as a correlation handle
        return "[RRN_HASH:" + Integer.toHexString(rrn.hashCode()) + "]";
    }

    private boolean doVerify(String rrn) {
        return rrn != null && !rrn.isBlank();
    }
}
