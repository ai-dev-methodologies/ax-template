package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibleInvestorRepository extends JpaRepository<EligibleInvestor, UUID> {

    boolean existsByRegisterIdAndHolderId(UUID registerId, String holderId);

    Optional<EligibleInvestor> findByRegisterIdAndHolderId(UUID registerId, String holderId);
}
