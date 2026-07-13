---
id: FIXTURE-MISMATCH
evidence:
  - upstream_id: demo-snap
    section: "fixture"
    quote: "this exact sentence is deliberately absent from the snapshot body"
---

# Fixture rule — quote does not appear in the present snapshot

The referenced snapshot (`demo-snap`) exists but does not contain this quote →
QUOTE_NOT_IN_SNAPSHOT, which stays fatal even under --allow-missing-snapshot.
