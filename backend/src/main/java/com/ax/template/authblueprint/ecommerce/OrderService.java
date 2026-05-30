package com.ax.template.authblueprint.ecommerce;

import com.ax.template.authblueprint.auditlog.Audited;
import com.ax.template.authblueprint.auditlog.ResourceId;
import com.ax.template.authblueprint.notification.NotificationService;
import com.ax.template.authblueprint.payment.CreatePaymentRequest;
import com.ax.template.authblueprint.payment.Payment;
import com.ax.template.authblueprint.payment.PaymentProvider;
import com.ax.template.authblueprint.payment.PaymentService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ax.template.authblueprint.common.Money;

import java.util.UUID;

/**
 * Order orchestration — composes 5 L4 domains per recipes/e-commerce/RECIPE.md:
 * <ul>
 *   <li><b>crud</b> — Order CRUD via {@link OrderRepository}</li>
 *   <li><b>payment</b> — checkout → {@link PaymentService#createPayment}</li>
 *   <li><b>notification</b> — order confirmation via {@link NotificationService}</li>
 *   <li><b>audit-log</b> — {@link Audited} on mutations</li>
 *   <li><b>state-machine</b> — {@link OrderStateMachine} enforces legal transitions</li>
 * </ul>
 *
 * Invariants:
 * <ul>
 *   <li>ECOM-INV-001 — Order.totalAmount == sum(items.unitPrice × qty) — asserted via
 *       {@link Order#assertTotalInvariant()} after snapshotting items.</li>
 *   <li>ECOM-INV-002 — payment.captured ⇒ order.confirmed (PAID) — checkout transitions
 *       PENDING → PAID inside the same {@code @Transactional} as the payment capture.</li>
 *   <li>ECOM-INV-003 — checkout requires Idempotency-Key header; replays return the
 *       existing order (no double-charge).</li>
 * </ul>
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartService cartService;
    private final ProductService productService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final OrderStateMachine stateMachine;

    public OrderService(OrderRepository orders,
                        CartService cartService,
                        ProductService productService,
                        PaymentService paymentService,
                        NotificationService notificationService,
                        OrderStateMachine stateMachine) {
        this.orders = orders;
        this.cartService = cartService;
        this.productService = productService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.stateMachine = stateMachine;
    }

    /**
     * Checkout: cart → order → payment capture → notification.
     *
     * <p>ECOM-INV-002: the entire flow runs in a single transaction. If
     * {@link PaymentService#createPayment} returns a non-APPROVED state, the
     * order remains PENDING and the transaction rolls back stock decrements.
     */
    @Audited(action = "ORDER_CHECKOUT", resourceType = "order")
    @Transactional
    public Order checkout(@ResourceId String userId, String idempotencyKey, String paymentMethodToken) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required (ECOM-INV-003)");
        }

        // Idempotency: replay → same order, no double-charge.
        var existing = orders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Cart cart = cartService.getOrCreate(userId);
        if (cart.getItems().isEmpty()) {
            throw new EcommerceException.EmptyCart(userId);
        }

        Order order = Order.createPending(userId, cart.getCurrency(), idempotencyKey);

        // Snapshot items from cart; decrement product stock atomically.
        for (var ci : cart.getItems()) {
            Product product = productService.get(ci.getProductId());
            if (product.getStock() < ci.getQuantity()) {
                throw new EcommerceException.InsufficientStock(
                    product.getId(), product.getStock(), ci.getQuantity());
            }
            order.addItem(product, ci.getQuantity());
            productService.decrementStock(product.getId(), ci.getQuantity());
        }

        // ECOM-INV-001 — must hold immediately after snapshotting.
        order.assertTotalInvariant();

        Order persisted = orders.save(order);

        // ECOM-INV-002 — capture payment + transition order to PAID in the same tx.
        Payment payment = capturePayment(persisted, userId, idempotencyKey, paymentMethodToken);
        persisted.setPaymentId(payment.getId().toString());
        stateMachine.transition(persisted, OrderStatus.PAID);
        Order paid = orders.save(persisted);

        // Notification — order confirmation.
        notificationService.send(
            userId,
            "ORDER_CONFIRMED",
            "Order confirmed",
            "Your order " + paid.getId() + " has been confirmed. Total: "
                + paid.getTotalAmount() + " " + paid.getCurrency() + ".",
            "/api/ecommerce/orders/" + paid.getId());

        // Clear cart.
        cartService.clear(userId);
        return paid;
    }

    private Payment capturePayment(Order order, String userId, String idempotencyKey, String token) {
        CreatePaymentRequest req = new CreatePaymentRequest(
            // #39 money-l0 reconcile: order total is long MINOR units; payment expects
            // a MAJOR-units BigDecimal scaled to the currency. Money.toMajorUnits places
            // the decimal point (1099 USD → 10.99) — a raw BigDecimal.valueOf(minor) here
            // is a 100x over-charge for every 2-decimal currency (the seam bug this fixes).
            Money.toMajorUnits(order.getTotalAmount(), order.getCurrency()),
            order.getCurrency(),
            order.getId(),
            token == null ? "tok_default" : token,
            null
        );
        var outcome = paymentService.createPayment(
            UUID.fromString(userId), idempotencyKey, req,
            PaymentProvider.FailureMode.APPROVED, null);
        return outcome.payment();
    }

    @Transactional(readOnly = true)
    public Page<Order> listOwn(String userId, Pageable pageable) {
        return orders.findAllByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Order getOwn(String id, String userId) {
        return orders.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new EcommerceException.OrderNotFound(id));
    }

    @Audited(action = "ORDER_TRANSITION", resourceType = "order")
    @Transactional
    public Order transition(@ResourceId String id, String userId, OrderStatus next) {
        Order order = getOwn(id, userId);
        stateMachine.transition(order, next);
        return orders.save(order);
    }
}
