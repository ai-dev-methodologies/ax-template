#!/usr/bin/env python3
"""
practices/consumer-proof/engine/lib/coverage_map_guard.py

Implements Part 1.5 of the gap-convergence engine design — the MECE/schema/
disk-truth guard for coverage-map.yaml. Six checks, ALL must pass:

  1. every axis value is a member of its closed enum (D/C/L/R) — free text = FAIL
  2. the cell id set == the exact expected cross-product minus masked cells
     (cardinality: 107 scored cells + masked rows, each masked row carrying a
     non-null na_reason) — a dropped OR duplicated cell = FAIL
  3. every D value appears in docs/IMPLEMENTATION-STATUS.md (drift = FAIL);
     every R value is `status: active` in recipes/_MANIFEST.yaml
  4. every covered_by / nonvacuity path resolves on disk (glob-match >= 1)
  5. status: covered with an empty nonvacuity list = FAIL (honesty floor)
  6. every SCORED cell's weight equals the canonical per-tier/concern value
     (S3=2; S2 in {AUTHZ, MONEY-QUANTITY, IDEMPOTENCY-CONCURRENCY, AUDIT-PII,
     TENANCY-SCOPE}=2, other S2=1; S1=1) — closes the weight-tamper
     over-report vector (a map that sets covered-cell weight high / gap-cell
     weight low passes every other check yet silently inflates C_total).
     not-applicable (masked) cells are excluded — they carry no scoring weight
     convention to pin.

Exit 0 = all six checks pass. Exit 1 = at least one check failed (findings
printed). Exit 2 = usage/toolchain error.

This guard is intentionally standalone in wave 1 (NOT registered into
practices/evals/run-all-guards.sh — the engine stays isolated per the
design's Part 3.7 isolation discipline).
"""
from __future__ import annotations

import glob
import os
import re
import sys

try:
    import yaml
except ImportError:
    print("coverage_map_guard: PyYAML is required (see CLAUDE.md R25 toolchain "
          "prerequisites).", file=sys.stderr)
    sys.exit(2)

# ---- closed enums (Part 1.2) ----
AXIS_D = [
    "activity-feed", "api-key", "approval-workflow", "audit-log", "auth", "billing",
    "comment-thread", "crud", "data-subject-rights", "email-outbox",
    "favorites-bookmarks", "feature-flags", "file-storage", "i18n-policy",
    "multi-tenant", "notification", "payment", "ratelimit", "realtime-policy",
    "scheduled-task", "search", "session-management", "tag-categorization",
    "webhook", "identity-verification",
]
AXIS_C = [
    "AUTHZ", "AUTHN-SESSION", "MONEY-QUANTITY", "IDEMPOTENCY-CONCURRENCY",
    "LIFECYCLE-STATE", "AUDIT-PII", "ERROR-CONTRACT", "QUERY-BOUNDS",
    "INPUT-VALIDATION", "TIME-LOCALE", "OBSERVABILITY-LIMITS", "TENANCY-SCOPE",
]
AXIS_L = ["BE", "FE", "XB"]
AXIS_R = [
    "saas-subscription", "e-commerce", "crm", "booking", "marketplace",
    "b2b-admin", "community", "lms", "cms", "internal-it", "api-gateway-relay",
]

BACKEND_ONLY_DOMAINS = {"i18n-policy", "multi-tenant", "ratelimit", "realtime-policy",
                         "identity-verification"}

# S2 concern -> declared (non-masked) layers, per Part 1.3
S2_LAYERS = {
    "AUTHZ": {"BE", "FE", "XB"},
    "AUTHN-SESSION": {"BE", "FE", "XB"},
    "MONEY-QUANTITY": {"BE", "FE", "XB"},
    "IDEMPOTENCY-CONCURRENCY": {"BE", "FE", "XB"},
    "LIFECYCLE-STATE": {"BE", "XB"},
    "AUDIT-PII": {"BE", "FE", "XB"},
    "ERROR-CONTRACT": {"BE", "FE", "XB"},
    "QUERY-BOUNDS": {"BE", "FE", "XB"},
    "INPUT-VALIDATION": {"BE", "FE", "XB"},
    "TIME-LOCALE": {"BE", "FE"},
    "OBSERVABILITY-LIMITS": {"BE", "FE"},
    "TENANCY-SCOPE": {"BE"},
}

EXPECTED_S1 = 65
EXPECTED_S2 = 31
EXPECTED_S3 = 11
EXPECTED_TOTAL = EXPECTED_S1 + EXPECTED_S2 + EXPECTED_S3  # 107

# Canonical weight schedule (Part 2 of the design). Pinned here so a tampered
# map cannot silently reweight cells to inflate/deflate C_total while still
# passing every other check (checks 1-5 never look at `weight` at all).
S2_HIGH_WEIGHT_CONCERNS = {
    "AUTHZ", "MONEY-QUANTITY", "IDEMPOTENCY-CONCURRENCY", "AUDIT-PII",
    "TENANCY-SCOPE",
}


