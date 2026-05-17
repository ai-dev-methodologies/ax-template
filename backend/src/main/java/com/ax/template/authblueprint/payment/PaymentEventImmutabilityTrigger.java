package com.ax.template.authblueprint.payment;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * H2 trigger that blocks UPDATE and DELETE on payment_events.
 * Mirrors the PCI-DSS append-only-audit guarantee at the DB layer
 * for the reference H2 workload. Production Postgres replaces this
 * with a {@code CREATE TRIGGER ... EXECUTE FUNCTION raise_immutable}.
 */
public class PaymentEventImmutabilityTrigger implements Trigger {

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName,
                     boolean before, int type) {
        // no-op
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        // FOR EACH STATEMENT triggers pass null oldRow/newRow. Any invocation here
        // means an UPDATE or DELETE was attempted on the append-only ledger.
        throw new SQLException(
            "payment_events is append-only; UPDATE/DELETE is blocked (PAYMENT-RECON-001)"
        );
    }

    @Override
    public void close() { /* no-op */ }

    @Override
    public void remove() { /* no-op */ }
}
