package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Root repository for {@link CatalogProduct} and through-root member reads.
 * No member repositories exist — all member writes go through {@link com.ax.template.authblueprint.common.MemberWriter}.
 */
public interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID> {

    /** Acquire PESSIMISTIC_WRITE lock on the product row for variant mutations (race-safe). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM CatalogProduct p WHERE p.id = :id")
    Optional<CatalogProduct> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO: Sku/ProductOption/ProductOptionValue/xrefs have no repo) ──

    @Query("SELECT s FROM Sku s WHERE s.productId = :productId AND s.id = :skuId")
    Optional<Sku> findSku(@Param("productId") UUID productId, @Param("skuId") UUID skuId);

    @Query("SELECT s FROM Sku s WHERE s.productId = :productId")
    List<Sku> findSkus(@Param("productId") UUID productId);

    @Query("SELECT s FROM Sku s WHERE s.productId = :productId AND s.isDefault = true")
    Optional<Sku> findDefaultSku(@Param("productId") UUID productId);

    /** Exact option_signature lookup — returns the ONE SKU matching the signature (INV-1). */
    @Query("SELECT s FROM Sku s WHERE s.productId = :productId AND s.optionSignature = :sig")
    Optional<Sku> findSkuByOptionSignature(@Param("productId") UUID productId, @Param("sig") String sig);

    @Query("SELECT o FROM ProductOption o WHERE o.productId = :productId")
    List<ProductOption> findOptions(@Param("productId") UUID productId);

    @Query("SELECT v FROM ProductOptionValue v WHERE v.optionId = :optionId")
    List<ProductOptionValue> findOptionValues(@Param("optionId") UUID optionId);

    @Query("SELECT v FROM ProductOptionValue v WHERE v.optionId IN :optionIds")
    List<ProductOptionValue> findOptionValuesByOptionIds(@Param("optionIds") List<UUID> optionIds);

    @Query("SELECT x FROM SkuOptionValueXref x WHERE x.skuId = :skuId")
    List<SkuOptionValueXref> findSkuOptionValueXrefs(@Param("skuId") UUID skuId);

    @Query("SELECT x FROM CategoryProductXref x WHERE x.productId = :productId AND x.categoryId = :categoryId")
    Optional<CategoryProductXref> findCategoryProductXref(@Param("productId") UUID productId,
                                                           @Param("categoryId") UUID categoryId);
}
