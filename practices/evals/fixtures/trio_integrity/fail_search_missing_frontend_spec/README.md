# Fixture: fail_search_missing_frontend_spec

**Rule tested:** trio_integrity_guard — full_trio domain requires both backend AND frontend spec files.

**Expected guard output:** `MISSING_FRONTEND_SPEC: search` → exit 1

## Setup

- `allowlist.yaml`: `search: full_trio`
- `specs/search-l0.yaml`: backend spec present ✓
- `specs/search-frontend-l0.yaml`: **ABSENT** — this is the intentional gap
- `contracts/search-openapi.yaml`: backend contract present ✓
- `contracts/search-ui.yaml`: **ABSENT**
- `blueprints/search-manifest.yaml`: backend blueprint present ✓
- `blueprints/search-ui-manifest.yaml`: **ABSENT**

## Pre-SP26 state

Before SP26 ships the full Spec Trio, `trio_integrity_guard.sh --domain search` exits 1
with `MISSING_FRONTEND_SPEC: search`. After SP26 delivers all 6 trio files the guard
exits 0 and this fixture transitions from FAIL to demonstrating the RED→GREEN journey.
