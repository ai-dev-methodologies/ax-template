package com.ax.template.authblueprint.tokenizedsecurities;

/**
 * Caller-to-holder authorization seam (HOLDER-AUTHZ, specs/tokenized-securities-l0.yaml).
 * Default impl: OwnershipHolderAuthorization — deny-by-default ownership registry.
 * A fork swaps this for on-chain identity (ERC-3643 ONCHAINID).
 */
public interface HolderAuthorization {
    /**
     * @return true ONLY when callerPrincipal controls holderId;
     *         absence of a claim returns false (fail-closed).
     */
    boolean controls(String callerPrincipal, String holderId);
}
