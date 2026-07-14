package com.ax.template.authblueprint.rangeownership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** NO delete method is declared — a registered range block is immutable and permanent. */
public interface RangeBlockRepository extends JpaRepository<RangeBlock, UUID> {

    List<RangeBlock> findAll();

    /** RNG-CONTAINMENT-001 — blocks owned by {@code ownerRef} that contain {@code identifierValue}. */
    @Query("SELECT b FROM RangeBlock b WHERE b.ownerRef = :ownerRef"
        + " AND b.rangeStart <= :identifierValue AND :identifierValue < b.rangeEnd")
    List<RangeBlock> findOwnedContaining(@Param("ownerRef") String ownerRef, @Param("identifierValue") long identifierValue);

    /** RNG-PORT-003 — is {@code ownerRef} a recognized numbering-plan participant (owns ANY block)? */
    boolean existsByOwnerRef(String ownerRef);
}
