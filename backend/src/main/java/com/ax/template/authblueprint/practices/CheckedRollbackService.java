package com.ax.template.authblueprint.practices;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixtures for PRACTICES-TX-003: Spring's default rollback policy only triggers on
 * unchecked exceptions. A checked exception escaping a @Transactional method commits
 * the transaction unless `rollbackFor` is declared explicitly. Reflection-level test
 * asserts the annotation carries the right `rollbackFor` value.
 */
@Service
public class CheckedRollbackService {

    /** Anti-pattern: default rollback policy ignores checked exceptions. */
    @Transactional
    public void naiveCheckedThrow() throws IOException {
        throw new IOException("simulated checked failure");
    }

    /** Correct: declare rollbackFor explicitly. */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackForChecked() throws IOException {
        throw new IOException("simulated checked failure");
    }
}
