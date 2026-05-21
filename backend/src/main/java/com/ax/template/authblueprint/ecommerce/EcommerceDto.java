package com.ax.template.authblueprint.ecommerce;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/** DTOs for the e-commerce capstone (recipes/e-commerce/RECIPE.md). */
public final class EcommerceDto {

    private EcommerceDto() {}

    public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Long price,
        @NotBlank String currency,
        @NotNull @Min(0) Integer stock,
        String imageFileId
    ) {}

    public record UpdateProductRequest(
        String name,
        String description,
        @Min(0) Long price,
        @Min(0) Integer stock,
        String imageFileId
    ) {}

    public record ProductResponse(
        String id, String ownerUserId, String name, String description,
        long price, String currency, int stock, String imageFileId,
        String status, Instant createdAt, Instant updatedAt
    ) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(
                p.getId(), p.getOwnerUserId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getCurrency(), p.getStock(), p.getImageFileId(),
                p.getStatus().name(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    public record ProductList(List<ProductResponse> content, long totalElements) {}

    public record AddCartItemRequest(
        @NotBlank String productId,
        @NotNull @Min(1) Integer quantity
    ) {}

    public record CartItemResponse(
        String id, String productId, int quantity,
        long unitPriceAtAddedTime, long lineTotal
    ) {
        public static CartItemResponse from(CartItem ci) {
            return new CartItemResponse(
                ci.getId(), ci.getProductId(), ci.getQuantity(),
                ci.getUnitPriceAtAddedTime(), ci.getLineTotal());
        }
    }

    public record CartResponse(
        String id, String userId, long totalAmount, String currency,
        List<CartItemResponse> items
    ) {
        public static CartResponse from(Cart cart) {
            return new CartResponse(
                cart.getId(), cart.getUserId(), cart.getTotalAmount(), cart.getCurrency(),
                cart.getItems().stream().map(CartItemResponse::from).toList());
        }
    }

    public record CheckoutRequest(@NotBlank String paymentMethodToken) {}

    public record OrderItemResponse(
        String id, String productId, String productNameAtPurchase,
        int quantity, long unitPrice, long lineTotal
    ) {
        public static OrderItemResponse from(OrderItem oi) {
            return new OrderItemResponse(
                oi.getId(), oi.getProductId(), oi.getProductNameAtPurchase(),
                oi.getQuantity(), oi.getUnitPrice(), oi.getLineTotal());
        }
    }

    public record OrderResponse(
        String id, String userId, long totalAmount, String currency,
        String status, String paymentId,
        List<OrderItemResponse> items, Instant createdAt
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                order.getId(), order.getUserId(), order.getTotalAmount(), order.getCurrency(),
                order.getStatus().name(), order.getPaymentId(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt());
        }
    }

    public record OrderList(List<OrderResponse> content, long totalElements) {}
}
