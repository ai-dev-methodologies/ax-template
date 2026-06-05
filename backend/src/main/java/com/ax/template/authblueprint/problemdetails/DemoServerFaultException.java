package com.ax.template.authblueprint.problemdetails;

/**
 * A dedicated signal for the reference 5xx path (PROBLEM-TRACE-001). The reference advice maps
 * THIS type — not the broad {@code Exception} — so the package-scoped, HIGHEST_PRECEDENCE
 * {@link ProblemDemoAdvice} never masks the richer framework handlers in
 * {@code common.GlobalProblemDetailAdvice} (a malformed body stays a 400, not a 500). Its
 * message deliberately carries internal context (a stack frame + SQLSTATE) so the test can
 * prove that context is logged but NEVER leaked into the client {@code detail}.
 *
 * <p>Spec: specs/problem-details-l0.yaml#PROBLEM-TRACE-001.
 */
public class DemoServerFaultException extends RuntimeException {

    public DemoServerFaultException(String message) {
        super(message);
    }
}
