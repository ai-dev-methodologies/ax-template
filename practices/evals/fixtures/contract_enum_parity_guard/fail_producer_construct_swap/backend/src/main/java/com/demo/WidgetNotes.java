package com.demo;

import java.util.Map;

/**
 * wire_source: literal_pattern — P0-2 ROUND 3, the CONSTRUCT-CLASS escape.
 *
 * <p>note() is the method the contract block describes, and it no longer builds its
 * payload with the construct the entry's `pattern` / `residue_probe` name: it uses a
 * DIFFERENT map factory and reads the value from a system property. Neither regex sees
 * it. The sibling altNote() still carries the contract's literal, so a FILE-scoped residue
 * check finds nothing to complain about and the extracted set still equals the contract —
 * while the method under test emits whatever the property says. (Real-tree analogue: the
 * reviewer's `Collections.singletonMap("status", System.getProperty("ax.ping.status",
 * "ok"))` in RateLimitPingController.ping(), with anonPing() left intact.)
 *
 * <p>Only BODY-scoped residue catches it: note()'s return survives the pattern deletion.
 *
 * <p>Decoy: Map.of("note", "note-decoy")
 */
public class WidgetNotes {

    public Map<String, String> note() {
        return java.util.Collections.singletonMap("note", System.getProperty("demo.note", "note-a"));
    }

    public Map<String, String> altNote() {
        return Map.of("note", "note-a");
    }
}
