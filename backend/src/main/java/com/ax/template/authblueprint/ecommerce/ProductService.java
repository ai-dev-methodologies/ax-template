package com.ax.template.authblueprint.ecommerce;

import com.ax.template.authblueprint.auditlog.Audited;
import com.ax.template.authblueprint.auditlog.ResourceId;
import com.ax.template.authblueprint.search.SearchIndexService;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Product CRUD + search indexing.
 * <p>
 * Trace:
 * <ul>
 *   <li>ECOM-INV-003 — mutations are @Audited (audit-log R14)</li>
 *   <li>Search composition — every create/update calls
 *       {@link SearchIndexService#index} so products are immediately searchable
 *       through {@code /api/v1/search} (R17).</li>
 * </ul>
 */
@Service
public class ProductService {

    /** Tenant slug used for the search backend in single-tenant mode (recipe `tenant_model: single`). */
    public static final String DEFAULT_TENANT = "default";

    /** Search domain key (per blueprints/search-manifest.yaml). */
    public static final String SEARCH_DOMAIN = "product";

    private final ProductRepository products;
    private final SearchIndexService search;

    public ProductService(ProductRepository products, SearchIndexService search) {
        this.products = products;
        this.search = search;
    }

    @Audited(action = "PRODUCT_CREATE", resourceType = "product")
    @Transactional
    public Product create(@ResourceId String ownerUserId, String name, String description,
                          long price, String currency, int stock, String imageFileId) {
        Product product = Product.create(ownerUserId, name, description, price, currency, stock, imageFileId);
        Product saved = products.save(product);
        indexInSearch(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Product> listActive(org.springframework.data.domain.Pageable pageable) {
        return products.findAllByStatusAndDeletedAtIsNull(ProductStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Product get(String id) {
        return products.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new EcommerceException.ProductNotFound(id));
    }

    @Audited(action = "PRODUCT_UPDATE", resourceType = "product")
    @Transactional
    public Product update(@ResourceId String id, String requesterUserId,
                          String name, String description, Long price, Integer stock, String imageFileId) {
        Product p = get(id);
        if (!p.getOwnerUserId().equals(requesterUserId)) {
            throw new EcommerceException.NotProductOwner(id);
        }
        p.update(name, description, price, stock, imageFileId);
        Product saved = products.save(p);
        indexInSearch(saved);
        return saved;
    }

    @Audited(action = "PRODUCT_DELETE", resourceType = "product")
    @Transactional
    public void delete(@ResourceId String id, String requesterUserId) {
        Product p = get(id);
        if (!p.getOwnerUserId().equals(requesterUserId)) {
            throw new EcommerceException.NotProductOwner(id);
        }
        p.setStatus(ProductStatus.INACTIVE);
        products.save(p);
        // Remove from search index when deactivated.
        search.delete(UUID.fromString(p.getId()), DEFAULT_TENANT);
    }

    /** Decrement stock atomically; used during checkout. */
    @Transactional
    public void decrementStock(String productId, int quantity) {
        Product p = get(productId);
        p.decrementStock(quantity);
        products.save(p);
    }

    private void indexInSearch(Product p) {
        String content = p.getName() + " " + (p.getDescription() == null ? "" : p.getDescription());
        String metadata = "{\"price\":" + p.getPrice() + ",\"currency\":\"" + p.getCurrency()
            + "\",\"stock\":" + p.getStock() + "}";
        // SearchIndexDocument id is UUID; reuse the product's id (already a UUID string).
        search.index(DEFAULT_TENANT, UUID.fromString(p.getId()), SEARCH_DOMAIN, content, metadata);
    }
}
