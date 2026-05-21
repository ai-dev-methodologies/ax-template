package com.ax.template.authblueprint.tagcategorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagAttachmentRepository extends JpaRepository<TagAttachment, UUID> {

    Optional<TagAttachment> findByTagIdAndEntityTypeAndEntityId(UUID tagId,
                                                                String entityType,
                                                                String entityId);

    /**
     * Tags currently attached to a given (entityType, entityId) pair, ordered by Tag.name.
     * Trace: TAG-ATTACH-003.
     */
    @Query(
        "SELECT t FROM Tag t " +
        "WHERE t.id IN (" +
        "  SELECT a.tagId FROM TagAttachment a " +
        "  WHERE a.entityType = :entityType AND a.entityId = :entityId" +
        ") " +
        "ORDER BY t.name ASC"
    )
    List<Tag> findTagsByEntity(@Param("entityType") String entityType,
                                @Param("entityId") String entityId);

    @Modifying
    @Query("DELETE FROM TagAttachment a WHERE a.tagId = :tagId AND a.entityType = :entityType AND a.entityId = :entityId")
    int deleteByTagAndEntity(@Param("tagId") UUID tagId,
                              @Param("entityType") String entityType,
                              @Param("entityId") String entityId);

    @Modifying
    @Query("DELETE FROM TagAttachment a WHERE a.tagId = :tagId")
    int deleteByTagId(@Param("tagId") UUID tagId);
}
