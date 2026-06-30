package com.ax.template.authblueprint.tokenizedsecurities;

import org.springframework.stereotype.Component;

/**
 * One-way issuance state machine for a security token.
 * DRAFT → ISSUED (no reverse transition).
 * Sole status mutator — calls register.markIssued() (package-private).
 * Mirrors ThresholdRegisterStateMachine (single edge, no un-issue/reset).
 */
@Component
public class SecurityTokenIssuanceStateMachine {

    public void issue(SecurityTokenRegister register) {
        if (register.getIssuanceStatus() != IssuanceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Token is not in DRAFT state; current status: " + register.getIssuanceStatus());
        }
        register.markIssued();
    }
}
