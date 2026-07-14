package com.ax.template.authblueprint.duplicatesubmission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** NO delete method is declared — channels are define-once immutable config. */
public interface DuplicateKeyChannelRepository extends JpaRepository<DuplicateKeyChannel, UUID> {
}
