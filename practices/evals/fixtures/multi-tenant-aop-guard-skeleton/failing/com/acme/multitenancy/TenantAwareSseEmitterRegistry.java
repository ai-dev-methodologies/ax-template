package com.acme.multitenancy;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * R7 failing-fixture sibling — bakes in three anti-patterns the 40th guard
 * MUST detect:
 *   (anti-1) tenantId resolved from @RequestParam — client_supplied_tenant.
 *   (anti-2) broadcast iterates the whole list with no tenantId filter —
 *            broadcast_no_tenant_filter.
 *   (anti-3) no TenantContext.set / TenantContext.clear around send() —
 *            per_message_no_set (count(set) == 0 != count(clear) == 0
 *            in the registry / send path, AND the @AuthorizedTenant /
 *            @JsonView serialization downstream sees empty context).
 */
@Component
public class TenantAwareSseEmitterRegistry {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // anti-1 — tenantId from query parameter (attacker controls it)
    public SseEmitter register(@RequestParam("tenant_id") UUID tenantId,
                               SseEmitter emitter) {
        emitters.add(emitter);
        return emitter;
    }

    // anti-2 + anti-3 — broadcast with no filter, no set/clear
    public void broadcast(Object payload) {
        for (SseEmitter e : emitters) {
            try {
                e.send(payload);
            } catch (IOException ex) {
                emitters.remove(e);
            }
        }
    }
}
