package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DataJpaTest
@Tag("PRACTICES")
@Tag("PRACTICES-PERS-004")
class PersistenceOptimisticLockingTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private VersionedAccountRepository accounts;

    @Test
    void practices_PERS_004_versionIncrementsOnUpdate() {
        VersionedAccount a = new VersionedAccount();
        a.setHolder("Alice");
        a.setBalance(100);
        VersionedAccount saved = accounts.saveAndFlush(a);
        long v0 = saved.getVersion();
        saved.setBalance(150);
        VersionedAccount updated = accounts.saveAndFlush(saved);
        assertThat(updated.getVersion()).isGreaterThan(v0);
    }

    @Test
    void practices_PERS_004_staleWriteThrowsOptimisticLockException() {
        // Persist a row + flush, then simulate two stale references racing on commit.
        VersionedAccount fresh = new VersionedAccount();
        fresh.setHolder("Bob");
        fresh.setBalance(500);
        VersionedAccount saved = accounts.saveAndFlush(fresh);
        Long id = saved.getId();
        em.detach(saved);

        // Reader 1: reads + modifies
        VersionedAccount r1 = accounts.findById(id).orElseThrow();
        r1.setBalance(600);
        accounts.saveAndFlush(r1);  // version bumped
        em.detach(r1);

        // Reader 2 started with the *original* version and now tries to commit.
        // It carries the stale @Version value → save must fail.
        VersionedAccount r2 = new VersionedAccount();
        // Hibernate enforces the version on flush via reflection — simplest reliable
        // reproduction: load again, force the version back to the pre-bump value via a
        // manual flush. Spring Data wraps OptimisticLockException as
        // ObjectOptimisticLockingFailureException.
        VersionedAccount conflict = accounts.findById(id).orElseThrow();
        // Step into the underlying JPA API to set the version to the older snapshot value.
        em.detach(conflict);
        // Apply a known-stale change using JPA's merge with the saved instance whose
        // version field is older than what's in the database now.
        saved.setBalance(700);   // saved still has the original version
        assertThatThrownBy(() -> {
            accounts.saveAndFlush(saved);
        }).isInstanceOfAny(
                ObjectOptimisticLockingFailureException.class,
                OptimisticLockException.class
        );
    }
}
