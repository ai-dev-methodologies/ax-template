package com.ax.template.authblueprint.commercecatalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Thin REST controller for commercecatalog.
 * Caller identity (for writes) is always from Authentication — never from the request body.
 * All domain logic is in {@link CatalogProductService} and {@link CategoryService}.
 */
@RestController
public class CatalogController {

    // ── Request/Response records ──────────────────────────────────────────────────────

    public record CreateProductReq(
        @NotBlank @Size(max = 400) String name,
        @Size(max = 2000) String description,
        boolean canSellWithoutOptions,
        Instant activeStartDate,
        Instant activeEndDate,
        // Default SKU — required (INV-3); null triggers 422 CATALOG_DEFAULT_SKU_REQUIRED (not @NotNull 400)
        DefaultSkuReq defaultSku
    ) {}

    public record DefaultSkuReq(
        Long retailPrice,
        Long salePrice,
        @Size(max = 3) String currency,
        Instant activeStartDate,
        Instant activeEndDate
    ) {}

    public record AddVariantSkuReq(
        Long retailPrice,
        Long salePrice,
        @Size(max = 3) String currency,
        Instant activeStartDate,
        Instant activeEndDate,
        @Size(max = 200) String externalId,
        @Size(max = 50) String upc,
        java.util.List<UUID> skuGeneratingOptionValueIds
    ) {}

    public record ResolveSkuReq(
        @NotNull Map<String, String> optionMap
    ) {}

    public record DefineOptionReq(
        @NotBlank @Size(max = 200) String attributeName,
        boolean required,
        boolean useInSkuGeneration
    ) {}

    public record AddOptionValueReq(
        @NotBlank @Size(max = 200) String attributeValue,
        Long priceAdjustment
    ) {}

    public record CreateCategoryReq(
        @NotBlank @Size(max = 400) String name,
        Instant activeStartDate,
        Instant activeEndDate,
        UUID parentId
    ) {}

    public record ReparentCategoryReq(
        UUID newParentId
    ) {}

    public record LinkCategoryReq(
        @NotNull UUID categoryId,
        int displayOrder
    ) {}

    public record ProductDto(UUID id, Long version, String name, String description,
                             UUID defaultSkuId, boolean canSellWithoutOptions,
                             Instant activeStartDate, Instant activeEndDate, boolean archived) {
        static ProductDto of(CatalogProduct p) {
            return new ProductDto(p.getId(), p.getVersion(), p.getName(), p.getDescription(),
                p.getDefaultSkuId(), p.isCanSellWithoutOptions(),
                p.getActiveStartDate(), p.getActiveEndDate(), p.isArchived());
        }
    }

    public record SkuDto(UUID id, UUID productId, boolean isDefault,
                         Long retailPrice, Long salePrice, String currency,
                         Instant activeStartDate, Instant activeEndDate,
                         String externalId, String upc, String optionSignature) {
        static SkuDto of(Sku s) {
            return new SkuDto(s.getId(), s.getProductId(), s.isDefault(),
                s.getRetailPrice(), s.getSalePrice(), s.getCurrency(),
                s.getActiveStartDate(), s.getActiveEndDate(),
                s.getExternalId(), s.getUpc(), s.getOptionSignature());
        }
    }

    public record OptionDto(UUID id, UUID productId, String attributeName,
                            boolean required, boolean useInSkuGeneration) {
        static OptionDto of(ProductOption o) {
            return new OptionDto(o.getId(), o.getProductId(), o.getAttributeName(),
                o.isRequired(), o.isUseInSkuGeneration());
        }
    }

    public record OptionValueDto(UUID id, UUID optionId, String attributeValue, Long priceAdjustment) {
        static OptionValueDto of(ProductOptionValue v) {
            return new OptionValueDto(v.getId(), v.getOptionId(), v.getAttributeValue(), v.getPriceAdjustment());
        }
    }

    public record CategoryDto(UUID id, Long version, String name, boolean archived,
                              Instant activeStartDate, Instant activeEndDate, UUID parentId) {
        static CategoryDto of(Category c) {
            return new CategoryDto(c.getId(), c.getVersion(), c.getName(), c.isArchived(),
                c.getActiveStartDate(), c.getActiveEndDate(), c.getParentId());
        }
    }

    // ── Controller ───────────────────────────────────────────────────────────────────

    private final CatalogProductService productService;
    private final CategoryService categoryService;

    public CatalogController(CatalogProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    /** Create a new product with its mandatory default SKU (INV-3: 422 if defaultSku absent). */
    @PostMapping("/api/catalog/products")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductReq req,
                                                    Authentication auth) {
        if (req.defaultSku() == null) {
            throw CatalogException.defaultSkuRequired();
        }
        DefaultSkuReq dsr = req.defaultSku();
        CatalogProduct product = productService.createProduct(
            req.name(), req.description(), req.canSellWithoutOptions(),
            req.activeStartDate(), req.activeEndDate(),
            dsr.retailPrice(), dsr.salePrice(), dsr.currency(),
            dsr.activeStartDate(), dsr.activeEndDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductDto.of(product));
    }

