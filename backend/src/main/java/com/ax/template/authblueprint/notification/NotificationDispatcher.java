package com.ax.template.authblueprint.notification;

import com.ax.template.authblueprint.common.AuditPiiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fans a {@link Notification} out across all configured {@link NotificationChannel}s.
 * <p>
 * Trace: NOTIF-SEND-002 — each channel is invoked in an isolated try/catch so
 * a delivery failure on one channel does not fail the HTTP request nor abort
 * delivery to peers.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final List<NotificationChannel> channels;

    public NotificationDispatcher(List<NotificationChannel> channels) {
        this.channels = channels;
    }

    public void dispatch(Notification notification, NotificationPreferences prefs) {
        for (NotificationChannel channel : channels) {
            if (!channel.enabledFor(prefs)) continue;
            try {
                channel.deliver(notification);
            } catch (Exception ex) {
                // R72 — anchor R61 audit-log-pii-hash-required. recipientUserId
                // can be email-shaped (Spring Authentication.getName() is
                // implementation-defined); hash before logging so the operator
                // log aggregator never holds the raw value.
                log.warn("notification-channel-failure channel={} notification={} recipientHash={}",
                    channel.id(), notification.getId(),
                    AuditPiiHelper.piiHash(notification.getRecipientUserId()), ex);
            }
        }
    }
}
