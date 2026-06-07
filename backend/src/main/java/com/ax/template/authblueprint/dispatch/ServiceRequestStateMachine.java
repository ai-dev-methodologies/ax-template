package com.ax.template.authblueprint.dispatch;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link ServiceRequest#getStatus()} for the NON-contended edges. The contended
 * OFFERED→ASSIGNED claim is deliberately ABSENT — it must be the atomic conditional UPDATE
 * ({@link ServiceRequestRepository#claim}, EXCL-CLAIM-001). Illegal edge → 409.
 *
 * <pre>
 *   PENDING   -> OFFERED            (offer)
 *   OFFERED   -> UNFULFILLED        (cascade exhausted, OFFER-CASCADE-004)
 *   ASSIGNED  -> FULFILLED          (complete)
 *   PENDING   -> CANCELLED          (cancel)
 *   OFFERED   -> CANCELLED          (cancel)
 *   OFFERED   -> ASSIGNED           (NOT here — atomic conditional UPDATE only)
 * </pre>
 */
@Component
public class ServiceRequestStateMachine {

    private static final Map<ServiceRequestStatus, Set<ServiceRequestStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ServiceRequestStatus.class);
        ALLOWED.put(ServiceRequestStatus.PENDING,
            EnumSet.of(ServiceRequestStatus.OFFERED, ServiceRequestStatus.CANCELLED));
        ALLOWED.put(ServiceRequestStatus.OFFERED,
            EnumSet.of(ServiceRequestStatus.UNFULFILLED, ServiceRequestStatus.CANCELLED));
        ALLOWED.put(ServiceRequestStatus.ASSIGNED,
            EnumSet.of(ServiceRequestStatus.FULFILLED));
        ALLOWED.put(ServiceRequestStatus.FULFILLED, EnumSet.noneOf(ServiceRequestStatus.class));
        ALLOWED.put(ServiceRequestStatus.UNFULFILLED, EnumSet.noneOf(ServiceRequestStatus.class));
        ALLOWED.put(ServiceRequestStatus.CANCELLED, EnumSet.noneOf(ServiceRequestStatus.class));
    }

    public void offer(ServiceRequest r) {
        assertTransition(r.getStatus(), ServiceRequestStatus.OFFERED);
        r.setStatus(ServiceRequestStatus.OFFERED);
    }

    public void markUnfulfilled(ServiceRequest r) {
        assertTransition(r.getStatus(), ServiceRequestStatus.UNFULFILLED);
        r.setStatus(ServiceRequestStatus.UNFULFILLED);
    }

    public void fulfill(ServiceRequest r) {
        assertTransition(r.getStatus(), ServiceRequestStatus.FULFILLED);
        r.setStatus(ServiceRequestStatus.FULFILLED);
    }

    public void cancel(ServiceRequest r) {
        assertTransition(r.getStatus(), ServiceRequestStatus.CANCELLED);
        r.setStatus(ServiceRequestStatus.CANCELLED);
    }

    private static void assertTransition(ServiceRequestStatus from, ServiceRequestStatus to) {
        Set<ServiceRequestStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ServiceRequestStatus.class));
        if (!allowed.contains(to)) {
            throw DispatchException.invalidTransition(from.name(), to.name());
        }
    }
}
