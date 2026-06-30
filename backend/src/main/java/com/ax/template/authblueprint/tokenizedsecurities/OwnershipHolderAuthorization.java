package com.ax.template.authblueprint.tokenizedsecurities;

import org.springframework.stereotype.Component;

/**
 * Default HolderAuthorization — deny-by-default ownership registry.
 * Absence of a HolderOwnership row means no principal controls the holder (fail-closed).
 * A fork swaps this for on-chain identity (ERC-3643 ONCHAINID).
 */
@Component
public class OwnershipHolderAuthorization implements HolderAuthorization {

    private final HolderOwnershipRepository ownerships;

    public OwnershipHolderAuthorization(HolderOwnershipRepository ownerships) {
        this.ownerships = ownerships;
    }

    @Override
    public boolean controls(String callerPrincipal, String holderId) {
        return ownerships.findByHolderId(holderId)
                .map(o -> o.getOwnerPrincipal().equals(callerPrincipal))
                .orElse(false);  // fail-closed: no row ⇒ not controlled
    }
}
