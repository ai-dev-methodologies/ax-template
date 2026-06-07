package com.ax.template.authblueprint.dispatch;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link Offer#getStatus()} (OFFER-FSM-001). PENDING is the only non-terminal
 * state; ACCEPTED/DECLINED/EXPIRED are terminal. Accepting/declining/expiring a non-PENDING offer
 * throws {@link DispatchException#invalidTransition} (→409) — this is the guard a late accept
 * hits when the sweep already EXPIRED the offer.
 */
@Component
public class OfferStateMachine {

    private static final Map<OfferStatus, Set<OfferStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(OfferStatus.class);
        ALLOWED.put(OfferStatus.PENDING,
            EnumSet.of(OfferStatus.ACCEPTED, OfferStatus.DECLINED, OfferStatus.EXPIRED));
        ALLOWED.put(OfferStatus.ACCEPTED, EnumSet.noneOf(OfferStatus.class));
        ALLOWED.put(OfferStatus.DECLINED, EnumSet.noneOf(OfferStatus.class));
        ALLOWED.put(OfferStatus.EXPIRED, EnumSet.noneOf(OfferStatus.class));
    }

    public void accept(Offer o) {
        assertTransition(o.getStatus(), OfferStatus.ACCEPTED);
        o.setStatus(OfferStatus.ACCEPTED);
    }

    public void decline(Offer o) {
        assertTransition(o.getStatus(), OfferStatus.DECLINED);
        o.setStatus(OfferStatus.DECLINED);
    }

    public void expire(Offer o) {
        assertTransition(o.getStatus(), OfferStatus.EXPIRED);
        o.setStatus(OfferStatus.EXPIRED);
    }

    private static void assertTransition(OfferStatus from, OfferStatus to) {
        Set<OfferStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(OfferStatus.class));
        if (!allowed.contains(to)) {
            throw DispatchException.invalidTransition(from.name(), to.name());
        }
    }
}
