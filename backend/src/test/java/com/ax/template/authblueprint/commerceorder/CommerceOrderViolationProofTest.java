package com.ax.template.authblueprint.commerceorder;

import com.ax.template.authblueprint.common.AggregateMember;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Structural + behavioral violation-proof tests for commerceorder.
 * No Spring context required — reflection + domain object calls only.
 *
 * <p>Covers:
 * <ol>
 *   <li>ORDER-SNAPSHOT-001: CommerceOrderItem snapshot columns have {@code @Column(updatable=false)}</li>
 *   <li>CommerceOrder.setStatus not public (FSM sole mutator)</li>
 *   <li>{@code @Version} on CommerceOrder</li>
 *   <li>Member types carry {@code @AggregateMember(root=CommerceOrder.class)}</li>
 *   <li>No member repositories</li>
 *   <li>CommerceOrderStateMachine ALLOWED map has no SUBMITTED → IN_PROCESS edge</li>
 *   <li>CANCELLED is terminal (empty outgoing edges)</li>
 *   <li>ORDER-MERGE-001 behavioral: add same sku twice via domain object → one line, qty summed</li>
 *   <li>ORDER-IMMUTABLE-001 behavioral: SUBMITTED order.editable() == false</li>
 * </ol>
 */
@Tag("COMMERCEORDER")
class CommerceOrderViolationProofTest {

    // ── 1. ORDER-SNAPSHOT-001: snapshot columns are @Column(updatable=false) ──

    @Test @Tag("ORDER-SNAPSHOT-001")
    void violation_snapshotColumnsAreImmutable() throws Exception {
        assertColumnNotUpdatable(CommerceOrderItem.class, "skuId");
        assertColumnNotUpdatable(CommerceOrderItem.class, "nameAtAdd");
        assertColumnNotUpdatable(CommerceOrderItem.class, "unitPriceAtAdd");
    }

    // ── 2. CommerceOrder.setStatus is NOT public ─────────────────────────────

    @Test @Tag("ORDER-LIFECYCLE-001")
    void violation_setStatusIsNotPublic() throws Exception {
        Method setStatus = null;
        for (Method m : CommerceOrder.class.getDeclaredMethods()) {
            if (m.getName().equals("setStatus")
                    && m.getParameterTypes().length == 1
                    && m.getParameterTypes()[0] == CommerceOrderStatus.class) {
                setStatus = m;
                break;
            }
        }
        assertThat(setStatus).as("CommerceOrder must declare setStatus(CommerceOrderStatus)").isNotNull();
        assertThat(Modifier.isPublic(setStatus.getModifiers()))
            .as("CommerceOrder.setStatus must NOT be public — FSM is the sole mutator").isFalse();
    }

    // ── 3. @Version on CommerceOrder ─────────────────────────────────────────

    @Test @Tag("ORDER-LIFECYCLE-001")
    void violation_orderHasVersionAnnotation() throws Exception {
        Field f = getDeclaredFieldInHierarchy(CommerceOrder.class, "version");
        assertThat(f).as("CommerceOrder.version field must exist").isNotNull();
        assertThat(f.isAnnotationPresent(Version.class))
            .as("CommerceOrder.version must carry @Version").isTrue();
    }

    // ── 4. Member types carry @AggregateMember(root=CommerceOrder.class) ─────

    @Test @Tag("ORDER-SNAPSHOT-001")
    void violation_aggregateMembersCarryCorrectRootAnnotation() {
        for (Class<?> cls : new Class<?>[]{
                CommerceOrderItem.class,
                CommerceFulfillmentGroup.class,
                CommerceFulfillmentGroupItem.class}) {
            AggregateMember ann = cls.getAnnotation(AggregateMember.class);
            assertThat(ann)
                .as(cls.getSimpleName() + " must carry @AggregateMember").isNotNull();
            assertThat(ann.root())
                .as(cls.getSimpleName() + " @AggregateMember.root must be CommerceOrder.class")
                .isEqualTo(CommerceOrder.class);
        }
    }

    // ── 5. No member repositories ─────────────────────────────────────────────

    @Test @Tag("ORDER-SNAPSHOT-001")
    void violation_noMemberRepositories() throws Exception {
        Class<?> orderRepoClass = CommerceOrderRepository.class;
        assertThat(orderRepoClass).as("CommerceOrderRepository must exist").isNotNull();

        assertClassDoesNotExist("com.ax.template.authblueprint.commerceorder.CommerceOrderItemRepository");
        assertClassDoesNotExist("com.ax.template.authblueprint.commerceorder.CommerceFulfillmentGroupRepository");
        assertClassDoesNotExist("com.ax.template.authblueprint.commerceorder.CommerceFulfillmentGroupItemRepository");
        // Also check old-style names
        assertClassDoesNotExist("com.ax.template.authblueprint.commerceorder.OrderItemRepository");
        assertClassDoesNotExist("com.ax.template.authblueprint.commerceorder.FulfillmentGroupRepository");
    }

