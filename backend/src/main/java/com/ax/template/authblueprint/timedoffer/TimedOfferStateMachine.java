package com.ax.template.authblueprint.timedoffer;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link TimedOffer#getStatus()} (TIMEDOFFER-LIFECYCLE-001). OPEN is the only
 * non-terminal state; ACCEPTED/DECLINED/EXPIRED are terminal. Accepting/declining/expiring a
 * non-OPEN offer throws {@link TimedOfferException#notOpen} (→409) — this is the guard a late accept
 * hits when the sweep already EXPIRED the offer, or when the candidate already declined. Every
 * transition records WHO decided and WHEN in the same write (no bare status change).
 */
@Component
public class TimedOfferStateMachine {

    private static final Map<OfferStatus, Set<OfferStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(OfferStatus.class);
        ALLOWED.put(OfferStatus.OPEN,
            EnumSet.of(OfferStatus.ACCEPTED, OfferStatus.DECLINED, OfferStatus.EXPIRED));
        ALLOWED.put(OfferStatus.ACCEPTED, EnumSet.noneOf(OfferStatus.class));
        ALLOWED.put(OfferStatus.DECLINED, EnumSet.noneOf(OfferStatus.class));
        ALLOWED.put(OfferStatus.EXPIRED, EnumSet.noneOf(OfferStatus.class));
    }

    public void accept(TimedOffer o, String by, Instant at) {
        assertTransition(o.getStatus(), OfferStatus.ACCEPTED);
        o.decide(OfferStatus.ACCEPTED, by, at);
    }

    public void decline(TimedOffer o, String by, Instant at) {
        assertTransition(o.getStatus(), OfferStatus.DECLINED);
        o.decide(OfferStatus.DECLINED, by, at);
    }

    public void expire(TimedOffer o, String by, Instant at) {
        assertTransition(o.getStatus(), OfferStatus.EXPIRED);
        o.decide(OfferStatus.EXPIRED, by, at);
    }

    private static void assertTransition(OfferStatus from, OfferStatus to) {
        Set<OfferStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(OfferStatus.class));
        if (!allowed.contains(to)) {
            throw TimedOfferException.notOpen(from.name());
        }
    }
}
