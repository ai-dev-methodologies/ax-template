package com.ax.template.authblueprint.rangeownership;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RangeRegistryLockRepository extends JpaRepository<RangeRegistryLock, String> {

    /** RNG-NONOVERLAP-002 keystone — serializes the check-then-insert block registration sequence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM RangeRegistryLock l WHERE l.id = :id")
    Optional<RangeRegistryLock> lockForUpdate(@Param("id") String id);
}
