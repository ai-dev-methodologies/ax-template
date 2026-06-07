package com.ax.template.authblueprint.transformation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransformationRunRepository extends JpaRepository<TransformationRun, UUID> {
}
