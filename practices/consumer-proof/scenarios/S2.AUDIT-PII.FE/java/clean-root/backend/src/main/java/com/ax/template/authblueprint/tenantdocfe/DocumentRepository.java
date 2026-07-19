package com.ax.template.authblueprint.tenantdocfe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Tenant-scoped finders exist alongside the plain JpaRepository ones —
    // the catalog cannot stop a consumer from calling findById/findAll
    // instead of these.
    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Document> findAllByTenantId(UUID tenantId);
}
