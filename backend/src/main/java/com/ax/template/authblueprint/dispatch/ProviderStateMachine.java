package com.ax.template.authblueprint.dispatch;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link Provider#getStatus()} for the NON-contended edges (AVAIL-FSM-001).
 * The contended AVAILABLE→ASSIGNED claim is deliberately ABSENT here — it must be the atomic
 * status-guarded conditional UPDATE ({@link ProviderRepository#claim}, EXCL-CLAIM-001), not a
 * load-then-set. An illegal edge throws {@link DispatchException#invalidTransition} (→409).
 *
 * <pre>
 *   OFFLINE   -> AVAILABLE          (goOnline)
 *   AVAILABLE -> OFFLINE            (goOffline / sweep-stale)
 *   ASSIGNED  -> AVAILABLE          (release on completion)
 *   AVAILABLE -> ASSIGNED           (NOT here — atomic conditional UPDATE only)
 * </pre>
 */
@Component
public class ProviderStateMachine {

    private static final Map<ProviderStatus, Set<ProviderStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ProviderStatus.class);
        ALLOWED.put(ProviderStatus.OFFLINE, EnumSet.of(ProviderStatus.AVAILABLE));
        ALLOWED.put(ProviderStatus.AVAILABLE, EnumSet.of(ProviderStatus.OFFLINE));
        ALLOWED.put(ProviderStatus.ASSIGNED, EnumSet.of(ProviderStatus.AVAILABLE));
    }

    public void goOnline(Provider p) {
        assertTransition(p.getStatus(), ProviderStatus.AVAILABLE);
        p.setStatus(ProviderStatus.AVAILABLE);
    }

    public void goOffline(Provider p) {
        assertTransition(p.getStatus(), ProviderStatus.OFFLINE);
        p.setStatus(ProviderStatus.OFFLINE);
    }

    /** ASSIGNED -> AVAILABLE on job completion (releases the exclusive hold). */
    public void release(Provider p) {
        assertTransition(p.getStatus(), ProviderStatus.AVAILABLE);
        p.setStatus(ProviderStatus.AVAILABLE);
    }

    private static void assertTransition(ProviderStatus from, ProviderStatus to) {
        Set<ProviderStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ProviderStatus.class));
        if (!allowed.contains(to)) {
            throw DispatchException.invalidTransition(from.name(), to.name());
        }
    }
}
