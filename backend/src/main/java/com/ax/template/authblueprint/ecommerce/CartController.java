package com.ax.template.authblueprint.ecommerce;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cart endpoints — single endpoint per user (owner-scoped). */
@RestController
@RequestMapping("/api/ecommerce/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public EcommerceDto.CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return EcommerceDto.CartResponse.from(cartService.getOrCreate(jwt.getSubject()));
    }

    @PostMapping("/items")
    public ResponseEntity<EcommerceDto.CartResponse> addItem(
        @Valid @RequestBody EcommerceDto.AddCartItemRequest req,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        Cart cart = cartService.addItem(jwt.getSubject(), req.productId(), req.quantity());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(EcommerceDto.CartResponse.from(cart));
    }

    @DeleteMapping("/items/{itemId}")
    public EcommerceDto.CartResponse removeItem(
        @PathVariable String itemId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        return EcommerceDto.CartResponse.from(cartService.removeItem(jwt.getSubject(), itemId));
    }

    @DeleteMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        cartService.clear(jwt.getSubject());
    }

    @ExceptionHandler(EcommerceException.ProductNotFound.class)
    public ResponseEntity<ProblemDetail> handleProductNotFound(EcommerceException.ProductNotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(EcommerceException.CartItemNotFound.class)
    public ResponseEntity<ProblemDetail> handleCartItemNotFound(EcommerceException.CartItemNotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(EcommerceException.InsufficientStock.class)
    public ResponseEntity<ProblemDetail> handleInsufficientStock(EcommerceException.InsufficientStock ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadReq(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }
}
