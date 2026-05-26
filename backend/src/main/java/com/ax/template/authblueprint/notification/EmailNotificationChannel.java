package com.ax.template.authblueprint.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Email channel — reference adapter. In production this delegates to an
 * SMTP gateway, SES, or similar. Here it logs at INFO so tests can observe
 * invocations via a captured logger or by replacing the bean with a Mockito
 * mock (see NOTIF-SEND-002).
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    @Override
    public String id() { return "email"; }

    @Override
    public void deliver(Notification notification) {
        // R65 — anchor R61 audit-log-pii-hash-required. recipientUserId can
        // be email-shaped (Spring Authentication.getName() is implementation
        // defined); hash before logging so the operator log aggregator never
        // holds the raw value. title can carry user-supplied content
        // (notification body excerpts) — omit it entirely from the dev log.
        log.info("email-notification recipientHash={} type={}",
            com.ax.template.authblueprint.common.AuditPiiHelper.piiHash(
                notification.getRecipientUserId()),
            notification.getType());
    }

    @Override
    public boolean enabledFor(NotificationPreferences prefs) {
        return prefs != null && prefs.isEmailEnabled();
    }
}
