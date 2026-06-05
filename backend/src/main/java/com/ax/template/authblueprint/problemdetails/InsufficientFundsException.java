package com.ax.template.authblueprint.problemdetails;

import java.math.BigDecimal;
import java.util.List;

/**
 * A domain problem that carries STRUCTURED context — the canonical case for
 * PROBLEM-EXTENSION-001. The balance and the participating accounts are conveyed as
 * named, typed fields, NOT stuffed into a free-text {@code detail} string. The advice
 * surfaces them as top-level RFC 9457 extension members ({@code balance}, {@code accounts})
 * that are siblings of {@code type}/{@code title}/{@code status}/{@code detail}/{@code instance}.
 *
 * <p>Anchored to RFC 9457 §3.2: "Problem type definitions MAY extend the problem details
 * object with additional members that are specific to that problem type."
 *
 * <p>Spec: specs/problem-details-l0.yaml#PROBLEM-EXTENSION-001.
 */
public class InsufficientFundsException extends RuntimeException {

    /** Registered type slug (PROBLEM-TYPE-001). */
    public static final String SLUG = ProblemTypeRegistry.INSUFFICIENT_FUNDS;

    private final BigDecimal balance;
    private final List<String> accounts;

    public InsufficientFundsException(BigDecimal balance, List<String> accounts) {
        super("insufficient funds"); // never surfaced to the client; detail is localized via MessageSource
        this.balance = balance;
        this.accounts = List.copyOf(accounts);
    }

    public BigDecimal balance() {
        return balance;
    }

    public List<String> accounts() {
        return accounts;
    }
}
