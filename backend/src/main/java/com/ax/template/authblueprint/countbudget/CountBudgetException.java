package com.ax.template.authblueprint.countbudget;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for periodic-count-budget. status + RFC 9457 type + machine-readable code.
 */
public class CountBudgetException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CountBudgetException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static CountBudgetException notFound() {
        return new CountBudgetException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Count-budget policy or period not found");
    }

    public static CountBudgetException duplicateSubject() {
        return new CountBudgetException(HttpStatus.CONFLICT,
            "urn:problem:periodic-count-budget-duplicate", "PCB_DUPLICATE_SUBJECT",
            "A count-budget policy for this subject key already exists");
    }

    public static CountBudgetException invalidValue() {
        return new CountBudgetException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:periodic-count-budget-invalid-value", "PCB_INVALID_VALUE",
            "cap must be a positive integer");
    }

    /** PCB-CONSUME-001 — the current period's captured cap has already been reached. */
    public static CountBudgetException budgetExhausted() {
        return new CountBudgetException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:periodic-count-budget-exhausted", "PCB_BUDGET_EXHAUSTED",
            "The current period's budget cap has been reached");
    }
}
