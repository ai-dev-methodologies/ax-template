package com.ax.template.authblueprint.derivedstatement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** No update/delete method is declared — a statement is generated once and only ever appended. */
public interface DerivedStatementRepository extends JpaRepository<DerivedStatement, UUID> {

    /** STMT-DERIVE-001 — the identity: the SAME (subject, period, basisHash) resolves to this row. */
    Optional<DerivedStatement> findBySubjectAndPeriodAndBasisHash(String subject, String period, String basisHash);

    /** The latest version for a (subject, period) — determines the next version number to append. */
    Optional<DerivedStatement> findTopBySubjectAndPeriodOrderByVersionNoDesc(String subject, String period);

    List<DerivedStatement> findBySubjectAndPeriodOrderByVersionNoAsc(String subject, String period);
}
