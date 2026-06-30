package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.UUID;

import org.springframework.stereotype.Component;

/** Fail-closed default — eligible only if an explicit grant row exists. */
@Component
public class AllowlistInvestorEligibility implements InvestorEligibility {

    private final EligibleInvestorRepository grants;

    public AllowlistInvestorEligibility(EligibleInvestorRepository grants) {
        this.grants = grants;
    }

    @Override
    public boolean isEligible(UUID registerId, String holderId) {
        return grants.existsByRegisterIdAndHolderId(registerId, holderId);
    }
}
