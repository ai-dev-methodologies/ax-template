package com.ax.template.authblueprint.identityverification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VerifiedIdentityRepository extends JpaRepository<VerifiedIdentity, UUID> {

    Page<VerifiedIdentity> findAllByProviderName(String providerName, Pageable pageable);
}
