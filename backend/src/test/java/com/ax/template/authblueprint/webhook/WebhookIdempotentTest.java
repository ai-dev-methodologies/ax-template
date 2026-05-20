package com.ax.template.authblueprint.webhook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IDEMPOTENCY family — WEBHOOK-IDEMPOTENT-001.
 * <p>
 * This is a sender-side contract documentation item: the test is README
 * presence + content (templates/L4/webhook/README.md must describe the
 * {@code X-Webhook-Delivery-Id} semantic to fork-receivers writing the
 * corresponding inbound handler).
 */
class WebhookIdempotentTest {

    private static final Path README = Path.of("..", "templates", "L4", "webhook", "README.md");

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-IDEMPOTENT-001")
    @DisplayName("WEBHOOK-IDEMPOTENT-001 — receiver-contract documentation present in templates/L4/webhook/README.md")
    void idempotent001_readmeDocumentsReceiverContract() throws Exception {
        assertThat(Files.exists(README))
            .as("WEBHOOK-IDEMPOTENT-001 — README required at templates/L4/webhook/README.md")
            .isTrue();

        String content = Files.readString(README, StandardCharsets.UTF_8);

        assertThat(content)
            .as("README MUST reference the stable delivery_id header by name")
            .contains("X-Webhook-Delivery-Id");
        assertThat(content)
            .as("README MUST anchor WEBHOOK-IDEMPOTENT-001 explicitly so fork-receivers can find it")
            .contains("WEBHOOK-IDEMPOTENT-001");
        assertThat(content)
            .as("README MUST point at WEBHOOK-RETRY-002 — the stable-id guarantee that underwrites this contract")
            .contains("WEBHOOK-RETRY-002");
    }
}
