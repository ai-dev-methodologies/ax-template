package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibleInvestorRepository extends JpaRepository<EligibleInvestor, UUID> {

    boolean existsByRegisterIdAndHolderId(UUID registerId, String holderId);

    Optional<EligibleInvestor> findByRegisterIdAndHolderId(UUID registerId, String holderId);

    /** READ-ELIGIBLE-001: paginated list for a given register. Uses findBy* prefix
     *  (not findAll*) to satisfy the ArchUnit unbounded-list guard. */
    Page<EligibleInvestor> findByRegisterId(UUID registerId, Pageable pageable);
}
