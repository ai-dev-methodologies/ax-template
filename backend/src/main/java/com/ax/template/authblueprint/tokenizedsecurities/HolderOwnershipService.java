package com.ax.template.authblueprint.tokenizedsecurities;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holder ownership registration — first-claim-wins, idempotent for same principal,
 * 409 when a different principal attempts to claim an already-owned holder.
 */
@Service
public class HolderOwnershipService {

    /** Result of a claim attempt: the ownership record and whether it was newly created. */
    public record ClaimResult(HolderOwnership ownership, boolean created) {}

    private final HolderOwnershipRepository ownerships;
    private final Clock clock;

    public HolderOwnershipService(HolderOwnershipRepository ownerships, Clock clock) {
        this.ownerships = ownerships;
        this.clock = clock;
    }

    /**
     * Read the current owner of a holder — 200 if claimed, empty if unclaimed (caller maps to 404).
     */
    @Transactional(readOnly = true)
    public Optional<HolderOwnership> findOwner(String holderId) {
        return ownerships.findByHolderId(holderId);
    }

    /**
     * First-claim-wins ownership registration.
     * Returns {@link ClaimResult#created()} == true for a first claim (→ 201 Created),
     * false for an idempotent re-claim by the same principal (→ 200 OK).
     * Throws 409 if a different principal already owns the holder.
     */
    @Transactional
    public ClaimResult claim(String holderId, String callerPrincipal) {
        return ownerships.findByHolderId(holderId)
                .map(existing -> {
                    if (!existing.getOwnerPrincipal().equals(callerPrincipal)) {
                        throw TokenizedSecuritiesException.holderAlreadyOwned();
                    }
                    return new ClaimResult(existing, false);  // idempotent re-claim → 200
                })
                .orElseGet(() -> {
                    try {
                        HolderOwnership saved = ownerships.saveAndFlush(
                                new HolderOwnership(holderId, callerPrincipal, Instant.now(clock)));
                        return new ClaimResult(saved, true);  // first claim → 201
                    } catch (DataIntegrityViolationException e) {
                        // concurrent race: another thread inserted first; re-read to classify
                        HolderOwnership raced = ownerships.findByHolderId(holderId)
                                .orElseThrow(TokenizedSecuritiesException::notFound);
                        if (!raced.getOwnerPrincipal().equals(callerPrincipal)) {
                            throw TokenizedSecuritiesException.holderAlreadyOwned();
                        }
                        return new ClaimResult(raced, false);  // concurrent identical claim → 200
                    }
                });
    }
}