    @GetMapping("/api/catalog/products/{id}")
    public ProductDto getProduct(@PathVariable UUID id) {
        return ProductDto.of(productService.getProduct(id));
    }

    /** Define a new option dimension for a product (e.g. "Color", "Size"). */
    @PostMapping("/api/catalog/products/{id}/options")
    public ResponseEntity<OptionDto> defineOption(@PathVariable UUID id,
                                                  @Valid @RequestBody DefineOptionReq req,
                                                  Authentication auth) {
        ProductOption option = productService.defineOption(id, req.attributeName(),
            req.required(), req.useInSkuGeneration());
        return ResponseEntity.status(HttpStatus.CREATED).body(OptionDto.of(option));
    }

    /** Add a value to a product option (e.g. "Red" to "Color"). */
    @PostMapping("/api/catalog/products/{id}/options/{optionId}/values")
    public ResponseEntity<OptionValueDto> addOptionValue(@PathVariable UUID id,
                                                         @PathVariable UUID optionId,
                                                         @Valid @RequestBody AddOptionValueReq req,
                                                         Authentication auth) {
        ProductOptionValue value = productService.addOptionValue(id, optionId,
            req.attributeValue(), req.priceAdjustment());
        return ResponseEntity.status(HttpStatus.CREATED).body(OptionValueDto.of(value));
    }

    /** Add a variant SKU to a product (acquires PESSIMISTIC_WRITE, checks INV-1 and INV-5). */
    @PostMapping("/api/catalog/products/{id}/skus")
    public ResponseEntity<SkuDto> addVariantSku(@PathVariable UUID id,
                                                @Valid @RequestBody AddVariantSkuReq req,
                                                Authentication auth) {
        Sku sku = productService.addVariantSku(id,
            req.retailPrice(), req.salePrice(), req.currency(),
            req.activeStartDate(), req.activeEndDate(),
            req.externalId(), req.upc(),
            req.skuGeneratingOptionValueIds() != null ? req.skuGeneratingOptionValueIds() : java.util.List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(SkuDto.of(sku));
    }

    /**
     * INV-1: resolve EXACTLY one SKU for the given option map (attrName→attrValue).
     * Looks up the product's real options/values from DB, computes the signature, exact-matches.
     * Returns the resolved SKU; 404 CATALOG_NO_MATCHING_SKU if no match.
     */
    @PostMapping("/api/catalog/products/{id}/resolve-sku")
    public SkuDto resolveSku(@PathVariable UUID id, @Valid @RequestBody ResolveSkuReq req) {
        return SkuDto.of(productService.resolveSku(id, req.optionMap()));
    }

    /**
     * INV-2: assert a specific SKU is purchasable NOW (uses injected Clock).
     * 409 CATALOG_SKU_NOT_PURCHASABLE if outside active window or product archived.
     */
    @GetMapping("/api/catalog/skus/{skuId}/purchasable")
    public ResponseEntity<Void> checkPurchasable(@PathVariable UUID skuId) {
        productService.assertPurchasable(skuId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/catalog/categories/{id}")
    public CategoryDto getCategory(@PathVariable UUID id) {
        return CategoryDto.of(categoryService.getCategory(id));
    }

    /** Create a category (with optional parent — cycle check if parentId non-null). */
    @PostMapping("/api/catalog/categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CreateCategoryReq req,
                                                      Authentication auth) {
        Category cat = categoryService.createCategory(req.name(), req.activeStartDate(),
            req.activeEndDate(), req.parentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryDto.of(cat));
    }

    /**
     * Reparent a category (INV-6: cycle → 409 CATALOG_CATEGORY_CYCLE).
     * PUT with body {"newParentId": "..."} or {"newParentId": null} for root.
     */
    @PutMapping("/api/catalog/categories/{id}/parent")
    public CategoryDto reparentCategory(@PathVariable UUID id,
                                        @RequestBody ReparentCategoryReq req,
                                        Authentication auth) {
        return CategoryDto.of(categoryService.reparent(id, req.newParentId()));
    }

    /** Link a product to a category (duplicate membership → 409). */
    @PostMapping("/api/catalog/products/{id}/categories")
    public ResponseEntity<Void> linkCategory(@PathVariable UUID id,
                                             @Valid @RequestBody LinkCategoryReq req,
                                             Authentication auth) {
        productService.linkCategory(id, req.categoryId(), req.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<ProblemDetail> handle(CatalogException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
