package com.ax.template.authblueprint.tenantdocfe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * VIOLATING variant.
 *
 * A very natural AI-generated shortcut: call the plain JpaRepository finders
 * (findById / findAll) that Spring Data generates for free, instead of the
 * tenant-scoped finders declared on the same repository. Every caller
 * (regardless of which tenant is on the request) can read or enumerate
 * ANOTHER tenant's documents — a cross-tenant data leak with no code path
 * that ever consults TenantContext.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Optional<Document> getDocument(UUID id) {
        return documentRepository.findById(id);
    }

    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }
}
