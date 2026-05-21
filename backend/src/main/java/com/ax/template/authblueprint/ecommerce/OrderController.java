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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Order endpoints — checkout, list, get. */
@RestController
@RequestMapping("/api/ecommerce/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<EcommerceDto.OrderResponse> checkout(
        @Valid @RequestBody EcommerceDto.CheckoutRequest req,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        Order order = orderService.checkout(jwt.getSubject(), idempotencyKey, req.paymentMethodToken());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(EcommerceDto.OrderResponse.from(order));
    }

    @GetMapping
    public EcommerceDto.OrderList list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal Jwt jwt) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Order> p = orderService.listOwn(jwt.getSubject(),
            PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new EcommerceDto.OrderList(
            p.getContent().stream().map(EcommerceDto.OrderResponse::from).toList(),
            p.getTotalElements());
    }

    @GetMapping("/{id}")
    public EcommerceDto.OrderResponse get(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return EcommerceDto.OrderResponse.from(orderService.getOwn(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/ship")
    public EcommerceDto.OrderResponse ship(
        @PathVariable String id,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        return EcommerceDto.OrderResponse.from(
            orderService.transition(id, jwt.getSubject(), OrderStatus.SHIPPED));
    }

    @PostMapping("/{id}/deliver")
    public EcommerceDto.OrderResponse deliver(
        @PathVariable String id,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        return EcommerceDto.OrderResponse.from(
            orderService.transition(id, jwt.getSubject(), OrderStatus.DELIVERED));
    }

    @PostMapping("/{id}/cancel")
    public EcommerceDto.OrderResponse cancel(
        @PathVariable String id,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }
        return EcommerceDto.OrderResponse.from(
            orderService.transition(id, jwt.getSubject(), OrderStatus.CANCELLED));
    }

    // ─── error mapping ─────────────────────────────────────────────────────────

    @ExceptionHandler(EcommerceException.OrderNotFound.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EcommerceException.OrderNotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(EcommerceException.EmptyCart.class)
    public ResponseEntity<ProblemDetail> handleEmptyCart(EcommerceException.EmptyCart ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(EcommerceException.InsufficientStock.class)
    public ResponseEntity<ProblemDetail> handleInsufficient(EcommerceException.InsufficientStock ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadReq(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }
}
