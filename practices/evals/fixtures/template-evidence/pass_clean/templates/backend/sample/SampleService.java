/**
 * @ax-template-meta
 * template_id: backend/sample/SampleService
 * layer: backend-application
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: sample-src-2026-05
 *     section: "Sample Section"
 *     quote: "A registered snapshot id with a non-empty section and quote."
 *   - source_type: external
 *     citation: "Sample external standard — clause 2"
 *     url: "https://example.invalid/standard#clause-2"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *
 * ## Deliberately dedented prose
 * This heading sits at column 0 inside the meta block, so the block is NOT valid YAML as a
 * whole. Shape B in the live tree does the same (see SseEmitterConfig.java). The guard must
 * still verify the evidence key, which is why it isolates that sub-block rather than
 * parsing the whole meta comment.
 */
package com.example.app.sample;

public class SampleService {
}
