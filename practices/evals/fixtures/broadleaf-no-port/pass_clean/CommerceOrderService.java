package com.ax.template.authblueprint.commerceorder;

import java.util.UUID;

// FIXTURE (pass): independently-written ax-template code. It ABSORBS the invariant
// (a cart→order spine with a price snapshot) but contains NO Broadleaf bytes — no
// FUL license header, no org.broadleafcommerce package/import. The guard MUST PASS.
public class CommerceOrderService {
    public CommerceOrder findCart(UUID userId) {
        return null;
    }
}
