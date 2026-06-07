package com.ax.template.authblueprint.announcement;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link Announcement#getState()} (ANN-LIFECYCLE-001). An illegal edge throws
 * {@link AnnouncementException#invalidTransition} (-> 409). No other code path may mutate state.
 *
 * <pre>
 *   DRAFT     -> PUBLISHED
 *   PUBLISHED -> ARCHIVED
 *   ARCHIVED  -> (terminal)
 * </pre>
 */
@Component
public class AnnouncementStateMachine {

    private static final Map<AnnouncementState, Set<AnnouncementState>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(AnnouncementState.class);
        ALLOWED.put(AnnouncementState.DRAFT, EnumSet.of(AnnouncementState.PUBLISHED));
        ALLOWED.put(AnnouncementState.PUBLISHED, EnumSet.of(AnnouncementState.ARCHIVED));
        ALLOWED.put(AnnouncementState.ARCHIVED, EnumSet.noneOf(AnnouncementState.class));
    }

    /** DRAFT -> PUBLISHED. */
    public void publish(Announcement a) {
        assertTransition(a.getState(), AnnouncementState.PUBLISHED);
        a.setState(AnnouncementState.PUBLISHED);
    }

    /** PUBLISHED -> ARCHIVED. */
    public void archive(Announcement a) {
        assertTransition(a.getState(), AnnouncementState.ARCHIVED);
        a.setState(AnnouncementState.ARCHIVED);
    }

    private static void assertTransition(AnnouncementState from, AnnouncementState to) {
        Set<AnnouncementState> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(AnnouncementState.class));
        if (!allowed.contains(to)) {
            throw AnnouncementException.invalidTransition(from, to);
        }
    }
}