def expected_cell_ids():
    """Returns (scored_ids: set, masked_ids: set)."""
    scored = set()
    masked = set()
    for d in AXIS_D:
        for l in AXIS_L:
            cid = f"S1.{d}.{l}"
            if d in BACKEND_ONLY_DOMAINS and l != "BE":
                masked.add(cid)
            else:
                scored.add(cid)
    for c in AXIS_C:
        declared = S2_LAYERS[c]
        for l in AXIS_L:
            cid = f"S2.{c}.{l}"
            if l in declared:
                scored.add(cid)
            else:
                masked.add(cid)
    for r in AXIS_R:
        scored.add(f"S3.{r}")
    return scored, masked


def load_map(map_path):
    with open(map_path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _resolve(repo_root, pattern):
    if not pattern:
        return False
    for c in (pattern, pattern.rstrip("/")):
        # recursive=True so '**' patterns actually recurse instead of
        # silently degrading to a single-level '*' match.
        if glob.glob(os.path.join(repo_root, c), recursive=True):
            return True
    return False


def check_axis_enums(cells):
    """Check 1: every axis value is in its closed enum. Returns list of findings."""
    findings = []
    for cell in cells:
        tier = cell.get("tier")
        layer = cell.get("layer")
        if tier not in ("S1", "S2", "S3"):
            findings.append(f"{cell.get('id')}: tier '{tier}' not in {{S1,S2,S3}}")
            continue
        if tier == "S1":
            d = cell.get("domain")
            if d not in AXIS_D:
                findings.append(f"{cell.get('id')}: domain '{d}' not in closed AXIS_D enum (free-text axis value)")
        if tier == "S2":
            c = cell.get("concern")
            if c not in AXIS_C:
                findings.append(f"{cell.get('id')}: concern '{c}' not in closed AXIS_C enum (free-text axis value)")
        if tier == "S3":
            r = cell.get("recipe")
            if r not in AXIS_R:
                findings.append(f"{cell.get('id')}: recipe '{r}' not in closed AXIS_R enum (free-text axis value)")
        if tier in ("S1", "S2") and layer not in AXIS_L:
            findings.append(f"{cell.get('id')}: layer '{layer}' not in closed AXIS_L enum {{BE,FE,XB}} (free-text axis value)")
        status = cell.get("status")
        if status not in ("covered", "partial", "gap", "not-applicable"):
            findings.append(f"{cell.get('id')}: status '{status}' not in {{covered,partial,gap,not-applicable}}")
    return findings


def check_cardinality(cells):
    """Check 2: cell id set == exact expected cross-product minus masked;
    masked rows present with na_reason. Dropped or duplicated = FAIL."""
    findings = []
    expected_scored, expected_masked = expected_cell_ids()

    seen_ids = {}
    for cell in cells:
        cid = cell.get("id")
        seen_ids.setdefault(cid, 0)
        seen_ids[cid] += 1

    dups = [cid for cid, n in seen_ids.items() if n > 1]
    for cid in sorted(dups):
        findings.append(f"DUPLICATE cell id: {cid} appears {seen_ids[cid]} times")

    actual_scored = {c["id"] for c in cells if c.get("status") != "not-applicable"}
    actual_masked = {c["id"] for c in cells if c.get("status") == "not-applicable"}

    missing_scored = expected_scored - actual_scored
    extra_scored = actual_scored - expected_scored
    for cid in sorted(missing_scored):
        findings.append(f"MISSING expected scored cell: {cid}")
    for cid in sorted(extra_scored):
        findings.append(f"UNEXPECTED scored cell not in the frozen cross-product: {cid}")

    missing_masked = expected_masked - actual_masked
    extra_masked_as_scored = expected_masked & actual_scored
    for cid in sorted(missing_masked):
        findings.append(f"MISSING expected masked (not-applicable) cell: {cid}")
    for cid in sorted(extra_masked_as_scored):
        findings.append(f"Cell {cid} is expected MASKED per Part 1.3 but is scored (not marked not-applicable)")

    if len(actual_scored) != EXPECTED_TOTAL and not missing_scored and not extra_scored:
        # only reachable if dup ids inflate/deflate the set size unexpectedly
        findings.append(f"cardinality mismatch: {len(actual_scored)} scored cells, expected {EXPECTED_TOTAL}")

    for cell in cells:
        if cell.get("status") == "not-applicable" and not cell.get("na_reason"):
            findings.append(f"{cell.get('id')}: status=not-applicable but na_reason is empty (REQUIRED)")

    return findings


def check_domain_recipe_drift(cells, repo_root):
    """Check 3: every D value appears in docs/IMPLEMENTATION-STATUS.md;
    every R value is status:active in recipes/_MANIFEST.yaml."""
    findings = []
    status_path = os.path.join(repo_root, "docs", "IMPLEMENTATION-STATUS.md")
    if not os.path.isfile(status_path):
        findings.append(f"docs/IMPLEMENTATION-STATUS.md not found at {status_path} — cannot verify D drift")
    else:
        with open(status_path, "r", encoding="utf-8", errors="replace") as f:
            status_text = f.read()
        for d in AXIS_D:
            if d not in status_text:
                findings.append(f"AXIS_D drift: domain '{d}' not found in docs/IMPLEMENTATION-STATUS.md")

    manifest_path = os.path.join(repo_root, "recipes", "_MANIFEST.yaml")
    if not os.path.isfile(manifest_path):
        findings.append(f"recipes/_MANIFEST.yaml not found at {manifest_path} — cannot verify R drift")
    else:
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = yaml.safe_load(f)
        active = set()
        for entry in manifest.get("recipes", []):
            if entry.get("status") == "active":
                active.add(entry.get("pattern"))
        for r in AXIS_R:
            if r not in active:
                findings.append(f"AXIS_R drift: recipe '{r}' is not status:active in recipes/_MANIFEST.yaml")
    return findings


def check_paths_resolve(cells, repo_root):
    """Check 4: every covered_by / nonvacuity path resolves on disk."""
    findings = []
    for cell in cells:
        for key in ("covered_by", "nonvacuity"):
            for p in cell.get(key) or []:
                if not _resolve(repo_root, p):
                    findings.append(f"{cell.get('id')}: {key} path does not resolve on disk: '{p}'")
    return findings


def check_covered_requires_nonvacuity(cells):
    """Check 5: status=covered with empty nonvacuity = FAIL (honesty floor)."""
    findings = []
    for cell in cells:
        if cell.get("status") == "covered" and not (cell.get("nonvacuity") or []):
            findings.append(f"{cell.get('id')}: status=covered but nonvacuity is EMPTY (honesty floor violation)")
    return findings


def _expected_weight(cell):
    """Canonical per-tier/concern weight (Part 2). Returns None for an
    unrecognized tier (already flagged separately by check_axis_enums)."""
    tier = cell.get("tier")
    if tier == "S1":
        return 1
    if tier == "S3":
        return 2
    if tier == "S2":
        return 2 if cell.get("concern") in S2_HIGH_WEIGHT_CONCERNS else 1
    return None


def check_weight_schedule(cells):
    """Check 6: every SCORED cell's weight must equal the canonical value for
    its tier/concern — S3=2; S2 in S2_HIGH_WEIGHT_CONCERNS=2, other S2=1;
    S1=1. Any deviation = FAIL.

    This closes the weight-tamper over-report vector: none of checks 1-5
    inspect `weight` at all, so a map that sets every covered cell's weight
    to e.g. 100 and every gap cell's weight to 0 previously passed the guard
    outright while computing C_total=1.0.

    not-applicable (masked) cells are intentionally excluded — Part 2 masks
    them out of scoring entirely, so whatever weight convention a masked row
    carries is not pinned by this check (avoids false positives on masked
    rows using a different convention).
    """
    findings = []
    for cell in cells:
        if cell.get("status") == "not-applicable":
            continue
        expected = _expected_weight(cell)
        if expected is None:
            continue
        w = cell.get("weight")
        if w != expected:
            findings.append(
                f"{cell.get('id')}: weight {w!r} does not match the canonical "
                f"tier/concern schedule (expected {expected}) — weight-tamper guard"
            )
    return findings


def run_all_checks(map_path, repo_root):
    doc = load_map(map_path)
    cells = doc.get("cells", [])
    findings = []
    findings += check_axis_enums(cells)
    findings += check_cardinality(cells)
    findings += check_domain_recipe_drift(cells, repo_root)
    findings += check_paths_resolve(cells, repo_root)
    findings += check_covered_requires_nonvacuity(cells)
    findings += check_weight_schedule(cells)
    return findings


def main(argv):
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument("--map", required=True)
    p.add_argument("--repo-root", default=os.getcwd())
    args = p.parse_args(argv)

    repo_root = os.path.abspath(args.repo_root)
    map_path = args.map
    if not os.path.isabs(map_path):
        map_path = os.path.join(os.getcwd(), map_path)

    if not os.path.isfile(map_path):
        print(f"coverage_map_guard: map not found: {map_path}", file=sys.stderr)
        return 2

    findings = run_all_checks(map_path, repo_root)
    if findings:
        print(f"[coverage_map_guard] FAIL — {len(findings)} finding(s) against {map_path}")
        for f in findings:
            print(f"  - {f}")
        return 1

    print(f"[coverage_map_guard] PASS — {map_path}")
    print(f"  cardinality: {EXPECTED_TOTAL} scored cells (S1={EXPECTED_S1} S2={EXPECTED_S2} S3={EXPECTED_S3}) "
          f"+ masked rows, all axes closed-enum, all D/R present, all covered_by/nonvacuity resolve, "
          f"no covered-without-nonvacuity, all weights match the canonical tier/concern schedule.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
