package com.ax.template.authblueprint.commentthread;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentEditRepository extends JpaRepository<CommentEdit, UUID> {

    List<CommentEdit> findByCommentIdOrderByEditedAtAsc(UUID commentId);
}
