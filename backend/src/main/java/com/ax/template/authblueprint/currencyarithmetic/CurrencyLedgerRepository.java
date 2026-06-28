package com.ax.template.authblueprint.currencyarithmetic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Root repository for the {@link CurrencyLedger} aggregate. The conversion trail is an
 * {@code @ElementCollection} loaded through the root, so it owns no repository of its own
 * (AX-DDD-MEMBER-REPO). All access is by ledger id; there is no unbounded collection finder.
 */
public interface CurrencyLedgerRepository extends JpaRepository<CurrencyLedger, UUID> {
}
