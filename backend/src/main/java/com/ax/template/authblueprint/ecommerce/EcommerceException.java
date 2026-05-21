package com.ax.template.authblueprint.ecommerce;

/**
 * E-commerce domain runtime errors. Mapped to RFC 7807 ProblemDetail at the
 * controller layer.
 */
public class EcommerceException extends RuntimeException {

    public EcommerceException(String msg) { super(msg); }

    public static class ProductNotFound extends EcommerceException {
        public ProductNotFound(String id) { super("product not found: " + id); }
    }

    public static class OrderNotFound extends EcommerceException {
        public OrderNotFound(String id) { super("order not found: " + id); }
    }

    public static class CartItemNotFound extends EcommerceException {
        public CartItemNotFound(String id) { super("cart item not found: " + id); }
    }

    public static class InsufficientStock extends EcommerceException {
        public InsufficientStock(String productId, int available, int requested) {
            super("insufficient stock for product " + productId
                + ": available=" + available + " requested=" + requested);
        }
    }

    public static class EmptyCart extends EcommerceException {
        public EmptyCart(String userId) { super("cart is empty for user " + userId); }
    }

    public static class NotProductOwner extends EcommerceException {
        public NotProductOwner(String productId) { super("not authorized to modify product " + productId); }
    }
}
