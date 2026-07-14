package com.ax.template.authblueprint.identityverification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerifiedIdentityRepository extends JpaRepository<VerifiedIdentity, UUID> {

    Page<VerifiedIdentity> findAllByProviderName(String providerName, Pageable pageable);

    /** IDV-CONCORDANCE-001 — every prior row sharing this ci, to check the paired di matches. */
    List<VerifiedIdentity> findByCi(String ci);

    /** IDV-CONCORDANCE-001 — every prior row sharing this di, to check the paired ci matches. */
    List<VerifiedIdentity> findByDi(String di);
}
