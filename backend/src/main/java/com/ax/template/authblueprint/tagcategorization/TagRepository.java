package com.ax.template.authblueprint.tagcategorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findBySlug(String slug);

    List<Tag> findByParentTagIdIsNullOrderByNameAsc();

    List<Tag> findByParentTagIdOrderByNameAsc(UUID parentTagId);

    long countByParentTagId(UUID parentTagId);
}
