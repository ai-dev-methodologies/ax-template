package com.ax.template.authblueprint.ecommerce;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Product CRUD endpoints. List / get are public; create / update / delete
 * require authentication and the caller must own the product.
 */
@RestController
@RequestMapping("/api/ecommerce/products")
public class ProductController {

    public static final String PRODUCT_NOT_FOUND_TYPE =
        "https://ax-template.dev/problems/product-not-found";
    public static final String NOT_OWNER_TYPE =
        "https://ax-template.dev/problems/not-product-owner";

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public EcommerceDto.ProductList list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Product> p = productService.listActive(
            PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new EcommerceDto.ProductList(
            p.getContent().stream().map(EcommerceDto.ProductResponse::from).toList(),
            p.getTotalElements());
    }

    @GetMapping("/{id}")
    public EcommerceDto.ProductResponse get(@PathVariable String id) {
        return EcommerceDto.ProductResponse.from(productService.get(id));
    }

    @PostMapping
    public ResponseEntity<EcommerceDto.ProductResponse> create(
        @Valid @RequestBody EcommerceDto.CreateProductRequest req,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        // ECOM-INV-003 — mutations require Idempotency-Key (advisory for product create;
        // strict for checkout). For symmetry with payment/billing we accept it but
        // do not de-dup on the controller side for product creation.
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        Product p = productService.create(
            jwt.getSubject(),
            req.name(), req.description(),
            req.price(), req.currency(), req.stock(), req.imageFileId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(EcommerceDto.ProductResponse.from(p));
    }

    @PutMapping("/{id}")
    public EcommerceDto.ProductResponse update(
        @PathVariable String id,
        @Valid @RequestBody EcommerceDto.UpdateProductRequest req,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        Product p = productService.update(
            id, jwt.getSubject(),
            req.name(), req.description(), req.price(), req.stock(), req.imageFileId());
        return EcommerceDto.ProductResponse.from(p);
    }

    @DeleteMapping("/{id}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable String id,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        productService.delete(id, jwt.getSubject());
    }

    // ─── error mapping ─────────────────────────────────────────────────────────

    @ExceptionHandler(EcommerceException.ProductNotFound.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EcommerceException.ProductNotFound ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(PRODUCT_NOT_FOUND_TYPE));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(EcommerceException.NotProductOwner.class)
    public ResponseEntity<ProblemDetail> handleNotOwner(EcommerceException.NotProductOwner ex) {
        // IDOR-safe: non-owner gets 404, not 403.
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "product not found");
        pd.setType(URI.create(PRODUCT_NOT_FOUND_TYPE));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadReq(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.badRequest().body(pd);
    }
}
