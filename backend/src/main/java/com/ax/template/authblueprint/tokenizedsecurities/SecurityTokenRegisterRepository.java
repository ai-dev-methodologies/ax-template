package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityTokenRegisterRepository extends JpaRepository<SecurityTokenRegister, UUID> {

    Optional<SecurityTokenRegister> findByTokenCode(String tokenCode);

    boolean existsByTokenCode(String tokenCode);

    boolean existsByUnderlyingAssetId(String underlyingAssetId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SecurityTokenRegister r WHERE r.tokenCode = :tokenCode")
    Optional<SecurityTokenRegister> findByTokenCodeForUpdate(@Param("tokenCode") String tokenCode);
}
