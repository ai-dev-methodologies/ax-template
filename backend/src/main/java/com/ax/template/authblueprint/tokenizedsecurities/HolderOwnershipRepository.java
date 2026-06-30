package com.ax.template.authblueprint.tokenizedsecurities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HolderOwnershipRepository extends JpaRepository<HolderOwnership, UUID> {
    Optional<HolderOwnership> findByHolderId(String holderId);
    boolean existsByHolderId(String holderId);
}
