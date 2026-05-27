package com.ax.template.authblueprint.emailoutbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * R60 iter1 F10 closure via Wave D2 — immutable history of every
 * {@link EmailTemplate} state across {@code upsertTemplate} updates.
 */
public interface EmailTemplateHistoryRepository extends JpaRepository<EmailTemplateHistory, UUID> {

    /** Forensic lookup — "what did template X look like at version N". */
    Optional<EmailTemplateHistory> findByTemplateCodeAndVersion(String templateCode, int version);

    /** Audit listing — every snapshot for the given template, newest first. */
    List<EmailTemplateHistory> findByTemplateCodeOrderByVersionDesc(String templateCode);
}
