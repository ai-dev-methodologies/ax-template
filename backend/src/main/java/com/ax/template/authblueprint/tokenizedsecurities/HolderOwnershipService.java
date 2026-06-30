package com.ax.template.authblueprint.tokenizedsecurities;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holder ownership registration — first-claim-wins, idempotent for same principal,
 * 409 when a different principal attempts to claim an already-owned holder.
 */
@Service
public class HolderOwnershipService {

    private final HolderOwnershipRepository ownerships;
    private final Clock clock;

    public HolderOwnershipService(HolderOwnershipRepository ownerships, Clock clock) {
        this.ownerships = ownerships;
        this.clock = clock;
    }

    @Transactional
    public HolderOwnership claim(String holderId, String callerPrincipal) {
        return ownerships.findByHolderId(holderId)
                .map(existing -> {
                    if (!existing.getOwnerPrincipal().equals(callerPrincipal)) {
                        throw TokenizedSecuritiesException.holderAlreadyOwned();
                    }
                    return existing;  // idempotent — same principal re-claiming
                })
                .orElseGet(() -> {
                    try {
                        return ownerships.saveAndFlush(
                                new HolderOwnership(holderId, callerPrincipal, Instant.now(clock)));
                    } catch (DataIntegrityViolationException e) {
                        // concurrent race: another thread inserted first; re-read to classify
                        HolderOwnership raced = ownerships.findByHolderId(holderId)
                                .orElseThrow(TokenizedSecuritiesException::notFound);
                        if (!raced.getOwnerPrincipal().equals(callerPrincipal)) {
                            throw TokenizedSecuritiesException.holderAlreadyOwned();
                        }
                        return raced;  // concurrent identical claim — idempotent
                    }
                });
    }
}
