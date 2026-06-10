package com.ax.template.authblueprint.emailoutbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {

    /** Forensic member read — "what did template X look like at version N" (through-root; HG-AGG-REPO). */
    @Query(
        "SELECT h FROM EmailTemplateHistory h WHERE h.templateCode = :code AND h.version = :version")
    Optional<EmailTemplateHistory> findHistoryAtVersion(
        @Param("code") String templateCode,
        @Param("version") int version);

}
