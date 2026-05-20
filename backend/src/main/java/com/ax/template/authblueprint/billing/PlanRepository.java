package com.ax.template.authblueprint.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, String> {
    Page<Plan> findAllByDeletedAtIsNull(Pageable pageable);
}
