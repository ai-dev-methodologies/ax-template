package com.ax.template.authblueprint.tokenizedsecurities;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default OnChainAnchor — thread-safe in-memory store (test-double / Phase 0).
 * A fork replaces this @Component with a real chain client adapter.
 */
@Component
public class InMemoryAnchor implements OnChainAnchor {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<AnchorRecord>> store =
            new ConcurrentHashMap<>();

    @Override
    public String anchor(AnchorIntent intent) {
        String ref = "anchor:" + intent.transferId();
        store.computeIfAbsent(intent.tokenCode(), k -> new CopyOnWriteArrayList<>())
             .add(new AnchorRecord(intent.transferId(), intent.fromHolderId(),
                     intent.toHolderId(), intent.units(), ref));
        return ref;
    }

    @Override
    public List<AnchorRecord> recordsFor(String tokenCode) {
        CopyOnWriteArrayList<AnchorRecord> list = store.get(tokenCode);
        return list == null ? List.of() : List.copyOf(list);
    }
}
