---
id: FIXTURE-MISSING
evidence:
  - upstream_id: no-such-snapshot
    section: "fixture"
    quote: "any text — the snapshot file does not exist"
---

# Fixture rule — snapshot file is missing

The referenced snapshot (`no-such-snapshot`) has no `.snapshot.md` file →
SNAPSHOT_FILE_MISSING, downgraded to advisory by --allow-missing-snapshot.
With no QUOTE mismatch present, the guard exits 0.
