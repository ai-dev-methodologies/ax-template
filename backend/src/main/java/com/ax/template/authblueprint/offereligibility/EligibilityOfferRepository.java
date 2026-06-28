package com.ax.template.authblueprint.offereligibility;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Root repository for the {@link EligibilityOffer} aggregate. The customer-xref allow-list is an
 * {@code @ElementCollection} loaded through the root, so it owns no repository of its own
 * (AX-DDD-MEMBER-REPO). All access is by offer id; there is no unbounded collection finder.
 */
public interface EligibilityOfferRepository extends JpaRepository<EligibilityOffer, UUID> {
}
