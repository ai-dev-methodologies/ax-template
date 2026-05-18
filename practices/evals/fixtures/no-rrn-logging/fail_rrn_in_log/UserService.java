/**
 * FIXTURE: no-rrn-logging/fail_rrn_in_log
 * Demonstrates WRONG pattern: logs the full RRN (주민등록번호) at INFO level.
 * Guard must catch this and emit "RRN_IN_LOG" violation.
 * Violates no-rrn-logging rule (개인정보보호법 §15-§22).
 */
package com.example.fixture.no_rrn_logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void registerUser(String name, String rrn) {
        // VIOLATION: RRN (주민등록번호) written directly to log at INFO level.
        // Even TRACE/DEBUG logs are forbidden — logs are retained in aggregators.
        log.info("registering user {} with RRN: {}", name, rrn);

        // ... registration logic ...
    }

    public void verifyIdentity(String rrn) {
        // VIOLATION: debug-level is equally forbidden
        log.debug("verifying identity for rrn={}", rrn);
    }
}
