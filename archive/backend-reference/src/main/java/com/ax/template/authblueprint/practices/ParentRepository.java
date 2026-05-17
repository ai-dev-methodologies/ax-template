package com.ax.template.authblueprint.practices;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    @Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children")
    List<Parent> findAllWithChildren();

    /**
     * Annotation-driven equivalent of the JPQL JOIN FETCH above. Spring Data JPA reads
     * the @EntityGraph attribute paths and adds the equivalent fetch hint to the query,
     * keeping the JPQL itself focused on filtering.
     */
    @EntityGraph(attributePaths = {"children"})
    @Query("SELECT p FROM Parent p")
    List<Parent> findAllWithChildrenViaEntityGraph();
}
