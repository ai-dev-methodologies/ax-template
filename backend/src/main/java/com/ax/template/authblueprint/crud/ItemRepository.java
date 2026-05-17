package com.ax.template.authblueprint.crud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {
    Page<ItemEntity> findByOwnerIdAndDeletedFalse(UUID ownerId, Pageable pageable);
    Optional<ItemEntity> findByIdAndOwnerIdAndDeletedFalse(UUID id, UUID ownerId);
}