    // ── 6. ALLOWED map has no SUBMITTED → IN_PROCESS edge ────────────────────

    @Test @Tag("ORDER-LIFECYCLE-001")
    void violation_allowedMapHasNoSubmittedToInProcessEdge() {
        java.util.Map<CommerceOrderStatus, Set<CommerceOrderStatus>> allowed =
            CommerceOrderStateMachine.getAllowedMap();
        Set<CommerceOrderStatus> fromSubmitted =
            allowed.getOrDefault(CommerceOrderStatus.SUBMITTED, Set.of());
        assertThat(fromSubmitted)
            .as("SUBMITTED → IN_PROCESS must NOT be in ALLOWED (no re-open edge)")
            .doesNotContain(CommerceOrderStatus.IN_PROCESS);
    }

    @Test @Tag("ORDER-LIFECYCLE-001")
    void violation_cancelledIsTerminalNoOutgoingEdges() {
        java.util.Map<CommerceOrderStatus, Set<CommerceOrderStatus>> allowed =
            CommerceOrderStateMachine.getAllowedMap();
        Set<CommerceOrderStatus> fromCancelled =
            allowed.getOrDefault(CommerceOrderStatus.CANCELLED, Set.of());
        assertThat(fromCancelled)
            .as("CANCELLED must be terminal — no outgoing edges")
            .isEmpty();
    }

    // ── 7. ORDER-MERGE-001 behavioral: domain object merge ───────────────────

    @Test @Tag("ORDER-MERGE-001")
    void violation_merge_sameSku_addsToExistingLine_notDuplicate() {
        CommerceOrder order = new CommerceOrder(
            UUID.randomUUID(), "user-1", "KRW", java.time.Instant.now());

        String skuId = "sku-widget";
        // First add: qty=2
        order.addOrMergeItem(skuId, "Widget", 1000L, 2);
        // Second add of SAME sku: qty=3 → must merge
        order.addOrMergeItem(skuId, "Widget", 1000L, 3);

        assertThat(order.getItems())
            .as("same sku added twice must produce exactly one CommerceOrderItem")
            .hasSize(1);
        assertThat(order.getItems().get(0).getQuantity())
            .as("merged quantity must be 2 + 3 = 5")
            .isEqualTo(5);
        assertThat(order.getItems().get(0).getLineTotal())
            .as("line total must be 5 × 1000 = 5000")
            .isEqualTo(5000L);
    }

    // ── 8. ORDER-IMMUTABLE-001 behavioral: SUBMITTED order not editable ───────

    @Test @Tag("ORDER-IMMUTABLE-001")
    void violation_submittedOrderIsNotEditable_throwsCommerceOrderException() {
        CommerceOrder order = new CommerceOrder(
            UUID.randomUUID(), "user-2", "USD", java.time.Instant.now());
        CommerceOrderStateMachine fsm = new CommerceOrderStateMachine();
        order.addOrMergeItem("sku-x", "Item X", 100L, 1);
        fsm.submit(order, 100L, 100L, 0L);

        // SUBMITTED → editable() must return false
        assertThat(order.getStatus().editable())
            .as("SUBMITTED order must NOT be editable").isFalse();

        // Service-level guard logic: throws ORDER_NOT_EDITABLE
        assertThatThrownBy(() -> {
            if (!order.getStatus().editable()) {
                throw new CommerceOrderException("ORDER_NOT_EDITABLE", 409,
                    "Order is not editable in status: " + order.getStatus());
            }
        })
        .isInstanceOf(CommerceOrderException.class)
        .hasMessageContaining("SUBMITTED");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void assertColumnNotUpdatable(Class<?> cls, String fieldName) throws Exception {
        Field f = getDeclaredFieldInHierarchy(cls, fieldName);
        assertThat(f).as(cls.getSimpleName() + "." + fieldName + " must exist").isNotNull();
        Column col = f.getAnnotation(Column.class);
        assertThat(col).as(cls.getSimpleName() + "." + fieldName + " must carry @Column").isNotNull();
        assertThat(col.updatable())
            .as(cls.getSimpleName() + "." + fieldName + " must be immutable (updatable=false)")
            .isFalse();
    }

    private Field getDeclaredFieldInHierarchy(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        return null;
    }

    private void assertClassDoesNotExist(String fqn) {
        try {
            Class.forName(fqn);
            assertThat(fqn)
                .as("Member repository " + fqn + " must NOT exist (route writes through the root aggregate)")
                .isNull(); // always fails
        } catch (ClassNotFoundException expected) {
            // Good — class does not exist
        }
    }
}
