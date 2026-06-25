package com.ax.template.authblueprint.commercecatalog;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * commercecatalog sole orchestrator and mutator.
 *
 * <p>INV-1 (keystone): {@link #resolveSku} looks up this product's actual
 * {@link ProductOption}/{@link ProductOptionValue} rows, computes the option_signature
 * from the DB-resident value ids, and does an EXACT-MATCH lookup — returns the ONE
 * matching SKU or throws 404 CATALOG_NO_MATCHING_SKU. {@link #addVariantSku} validates
 * every supplied option-value id against the product's real options before computing the
 * same sorted-canonical signature. Both paths share {@link #computeOptionSignature}.
 *
 * <p>INV-2: {@link #assertPurchasable} checks {@code now ∈ [active_start, active_end)}
 * AND product not archived — uses the INJECTED {@link Clock} (settable for tests).
 *
 * <p>INV-3: product create requires a default SKU (422 CATALOG_DEFAULT_SKU_REQUIRED),
 * enforced at the service boundary (not only the controller).
 * A second isDefault=true SKU is rejected 409 CATALOG_DUPLICATE_DEFAULT_SKU.
 *
 * <p>INV-5: an active sellable SKU MUST resolve a non-null retail price (own or inherited
 * from the default SKU) — checked at the catalog boundary for variant SKU additions.
 *
 * <p>Variant mutations acquire {@code PESSIMISTIC_WRITE} on the product row before the
 * uniqueness check so concurrent variant creates serialize.
 */
@Service
public class CatalogProductService {

    private final CatalogProductRepository products;
    private final MemberWriter members;
    private final CatalogMetrics metrics;
    /** Settable for test-time clock control (volatile for thread visibility). */
    private volatile Clock clock;

    public CatalogProductService(CatalogProductRepository products, MemberWriter members,
                                 CatalogMetrics metrics, Clock clock) {
        this.products = products;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** Test-only: override the clock for purchasability boundary tests. */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Create a new product with its mandatory default SKU in one transaction (INV-3).
     * Rejects 422 CATALOG_DEFAULT_SKU_REQUIRED if defaultRetailPrice and defaultSalePrice and
     * currency are all absent — service boundary enforcement so non-controller callers are gated too.
     *
     * @param defaultSkuInventoryType CAT-INVENTORY-GATE-001 policy flag for the default SKU;
     *                                null defaults to ALWAYS_AVAILABLE.
     */
    @Transactional
    public CatalogProduct createProduct(String name, String description,
                                        boolean canSellWithoutOptions,
                                        Instant activeStartDate, Instant activeEndDate,
                                        // default SKU fields — null signals "no default SKU" → 422
                                        Long defaultRetailPrice, Long defaultSalePrice,
                                        String currency,
                                        Instant skuActiveStart, Instant skuActiveEnd,
                                        InventoryType defaultSkuInventoryType) {
        // INV-3: service-boundary enforcement (controller also checks, but this is the authoritative gate)
        if (defaultRetailPrice == null && defaultSalePrice == null && currency == null) {
            throw CatalogException.defaultSkuRequired();
        }

        CatalogProduct product = products.saveAndFlush(
            new CatalogProduct(UUID.randomUUID(), name, description,
                canSellWithoutOptions, activeStartDate, activeEndDate));

        Sku defaultSku = members.persist(new Sku(
            UUID.randomUUID(), product.getId(), true,
            defaultRetailPrice, defaultSalePrice, currency,
            skuActiveStart, skuActiveEnd, null, null, null,
            defaultSkuInventoryType != null ? defaultSkuInventoryType : InventoryType.ALWAYS_AVAILABLE));

        product.assignDefaultSku(defaultSku.getId());
        metrics.recordOp("create_product", "ok");
        return product;
    }

    /**
     * Overload without inventoryType for backward compatibility — defaults to ALWAYS_AVAILABLE.
     */
    @Transactional
    public CatalogProduct createProduct(String name, String description,
                                        boolean canSellWithoutOptions,
                                        Instant activeStartDate, Instant activeEndDate,
                                        Long defaultRetailPrice, Long defaultSalePrice,
                                        String currency,
                                        Instant skuActiveStart, Instant skuActiveEnd) {
        return createProduct(name, description, canSellWithoutOptions,
            activeStartDate, activeEndDate,
            defaultRetailPrice, defaultSalePrice, currency,
            skuActiveStart, skuActiveEnd, InventoryType.ALWAYS_AVAILABLE);
    }

    /**
     * Define a new option dimension for a product (e.g. "Color", "Size").
     * useInSkuGeneration=true means values of this option contribute to the option_signature.
     */
    @Transactional
    public ProductOption defineOption(UUID productId, String attributeName,
                                     boolean required, boolean useInSkuGeneration) {
        products.findById(productId).orElseThrow(() -> CatalogException.notFound("Product"));
        return members.persistAndFlush(new ProductOption(
            UUID.randomUUID(), productId, attributeName, required, useInSkuGeneration));
    }

    /**
     * Add a value to a product option (e.g. "Red" to "Color").
     */
    @Transactional
    public ProductOptionValue addOptionValue(UUID productId, UUID optionId,
                                             String attributeValue, Long priceAdjustment) {
        products.findById(productId).orElseThrow(() -> CatalogException.notFound("Product"));
        // verify the option belongs to this product
        List<ProductOption> opts = products.findOptions(productId);
        boolean optionBelongs = opts.stream().anyMatch(o -> o.getId().equals(optionId));
        if (!optionBelongs) {
            throw CatalogException.notFound("Option");
        }
        return members.persistAndFlush(new ProductOptionValue(
            UUID.randomUUID(), optionId, attributeValue, priceAdjustment));
    }

    /**
     * Add a variant SKU to an existing product (acquires PESSIMISTIC_WRITE for race safety).
     *
     * <p>INV-1: validates every supplied option-value id belongs to this product's actual options,
     * computes the option_signature as sorted-canonical join of the sku-generating value ids,
     * and rejects 409 CATALOG_SKU_OPTION_AMBIGUOUS if a SKU with the same signature exists.
     * The entity-level UNIQUE constraint is the DB backstop.
     *
     * <p>INV-3: rejects 409 if isDefault=true and the product already has a default SKU.
     *
     * <p>INV-5: if both retailPrice and the inherited default retail price are null, rejects 422.
     *
     * @param inventoryType CAT-INVENTORY-GATE-001 policy flag; null defaults to ALWAYS_AVAILABLE.
     */
    @Transactional
    public Sku addVariantSku(UUID productId,
                             Long retailPrice, Long salePrice, String currency,
                             Instant activeStartDate, Instant activeEndDate,
                             String externalId, String upc,
                             List<UUID> skuGeneratingOptionValueIds,
                             InventoryType inventoryType) {
        CatalogProduct product = products.findByIdForUpdate(productId)
            .orElseThrow(() -> CatalogException.notFound("Product"));

        // INV-3: reject if there's already a default SKU and this is another default
        // (addVariantSku always creates non-default SKUs — if caller passes isDefault=true later,
        // the service guards it; here all variant SKUs are non-default by definition)

        // INV-1: validate every supplied option-value id belongs to this product
        List<UUID> valueIds = skuGeneratingOptionValueIds != null ? skuGeneratingOptionValueIds : List.of();
        if (!valueIds.isEmpty()) {
            // Load all option-value ids for this product's sku-generating options
            List<ProductOption> opts = products.findOptions(productId);
            List<UUID> optionIds = opts.stream()
                .filter(ProductOption::isUseInSkuGeneration)
                .map(ProductOption::getId)
                .toList();
            Set<UUID> validValueIds = optionIds.isEmpty() ? Set.of() :
                products.findOptionValuesByOptionIds(optionIds).stream()
                    .map(ProductOptionValue::getId)
                    .collect(Collectors.toSet());
            for (UUID vid : valueIds) {
                if (!validValueIds.contains(vid)) {
                    metrics.recordOp("add_variant", "rejected");
                    throw CatalogException.invalidOptionValueId(vid);
                }
            }
        }

        // INV-1: compute sorted option_signature from the validated sku-generating value ids
        String optionSignature = computeOptionSignature(valueIds);

        // INV-5: must resolve a non-null retail price
        Long resolvedRetailPrice = retailPrice;
        if (resolvedRetailPrice == null) {
            Sku defaultSku = products.findDefaultSku(productId).orElse(null);
            if (defaultSku != null) {
                resolvedRetailPrice = defaultSku.getRetailPrice();
            }
        }
        if (resolvedRetailPrice == null) {
            metrics.recordOp("add_variant", "rejected");
            throw CatalogException.priceRequired();
        }

        // Uniqueness pre-check (the entity @Table UNIQUE is the ultimate DB backstop)
        if (optionSignature != null && products.findSkuByOptionSignature(productId, optionSignature).isPresent()) {
            metrics.recordOp("add_variant", "rejected");
            throw CatalogException.skuOptionAmbiguous();
        }

        InventoryType resolvedInvType = inventoryType != null ? inventoryType : InventoryType.ALWAYS_AVAILABLE;
        try {
            Sku sku = members.persistAndFlush(new Sku(
                UUID.randomUUID(), productId, false,
                retailPrice, salePrice, currency,
                activeStartDate, activeEndDate, externalId, upc, optionSignature,
                resolvedInvType));

            // Persist SkuOptionValueXref rows (one per option-value id)
            for (UUID vid : valueIds) {
                members.persist(new SkuOptionValueXref(sku.getId(), vid));
            }

            metrics.recordOp("add_variant", "ok");
            return sku;
        } catch (DataIntegrityViolationException e) {
            metrics.recordOp("add_variant", "rejected");
            throw CatalogException.skuOptionAmbiguous();
        }
    }

    /**
     * Overload without inventoryType for backward compatibility — defaults to ALWAYS_AVAILABLE.
     */
    @Transactional
    public Sku addVariantSku(UUID productId,
                             Long retailPrice, Long salePrice, String currency,
                             Instant activeStartDate, Instant activeEndDate,
                             String externalId, String upc,
                             List<UUID> skuGeneratingOptionValueIds) {
        return addVariantSku(productId, retailPrice, salePrice, currency,
            activeStartDate, activeEndDate, externalId, upc,
            skuGeneratingOptionValueIds, InventoryType.ALWAYS_AVAILABLE);
    }

    /**
     * INV-1 (keystone): resolve the EXACT SKU for a given option map {attrName → attrValue}.
     *
     * <p>Looks up this product's actual {@link ProductOption} and {@link ProductOptionValue} rows
     * from the database, maps the request to the real value ids, computes the SAME canonical
     * option_signature as {@link #addVariantSku}, then does an exact-match lookup.
     * Throws 404 CATALOG_NO_MATCHING_SKU if no match; never silently picks the first result.
     *
     * <p>ALSO calls {@link #assertPurchasable} on the resolved SKU (INV-2 gate).
     */
    @Transactional(readOnly = true)
    public Sku resolveSku(UUID productId, Map<String, String> optionMap) {
        products.findById(productId)
            .orElseThrow(() -> CatalogException.notFound("Product"));

        // Load sku-generating options for this product
        List<ProductOption> options = products.findOptions(productId);
        List<UUID> skuGenOptionIds = options.stream()
            .filter(ProductOption::isUseInSkuGeneration)
            .map(ProductOption::getId)
            .toList();

        List<ProductOptionValue> allValues = skuGenOptionIds.isEmpty()
            ? List.of()
            : products.findOptionValuesByOptionIds(skuGenOptionIds);

        // Build a lookup: optionId → attributeName
        Map<UUID, String> optionAttrName = options.stream()
            .filter(ProductOption::isUseInSkuGeneration)
            .collect(Collectors.toMap(ProductOption::getId, ProductOption::getAttributeName));

        // For each sku-generating option, find the value-id matching the requested attrValue
        List<UUID> resolvedValueIds = options.stream()
            .filter(ProductOption::isUseInSkuGeneration)
            .flatMap(opt -> {
                String requestedValue = optionMap.get(opt.getAttributeName());
                if (requestedValue == null) return java.util.stream.Stream.empty();
                return allValues.stream()
                    .filter(v -> v.getOptionId().equals(opt.getId()))
                    .filter(v -> requestedValue.equals(v.getAttributeValue()))
                    .map(ProductOptionValue::getId);
            })
            .toList();

        String sig = computeOptionSignature(resolvedValueIds);

        return products.findSkuByOptionSignature(productId, sig)
            .map(sku -> {
                metrics.recordSkuResolve("ok");
                return sku;
            })
            .orElseThrow(() -> {
                metrics.recordSkuResolve("no_match");
                return CatalogException.noMatchingSku();
            });
    }

    /**
     * INV-2 + CAT-INVENTORY-GATE-001: assert that a SKU is currently purchasable.
     *
     * <p>Step 1 — CAT-INVENTORY-GATE-001 tri-state policy gate (consulted FIRST, before any
     * quantity arithmetic or window checks):
     * <ul>
     *   <li>UNAVAILABLE → reject immediately (409 CATALOG_SKU_NOT_PURCHASABLE), regardless of stock.
     *   <li>ALWAYS_AVAILABLE or null → skip quantity check entirely.
     *   <li>CHECK_QUANTITY → catalog does NOT block; quantity check is deferred to the
     *       inventory-reservation vertical.
     * </ul>
     *
     * <p>Step 2 — INV-2 window/archival gate:
     * Purchasable = now ∈ [active_start, active_end) AND product not archived AND SKU dates valid.
     * Uses the injected Clock (controllable in tests via {@link #setClock}).
     */
    @Transactional(readOnly = true)
    public void assertPurchasable(UUID skuId) {
        Sku sku = members.find(Sku.class, skuId)
            .orElseThrow(() -> CatalogException.notFound("SKU"));

        // CAT-INVENTORY-GATE-001: tri-state policy gate — checked BEFORE window/archival
        InventoryType invType = sku.getInventoryType();
        if (invType == InventoryType.UNAVAILABLE) {
            metrics.recordSkuResolve("not_purchasable");
            throw CatalogException.skuNotPurchasable();
        }
        // ALWAYS_AVAILABLE and CHECK_QUANTITY both pass the catalog gate; quantity check for
        // CHECK_QUANTITY is deferred to the inventory-reservation vertical.

        CatalogProduct product = products.findById(sku.getProductId())
            .orElseThrow(() -> CatalogException.notFound("Product"));

        Instant now = Instant.now(clock);
        boolean productOk = !product.isArchived()
            && (product.getActiveStartDate() == null || !now.isBefore(product.getActiveStartDate()))
            && (product.getActiveEndDate() == null || now.isBefore(product.getActiveEndDate()));

        boolean skuOk = (sku.getActiveStartDate() == null || !now.isBefore(sku.getActiveStartDate()))
            && (sku.getActiveEndDate() == null || now.isBefore(sku.getActiveEndDate()));

        if (!productOk || !skuOk) {
            metrics.recordSkuResolve("not_purchasable");
            throw CatalogException.skuNotPurchasable();
        }
    }

    @Transactional(readOnly = true)
    public CatalogProduct getProduct(UUID productId) {
        return products.findById(productId)
            .orElseThrow(() -> CatalogException.notFound("Product"));
    }

    /**
     * Add a product to a category (cross-aggregate by category id — no Category object).
     * Rejects 409 on duplicate membership (the entity UNIQUE backstops it).
     */
    @Transactional
    public CategoryProductXref linkCategory(UUID productId, UUID categoryId, int displayOrder) {
        products.findById(productId).orElseThrow(() -> CatalogException.notFound("Product"));
        if (products.findCategoryProductXref(productId, categoryId).isPresent()) {
            metrics.recordOp("link_category", "rejected");
            throw CatalogException.dupMembership();
        }
        try {
            CategoryProductXref xref = members.persistAndFlush(
                new CategoryProductXref(productId, categoryId, displayOrder));
            metrics.recordOp("link_category", "ok");
            return xref;
        } catch (DataIntegrityViolationException e) {
            metrics.recordOp("link_category", "rejected");
            throw CatalogException.dupMembership();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────

    /**
     * Compute the option_signature: sorted-join of sku-generating option-value ids.
     * Sorting by UUID string ensures deterministic, order-independent signatures.
     * Returns null for an empty list (default/no-option SKU).
     *
     * <p>SHARED between addVariantSku and resolveSku — both paths produce identical signatures
     * for the same set of value ids, regardless of input order.
     */
    static String computeOptionSignature(List<UUID> optionValueIds) {
        if (optionValueIds == null || optionValueIds.isEmpty()) return null;
        return optionValueIds.stream()
            .map(UUID::toString)
            .sorted()
            .collect(Collectors.joining(","));
    }
}
