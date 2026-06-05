package com.ax.template.authblueprint.softdelete;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link SoftDeleteNote}. Cascade soft-delete/restore + purge operate over the
 * unfiltered child set (they must see tombstoned children); normal reads use the live finder.
 */
public interface SoftDeleteNoteRepository extends JpaRepository<SoftDeleteNote, UUID> {

    List<SoftDeleteNote> findByAccountId(UUID accountId);

    List<SoftDeleteNote> findByAccountIdAndDeletedAtIsNull(UUID accountId);
}
