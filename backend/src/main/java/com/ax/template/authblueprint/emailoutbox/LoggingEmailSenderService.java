package com.ax.template.authblueprint.emailoutbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Default {@link EmailSenderService} for fork-receivers who have not yet
 * wired their SMTP / SES / SendGrid adapter. Logs the (recipient, subject)
 * pair to stdout and returns success. Fork-receivers MUST replace this
 * with their real provider before production — never ship a logging-only
 * sender to prod or no emails will leave the catalog.
 */
@Configuration
class LoggingEmailSenderConfig {

    private static final Logger LOG = LoggerFactory.getLogger("email-outbox.logging-sender");

    @Bean
    @ConditionalOnMissingBean(EmailSenderService.class)
    EmailSenderService defaultEmailSenderService(
            @Value("${ax.email-outbox.allow-logging-sender-in-prod:false}") boolean allowInProd,
            @Value("${spring.profiles.active:default}") String activeProfile) {
        // R47 dev-stub safety: refuse to ship in 'prod' / 'production' profile
        // unless the fork-receiver explicitly opts in. Matches the
        // useCallerId / useCallerRole production hard-stop pattern.
        if (!allowInProd && (activeProfile != null
                && (activeProfile.contains("prod") || activeProfile.contains("production")))) {
            throw new IllegalStateException(
                "LoggingEmailSenderService is the catalog default — replace it with a real adapter "
              + "(SMTP / SES / SendGrid / Mailgun) before production. To override during incident "
              + "response only, set ax.email-outbox.allow-logging-sender-in-prod=true.");
        }
        return (recipient, subject, body) -> LOG.info(
            // R60 dogfood F11 closure — the catalog's default sender stub also
            // hashes the recipient. Subject can carry verification codes /
            // password reset links, so it is intentionally omitted from the
            // dev log — operators relying on this stub already know which
            // template fired via the EmailOutboxService AUDIT line above.
            "[logging-only] would send recipientHash={}",
            EmailPiiHelper.recipientHash(recipient));
    }
}
