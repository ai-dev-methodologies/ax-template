package com.ax.template.authblueprint.duplicatesubmission;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link Submission#getStatus()} / {@link Submission#getActiveKey()}.
 * DUPKEY-WITHDRAWN-003 — ACTIVE is the only non-terminal state; WITHDRAWN and REJECTED are both
 * terminal with no edge back out (double-withdraw / double-reject is a 409).
 */
@Component
public class SubmissionStateMachine {

    private static final Map<SubmissionStatus, Set<SubmissionStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(SubmissionStatus.class);
        ALLOWED.put(SubmissionStatus.ACTIVE,
            EnumSet.of(SubmissionStatus.WITHDRAWN, SubmissionStatus.REJECTED));
        ALLOWED.put(SubmissionStatus.WITHDRAWN, EnumSet.noneOf(SubmissionStatus.class));
        ALLOWED.put(SubmissionStatus.REJECTED, EnumSet.noneOf(SubmissionStatus.class));
    }

    /** ACTIVE → WITHDRAWN — releases the natural key. */
    public void withdraw(Submission submission) {
        assertTransition(submission.getStatus(), SubmissionStatus.WITHDRAWN);
        submission.release(SubmissionStatus.WITHDRAWN);
    }

    /** ACTIVE → REJECTED — releases the natural key. */
    public void reject(Submission submission) {
        assertTransition(submission.getStatus(), SubmissionStatus.REJECTED);
        submission.release(SubmissionStatus.REJECTED);
    }

    private static void assertTransition(SubmissionStatus from, SubmissionStatus to) {
        Set<SubmissionStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(SubmissionStatus.class));
        if (!allowed.contains(to)) {
            throw DuplicateSubmissionException.illegalTransition(from, to);
        }
    }
}
