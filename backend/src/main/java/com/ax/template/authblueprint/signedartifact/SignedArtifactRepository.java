package com.ax.template.authblueprint.signedartifact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** NO delete method is declared — an artifact issuance is an append-only fact. */
public interface SignedArtifactRepository extends JpaRepository<SignedArtifact, UUID> {
}
