package com.ax.template.authblueprint.ecommerce;

import com.ax.template.authblueprint.auditlog.Audited;
import com.ax.template.authblueprint.auditlog.ResourceId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cart management — one cart per user. Auto-creates on first access.
 * <p>
 * Trace: ECOM-INV-004 — mutations @Audited.
 */
@Service
public class CartService {

    public static final String DEFAULT_CURRENCY = "KRW";

    private final CartRepository carts;
    private final ProductService productService;

    public CartService(CartRepository carts, ProductService productService) {
        this.carts = carts;
        this.productService = productService;
    }

    /** Get-or-create the calling user's cart. */
    @Transactional
    public Cart getOrCreate(String userId) {
        return carts.findByUserId(userId)
            .orElseGet(() -> carts.save(Cart.createFor(userId, DEFAULT_CURRENCY)));
    }

    @Audited(action = "CART_ADD_ITEM", resourceType = "cart")
    @Transactional
    public Cart addItem(@ResourceId String userId, String productId, int quantity) {
        Cart cart = getOrCreate(userId);
        Product product = productService.get(productId);
        if (product.getStock() < quantity) {
            throw new EcommerceException.InsufficientStock(productId, product.getStock(), quantity);
        }
        cart.addItem(product, quantity);
        return carts.save(cart);
    }

    @Audited(action = "CART_REMOVE_ITEM", resourceType = "cart")
    @Transactional
    public Cart removeItem(@ResourceId String userId, String cartItemId) {
        Cart cart = getOrCreate(userId);
        cart.removeItem(cartItemId);
        return carts.save(cart);
    }

    @Audited(action = "CART_CLEAR", resourceType = "cart")
    @Transactional
    public void clear(@ResourceId String userId) {
        carts.findByUserId(userId).ifPresent(c -> {
            c.clear();
            carts.save(c);
        });
    }
}
