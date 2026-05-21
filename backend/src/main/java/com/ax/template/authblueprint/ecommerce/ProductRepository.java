package com.ax.template.authblueprint.ecommerce;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Product> findAllByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);

    Optional<Product> findByIdAndDeletedAtIsNull(String id);
}
