package com.ax.template.authblueprint.ledgeradmin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AuditLedgerRepository — bounded reads only (a page, never the whole ledger).
 * See practices/rules/api-pagination-pageable.md.
 */
public interface AuditLedgerRepository extends JpaRepository<AuditLedgerEntry, Long> {

    Page<AuditLedgerEntry> findAllByEntityRef(String entityRef, Pageable pageable);
}
