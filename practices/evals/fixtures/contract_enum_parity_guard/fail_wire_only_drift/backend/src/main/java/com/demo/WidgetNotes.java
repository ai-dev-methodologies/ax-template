package com.demo;

import java.util.Map;

/**
 * wire_source: literal_pattern — the wire literal is produced inline, exactly like the
 * ratelimit probe payload the P0-2 reviewer mutated. A javadoc decoy is included on
 * purpose: comments are stripped before the pattern runs, so `"note-decoy"` below must
 * NOT enter the extracted set.
 *
 * <p>Decoy: Map.of("note", "note-decoy")
 */
public class WidgetNotes {

    public Map<String, String> note() {
        return Map.of("note", "note-healthy");
    }
}
