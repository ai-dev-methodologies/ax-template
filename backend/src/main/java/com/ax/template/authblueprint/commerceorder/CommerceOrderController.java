package com.ax.template.authblueprint.commerceorder;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Thin HTTP adapter for the commerceorder domain.
 * All mutations delegate to {@link CommerceOrderService}; no business logic here.
 */
@RestController("commerceOrderController")
@RequestMapping("/api/orders")
public class CommerceOrderController {

    private final CommerceOrderService orderService;

    public CommerceOrderController(CommerceOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createCart(@RequestBody CreateCartRequest req,
                                                     Authentication auth) {
        CommerceOrder order = orderService.createCart(auth.getName(), req.currency());
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
            .body(OrderResponse.from(order));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderItemResponse> addItem(@PathVariable UUID id,
                                                      @RequestBody AddItemRequest req,
                                                      Authentication auth) {
        CommerceOrderItem item = orderService.addItem(
            id, auth.getName(),
            req.skuId(), req.nameAtAdd(), req.unitPriceAtAdd(), req.quantity());
        return ResponseEntity.ok(OrderItemResponse.from(item));
    }

    @PatchMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> updateItemQty(@PathVariable UUID id,
                                               @PathVariable UUID itemId,
                                               @RequestBody UpdateQtyRequest req,
                                               Authentication auth) {
        orderService.updateItemQuantity(id, auth.getName(), itemId, req.quantity());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID id,
                                            @PathVariable UUID itemId,
                                            Authentication auth) {
        orderService.removeItem(id, auth.getName(), itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<OrderResponse> submit(@PathVariable UUID id,
                                                 @RequestBody SubmitRequest req,
                                                 Authentication auth) {
        CommerceOrder order = orderService.submit(
            id, auth.getName(), req.total(), req.subTotal(), req.tax());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID id, Authentication auth) {
        CommerceOrder order = orderService.cancel(id, auth.getName());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/fulfillment")
    public ResponseEntity<OrderResponse> assignFulfillment(@PathVariable UUID id,
                                                            @RequestBody FulfillmentRequest req,
                                                            Authentication auth) {
        CommerceOrder order = orderService.assignFulfillment(
            id, auth.getName(), req.toServiceGroups());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id, Authentication auth) {
        CommerceOrder order = orderService.findById(id, auth.getName());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    // ── request / response records ─────────────────────────────────────────────

    public record CreateCartRequest(String currency) {}

    public record AddItemRequest(String skuId, String nameAtAdd, long unitPriceAtAdd, int quantity) {}

    public record UpdateQtyRequest(int quantity) {}

    public record SubmitRequest(long total, long subTotal, long tax) {}

    public record FulfillmentGroupItemDto(UUID orderItemId, int quantity) {}

    public record FulfillmentGroupDto(String address, List<FulfillmentGroupItemDto> items) {}

    public record FulfillmentRequest(List<FulfillmentGroupDto> groups) {
        public List<CommerceOrderService.FulfillmentGroupRequest> toServiceGroups() {
            return groups.stream()
                .map(g -> new CommerceOrderService.FulfillmentGroupRequest(
                    g.address(),
                    g.items().stream()
                        .map(i -> new CommerceOrderService.FulfillmentGroupItemRequest(
                            i.orderItemId(), i.quantity()))
                        .toList()))
                .toList();
        }
    }

    public record OrderItemResponse(UUID id, String skuId, String nameAtAdd,
                                    long unitPriceAtAdd, int quantity, long lineTotal) {
        static OrderItemResponse from(CommerceOrderItem item) {
            return new OrderItemResponse(item.getId(), item.getSkuId(), item.getNameAtAdd(),
                item.getUnitPriceAtAdd(), item.getQuantity(), item.getLineTotal());
        }
    }

    public record OrderResponse(UUID id, String userId, String currency, String status,
                                 long total, long subTotal, long tax,
                                 String submittedAt, String createdAt,
                                 List<OrderItemResponse> items) {
        static OrderResponse from(CommerceOrder order) {
            List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
            return new OrderResponse(
                order.getId(), order.getUserId(), order.getCurrency(),
                order.getStatus().name(),
                order.getTotal(), order.getSubTotal(), order.getTax(),
                order.getSubmittedAt() == null ? null : order.getSubmittedAt().toString(),
                order.getCreatedAt().toString(),
                items);
        }
    }
}
