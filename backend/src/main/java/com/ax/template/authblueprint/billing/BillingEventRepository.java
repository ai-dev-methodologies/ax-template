package com.ax.template.authblueprint.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingEventRepository extends JpaRepository<BillingEvent, String> {

    /** BILLING-IDEMP-001 — duplicate provider_event_id returns the original. */
    Optional<BillingEvent> findByProviderEventId(String providerEventId);
}
