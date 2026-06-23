package com.ax.template.authblueprint.uomconversion;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a material/conversion is recorded, never removed. */
public interface MaterialRepository extends JpaRepository<Material, UUID> {

    /** UOMCONV-VERSION-001 — the material row serializes the read-version / append-version sequence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Material m WHERE m.id = :id")
    Optional<Material> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — MaterialProperty / Conversion own no repository) ──

    @Query("SELECT p FROM MaterialProperty p WHERE p.materialId = :materialId ORDER BY p.version DESC")
    List<MaterialProperty> findProperties(@Param("materialId") UUID materialId);

    @Query("SELECT p FROM MaterialProperty p WHERE p.materialId = :materialId AND p.version = :version")
    Optional<MaterialProperty> findProperty(@Param("materialId") UUID materialId,
                                            @Param("version") long version);

    @Query("SELECT c FROM Conversion c WHERE c.id = :id")
    Optional<Conversion> findConversion(@Param("id") UUID id);

    @Query("SELECT c FROM Conversion c WHERE c.idempotencyBasis = :basis")
    Optional<Conversion> findConversionByBasis(@Param("basis") String basis);
}
