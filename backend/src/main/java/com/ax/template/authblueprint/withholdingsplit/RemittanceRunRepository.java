package com.ax.template.authblueprint.withholdingsplit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** NO delete method — a remittance run, once collected, is frozen for its period (WHT-REMIT-003). */
public interface RemittanceRunRepository extends JpaRepository<RemittanceRun, UUID> {

    Optional<RemittanceRun> findByPeriod(String period);
}
