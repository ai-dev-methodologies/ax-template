package com.demo;

import java.util.Map;

/**
 * P0-2 round 2 — the reviewer's SECOND escape, in miniature.
 *
 * <p>{@code note()} is the endpoint the contract's `note` block describes, and it emits
 * {@code "note-healthy"} — a value the contract does NOT declare. But the expression is
 * {@code "note-healthy".toString()}, not a plain literal, so the manifest's
 * `pattern: Map\.of\("note", "([^"]+)"\)` does not match it. The regex instead captures
 * {@code "note-a"} from {@code altNote()} — an UNRELATED method — and the extracted set
 * comes out exactly equal to the contract. Set equality held on literals nothing puts on
 * the wire, and the guard reported PASS.
 *
 * <p>`residue_probe: Map\.of\(\s*"note"\s*,` closes it: after every `pattern` capture is
 * deleted, the probe still matches the producing construct in {@code note()}, so the guard
 * ERRORS with "cannot prove the literal set" instead of guessing.
 *
 * <p>Decoy: Map.of("note", "note-decoy")
 */
public class WidgetNotes {

    public Map<String, String> note() {
        return Map.of("note", "note-healthy".toString());
    }

    public Map<String, String> altNote() {
        return Map.of("note", "note-a");
    }
}
