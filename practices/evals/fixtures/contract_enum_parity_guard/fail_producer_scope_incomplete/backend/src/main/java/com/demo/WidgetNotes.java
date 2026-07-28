package com.demo;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * wire_source: literal_pattern — P0-2 ROUND 3, the DECLARATION-COMPLETENESS escape.
 *
 * <p>`producer_scope.methods:` is itself an author assertion. Here the entry declares only
 * note() — which dutifully carries the contract's literal — while a SECOND request-mapped
 * handler, altNote(), serves the same block and emits "note-b" through a construct neither
 * `pattern:` nor `residue_probe:` names. Body-scoped residue finds nothing (it is only
 * looking inside note()), the extracted set still equals the contract, and the second
 * endpoint is on the wire emitting a token the contract does not admit.
 *
 * <p>The handler set is a fact on disk: every request-mapped method must be declared.
 *
 * <p>Decoy: Map.of("note", "note-decoy")
 */
public class WidgetNotes {

    @GetMapping("/note")
    public Map<String, String> note() {
        return Map.of("note", "note-a");
    }

    @GetMapping("/alt-note")
    public Map<String, String> altNote() {
        return java.util.Collections.singletonMap("note", "note-b");
    }
}
