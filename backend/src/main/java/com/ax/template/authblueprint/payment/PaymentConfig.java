package com.ax.template.authblueprint.payment;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment blueprint configuration: installs an H2 trigger that enforces
 * append-only semantics on payment_events at the DB layer.
 *
 * <p>In production with PostgreSQL, replace this with a CREATE TRIGGER migration
 * (Flyway V003__) — the trigger logic is identical:
 * <pre>
 *   CREATE TRIGGER payment_events_no_update BEFORE UPDATE ON payment_events
 *     FOR EACH ROW EXECUTE FUNCTION raise_immutable();
 * </pre>
 *
 * <p>The H2 path uses Java-based triggers because H2 cannot bind PL/pgSQL.
 */
@Configuration
public class PaymentConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public PaymentConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    @Transactional
    public void installImmutabilityGuard() {
        try {
            // FOR EACH STATEMENT so UPDATE/DELETE attempts that match 0 rows still fire.
            // PAYMENT-RECON-001 verifies via UPDATE WHERE payment_id=<missing> that the
            // trigger blocks the statement before row-matching happens.
            jdbcTemplate.execute(
                "CREATE TRIGGER IF NOT EXISTS payment_events_no_update_stmt "
                    + "BEFORE UPDATE, DELETE ON payment_events "
                    + "FOR EACH STATEMENT CALL \"com.ax.template.authblueprint.payment.PaymentEventImmutabilityTrigger\""
            );
        } catch (Exception e) {
            log.warn("payment_events trigger installation skipped: {}", e.getMessage());
        }
    }
}
