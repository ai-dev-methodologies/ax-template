package fixtures.base_repository.fail_class_not_interface;

// FIXTURE: fail_class_not_interface
// EXPECTED_VIOLATION: REPOSITORY_IS_CLASS_NOT_JPAREPOSITORY_INTERFACE
// RULE: testing-archunit-repository-shape.md (PRACTICES-TEST-004)
//
// This repository violates the rule: it is a concrete class that hand-rolls
// data access instead of extending Spring Data's JpaRepository<T, ID>.
// Classes named *Repository that are not JpaRepository interfaces bypass
// Spring Data's query derivation, transaction defaults, and exception translation.
// An archunit rule asserting *Repository implements JpaRepository will FAIL
// against this fixture.

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

// VIOLATION: class (should be interface), no JpaRepository extension
@Repository
public class ItemRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Object> findAll() {
        return em.createQuery("from Item").getResultList();
    }
}
