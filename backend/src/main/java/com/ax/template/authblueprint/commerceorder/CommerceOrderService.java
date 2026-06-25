package com.ax.template.authblueprint.commerceorder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sole orchestrator for the commerceorder aggregate.
 */
@Service
@Transactional
public class CommerceOrderService {

    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderStateMachine stateMachine;
    private final CommerceOrderMetrics metrics;

    public CommerceOrderService(CommerceOrderRepository orderRepository,
                                CommerceOrderStateMachine stateMachine,
                                CommerceOrderMetrics metrics) {
        this.orderRepository = orderRepository;
        this.stateMachine = stateMachine;
        this.metrics = metrics;
    }

    public CommerceOrder createCart(String userId, String currency) {
        if (userId == null || userId.isBlank()) {
            throw new CommerceOrderException("ORDER_INVALID_INPUT", 422, "userId is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new CommerceOrderException("ORDER_INVALID_INPUT", 422, "currency is required");
        }
        CommerceOrder order = new CommerceOrder(UUID.randomUUID(), userId, currency, Instant.now());
        return orderRepository.save(order);
    }

    public CommerceOrderItem addItem(UUID orderId, String userId,
                                     String skuId, String nameAtAdd,
                                     long unitPriceAtAdd, int quantity) {
        if (quantity <= 0) {
            throw new CommerceOrderException("ORDER_INVALID_INPUT", 422,
                "quantity must be positive, got: " + quantity);
        }
        if (unitPriceAtAdd < 0) {
            throw new CommerceOrderException("ORDER_INVALID_INPUT", 422,
                "unitPriceAtAdd must be non-negative");
        }
        CommerceOrder order = loadForMutation(orderId, userId);
        assertEditable(order);
        CommerceOrderItem item = order.addOrMergeItem(skuId, nameAtAdd, unitPriceAtAdd, quantity);
        orderRepository.save(order);
        return item;
    }

    public void updateItemQuantity(UUID orderId, String userId, UUID itemId, int quantity) {
        if (quantity <= 0) {
            throw new CommerceOrderException("ORDER_INVALID_INPUT", 422,
                "quantity must be positive, got: " + quantity);
        }
        CommerceOrder order = loadForMutation(orderId, userId);
        assertEditable(order);
        order.updateItemQuantity(itemId, quantity);
        orderRepository.save(order);
    }

    public void removeItem(UUID orderId, String userId, UUID itemId) {
        CommerceOrder order = loadForMutation(orderId, userId);
        assertEditable(order);
        order.removeItem(itemId);
        orderRepository.save(order);
    }

    public CommerceOrder submit(UUID orderId, String userId, long total, long subTotal, long tax) {
        CommerceOrder order = loadForMutation(orderId, userId);
        stateMachine.submit(order, total, subTotal, tax);
        order = orderRepository.save(order);
        metrics.recordOrderTotal("submitted");
        return order;
    }

    public CommerceOrder cancel(UUID orderId, String userId) {
        CommerceOrder order = loadForMutation(orderId, userId);
        stateMachine.cancel(order);
        return orderRepository.save(order);
    }

    public CommerceOrder assignFulfillment(UUID orderId, String userId,
                                           List<FulfillmentGroupRequest> groups) {
        CommerceOrder order = loadForMutation(orderId, userId);

        // H1: fulfillment is a post-submit operation — reject non-SUBMITTED orders
        if (order.getStatus() != CommerceOrderStatus.SUBMITTED) {
            throw new CommerceOrderException("ORDER_NOT_SUBMITTED", 409,
                "Fulfillment can only be assigned to a SUBMITTED order; current status: "
                + order.getStatus());
        }

        // Build required-quantity map from the order's actual items
        Map<UUID, Integer> requiredQty = new HashMap<>();
        for (CommerceOrderItem item : order.getItems()) {
            requiredQty.put(item.getId(), item.getQuantity());
        }

        // Build assigned-quantity map from the request
        Map<UUID, Integer> assignedQty = new HashMap<>();
        for (FulfillmentGroupRequest groupReq : groups) {
            for (FulfillmentGroupItemRequest itemReq : groupReq.items()) {
                assignedQty.merge(itemReq.orderItemId(), itemReq.quantity(), Integer::sum);
            }
        }

        // H3: reject any orderItemId not belonging to this order
        for (UUID assignedId : assignedQty.keySet()) {
            if (!requiredQty.containsKey(assignedId)) {
                throw new CommerceOrderException("ORDER_FULFILLMENT_NOT_CONSERVED", 422,
                    "Fulfillment references unknown order item id: " + assignedId);
            }
        }

        // Conservation check: every order item must be fully and exactly covered
        for (Map.Entry<UUID, Integer> entry : requiredQty.entrySet()) {
            int assigned = assignedQty.getOrDefault(entry.getKey(), 0);
            if (assigned != entry.getValue()) {
                throw new CommerceOrderException("ORDER_FULFILLMENT_NOT_CONSERVED", 422,
                    "Fulfillment not conserved for item " + entry.getKey()
                    + ": required=" + entry.getValue() + " assigned=" + assigned);
            }
        }

        // H2: replace semantics — clear existing groups (orphanRemoval removes them from DB)
        order.clearFulfillmentGroups();

        // Build unit-price lookup for merchandise total calculation
        Map<UUID, Long> unitPriceByItemId = new HashMap<>();
        for (CommerceOrderItem oi : order.getItems()) {
            unitPriceByItemId.put(oi.getId(), oi.getUnitPriceAtAdd());
        }

        for (FulfillmentGroupRequest groupReq : groups) {
            UUID groupId = UUID.randomUUID();
            CommerceFulfillmentGroup fg =
                new CommerceFulfillmentGroup(groupId, order, groupReq.address());
            long groupMerchandiseTotal = 0L;
            for (FulfillmentGroupItemRequest itemReq : groupReq.items()) {
                // Items owned by root (CommerceOrder), not by the sibling member fg (DDD-006)
                CommerceFulfillmentGroupItem fgi = new CommerceFulfillmentGroupItem(
                    UUID.randomUUID(), groupId, itemReq.orderItemId(), itemReq.quantity());
                order.addFulfillmentGroupItem(fgi);
                // M2: fail-closed on overflow
                long lineAmt = Math.multiplyExact(
                    itemReq.quantity(), unitPriceByItemId.get(itemReq.orderItemId()));
                groupMerchandiseTotal = Math.addExact(groupMerchandiseTotal, lineAmt);
            }
            fg.setMerchandiseTotal(groupMerchandiseTotal);
            order.addFulfillmentGroup(fg);
        }

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public CommerceOrder findById(UUID orderId, String userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new CommerceOrderException("ORDER_NOT_FOUND", 404,
                "Order not found: " + orderId));
    }

    private CommerceOrder loadForMutation(UUID orderId, String userId) {
        return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
            .orElseThrow(() -> new CommerceOrderException("ORDER_NOT_FOUND", 404,
                "Order not found: " + orderId));
    }

    private static void assertEditable(CommerceOrder order) {
        if (!order.getStatus().editable()) {
            throw new CommerceOrderException("ORDER_NOT_EDITABLE", 409,
                "Order is not editable in status: " + order.getStatus());
        }
    }

    public record FulfillmentGroupRequest(String address, List<FulfillmentGroupItemRequest> items) {}
    public record FulfillmentGroupItemRequest(UUID orderItemId, int quantity) {}
}
