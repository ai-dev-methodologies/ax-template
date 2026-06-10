package com.ax.template.authblueprint.commentthread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, String entityId);

    /** COMMENT-HISTORY member read — through-root (CommentEdit owns no repository; HG-AGG-REPO). */
    @Query(
        "SELECT e FROM CommentEdit e WHERE e.commentId = :commentId ORDER BY e.editedAt ASC")
    List<CommentEdit> findEditsByCommentId(@Param("commentId") UUID commentId);
}
