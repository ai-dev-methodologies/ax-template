package com.ax.template.authblueprint.practices;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for the {@link SoftDeletedRecord} fixture entity.
 *
 * <p>Used by {@link PracticesDemoController} and {@code BaseEntitySoftDeleteIT}
 * to exercise the PRACTICES-PERS-005 soft-delete contract:
 * <ul>
 *   <li>{@code deleteById(id)} triggers the {@code @SQLDelete} UPDATE → row retained
 *   <li>{@code findAll()} automatically excludes soft-deleted rows via {@code @Where}
 * </ul>
 */
public interface SoftDeletedRecordRepository extends JpaRepository<SoftDeletedRecord, UUID> {
}
