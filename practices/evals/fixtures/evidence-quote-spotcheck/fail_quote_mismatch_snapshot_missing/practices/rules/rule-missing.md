---
id: FIXTURE-MISSING
evidence:
  - upstream_id: no-such-snapshot
    section: "fixture"
    quote: "any text — the snapshot file does not exist"
---

# Fixture rule — snapshot file is missing

The referenced snapshot (`no-such-snapshot`) has no `.snapshot.md` file →
SNAPSHOT_FILE_MISSING, which --allow-missing-snapshot downgrades to advisory.
