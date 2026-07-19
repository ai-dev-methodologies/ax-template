package com.ax.template.authblueprint.tenantdocfe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CLEAN variant.
 *
 * Every repository read is scoped to the caller's tenant, resolved from
 * TenantContext.current() — never the plain findById/findAll finders. A
 * missing tenant context fails closed (IllegalStateException) rather than
 * silently returning cross-tenant data.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Optional<Document> getDocument(UUID id) {
        UUID tenantId = currentTenantOrThrow();
        return documentRepository.findByIdAndTenantId(id, tenantId);
    }

    public List<Document> listDocuments() {
        UUID tenantId = currentTenantOrThrow();
        return documentRepository.findAllByTenantId(tenantId);
    }

    private UUID currentTenantOrThrow() {
        return TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("TenantContext is empty — refusing to scope a query"));
    }
}
