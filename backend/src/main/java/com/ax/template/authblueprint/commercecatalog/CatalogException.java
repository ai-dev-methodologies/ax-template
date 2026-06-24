package com.ax.template.authblueprint.commercecatalog;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for commercecatalog. Carries HTTP status + RFC 9457 type + machine-readable code.
 */
public class CatalogException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private CatalogException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** INV-3: a product create request omitted the default SKU. */
    public static CatalogException defaultSkuRequired() {
        return new CatalogException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:catalog-default-sku-required", "CATALOG_DEFAULT_SKU_REQUIRED",
            "Product creation requires a default SKU; provide defaultSku in the request body");
    }

    /** INV-1: another SKU of the same product already has the same option_signature. */
    public static CatalogException skuOptionAmbiguous() {
        return new CatalogException(HttpStatus.CONFLICT,
            "urn:problem:catalog-sku-option-ambiguous", "CATALOG_SKU_OPTION_AMBIGUOUS",
            "A SKU with the same option combination already exists for this product");
    }

    /** INV-1: resolveSku found no SKU matching the requested option map. */
    public static CatalogException noMatchingSku() {
        return new CatalogException(HttpStatus.NOT_FOUND,
            "urn:problem:catalog-no-matching-sku", "CATALOG_NO_MATCHING_SKU",
            "No SKU matches the provided option combination for this product");
    }

    /** INV-2: assertPurchasable — SKU is outside its active window or product is archived. */
    public static CatalogException skuNotPurchasable() {
        return new CatalogException(HttpStatus.CONFLICT,
            "urn:problem:catalog-sku-not-purchasable", "CATALOG_SKU_NOT_PURCHASABLE",
            "The SKU is not purchasable: it is outside its active window, or the product is archived");
    }

    /** INV-5: an active sellable SKU has no resolvable retail price. */
    public static CatalogException priceRequired() {
        return new CatalogException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:catalog-price-required", "CATALOG_PRICE_REQUIRED",
            "An active sellable SKU must resolve a non-null retail price (own or inherited from the default SKU)");
    }

    /** INV-6: adding the edge would create a cycle in the category tree. */
    public static CatalogException categoryCycle() {
        return new CatalogException(HttpStatus.CONFLICT,
            "urn:problem:catalog-category-cycle", "CATALOG_CATEGORY_CYCLE",
            "The requested parent would create a cycle in the category tree");
    }

    public static CatalogException notFound(String what) {
        return new CatalogException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", what + " not found");
    }

    /** Duplicate product-category membership. */
    public static CatalogException dupMembership() {
        return new CatalogException(HttpStatus.CONFLICT,
            "urn:problem:catalog-dup-membership", "CATALOG_DUP_MEMBERSHIP",
            "Product is already a member of this category");
    }

    /** addVariantSku supplied an option-value id that does not belong to this product's options. */
    public static CatalogException invalidOptionValueId(java.util.UUID id) {
        return new CatalogException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:catalog-invalid-option-value", "CATALOG_INVALID_OPTION_VALUE",
            "Option-value id " + id + " does not belong to this product's defined options");
    }

    /** INV-3: a second default SKU cannot be added. */
    public static CatalogException duplicateDefaultSku() {
        return new CatalogException(HttpStatus.CONFLICT,
            "urn:problem:catalog-duplicate-default-sku", "CATALOG_DUPLICATE_DEFAULT_SKU",
            "A product can have at most one default SKU");
    }
}
