#!/usr/bin/env python3
"""
practices/consumer-proof/engine/lib/coverage_map_guard.py

Implements Part 1.5 of the gap-convergence engine design — the MECE/schema/
disk-truth guard for coverage-map.yaml. Eight checks, ALL must pass:

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
  7. every S3 (COMPOSITION) cell with status=covered must have at least one
     nonvacuity entry that is a live, re-executable test path (matches
     *Test.java / *IT.java / *.vitest.* / *.spec.*) — and NOT a bare
     *-compose.spec.* file-existence artifact and NOT a .md sealed-verdict
     record. Closes the S3 composition-escape (docs/BACKLOG.md P2-29): check 4
     only asserted a nonvacuity path RESOLVES on disk, so a sealed-verdict
     markdown or a bare compose-spec (which only asserts RECIPE.md/frontmatter
     file-existence, never runtime composition) silently qualified a cell for
     `covered`. See README.md "The S3 (COMPOSITION) nonvacuity bar" for the
     full bar (this check enforces criterion (a) only — liveness/path-shape;
     (b)/(c)/(d) are content properties left to human/adversarial review).
     BACKLOG P3-58 strengthening: the filename-shape exclusion above is a
     RENAME BYPASS — a bare compose-spec's exact assertion shape (every
     `expect(...)` traces back to `fs.existsSync`/`fs.readFileSync`, never a
     runtime/HTTP/browser call) still qualifies as "live" if the file is
     merely renamed off the `*-compose.spec.*` convention. Check 7 therefore
     also reads the content of every candidate path that survives the
     filename filter: if the file imports/uses Node's `fs` module AND
     contains none of a short list of runtime-interaction markers (Playwright
     `page.`/`request.`, `fetch(`, `axios.`, `supertest`, RestAssured
     `given()`, MockMvc `.perform(`), it is rejected as FS_EXISTENCE_ONLY
     regardless of filename. This is still a necessary-not-sufficient floor
     (PM-2): it cannot prove a file DOES exercise real composition, only rule
     out the specific "only asserts on fs.* results" shape.
  8. every S1 (CAPABILITY) / S2 (INVARIANT) cell with status=covered must have
     at least one nonvacuity entry that is NOT a `.md` file. BACKLOG P3-60:
     the S3 nonvacuity bar (check 7) explicitly bars `.md` sealed-verdict
     records; S1/S2 never had the equivalent floor. Stated honestly per the
     PRD: 0 of the 70 currently-`covered` S1/S2 cells are `.md`-only today —
     this check gates a vector that has not yet been exploited on the live
     map, not one that was found live. It is prophylactic, not a live-bug
     closure, and the README says so explicitly.

Exit 0 = all eight checks pass. Exit 1 = at least one check failed (findings
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



# Part 1.5 / P2-29 — the S3 (COMPOSITION) nonvacuity bar. A path counts as a
# "live, re-executable test path" only by its file-path SHAPE (this is a
# structural/schema guard, not a content-semantics verifier — see README.md
# for the full four-part bar and why (b)/(c)/(d) stay a review judgment call).
_LIVE_TEST_PATH_RE = re.compile(r"(Test|IT)\.java$|\.vitest\.\w+$|\.spec\.\w+$")
# Explicit denylist: this catalog's established naming convention for a
# Playwright spec that ONLY asserts RECIPE.md/frontmatter file-existence
# (e.g. frontend/tests/recipes/booking-compose.spec.ts) — confirmed by
# inspection (see the notes field on every non-e-commerce S3 cell). Such a
# file matches the *.spec.* shape above yet proves zero runtime composition,
# so it is excluded even though its filename would otherwise qualify.
_BARE_COMPOSE_SPEC_RE = re.compile(r"-compose\.spec\.\w+$")


# BACKLOG P3-58 — the filename-shape exclusion above is a RENAME BYPASS: a bare
# compose-spec's assertion shape (every `expect(...)` traces back to an `fs.existsSync`/
# `fs.readFileSync` result — no runtime/HTTP/browser interaction at all) still matches
# `_LIVE_TEST_PATH_RE` and survives `_BARE_COMPOSE_SPEC_RE` if the file is simply renamed
# off the `-compose.spec.*` convention (e.g. `booking-flow.spec.ts`). This content
# heuristic is a necessary-not-sufficient floor (PM-2, matches check 7's existing
# posture): it can prove a file is FS-EXISTENCE-ONLY, it cannot prove a file that isn't
# genuinely composes multiple domains at runtime — that stays a review judgment call.
_FS_USAGE_RE = re.compile(r"\bfs\.(existsSync|readFileSync)\b")
_RUNTIME_INTERACTION_RE = re.compile(
    r"\bpage\.(goto|click|fill|waitFor)\b"
    r"|\brequest\.(get|post|put|delete|patch|fetch)\b"
    r"|\bfetch\("
    r"|\baxios\."
    r"|\bsupertest\b"
    r"|\bgiven\(\)"
    r"|\bMockMvc\b"
    r"|\.perform\("
)


def _is_fs_existence_only_test(repo_root, p):
    """True iff the file(s) matching path `p` under repo_root use Node's `fs`
    module (existsSync/readFileSync) and contain NONE of the short list of
    runtime-interaction markers above — i.e. every assertion plausibly derives
    from file-existence/content-string checks, not real composition. False if
    the path does not resolve, or resolves to a non-text/unreadable file, or
    simply never uses `fs.*` at all (out of scope for this specific heuristic;
    such a file is judged on its `_LIVE_TEST_PATH_RE` shape alone, as before)."""
    if not p or p.endswith("/"):
        return False
    matches = glob.glob(os.path.join(repo_root, p), recursive=True)
    if not matches:
        return False
    for m in matches:
        try:
            text = open(m, encoding="utf-8", errors="replace").read()
        except OSError:
            continue
        if _FS_USAGE_RE.search(text) and not _RUNTIME_INTERACTION_RE.search(text):
            return True
    return False


def _is_live_s3_nonvacuity_path(repo_root, p):
    """True iff `p` is a live/re-executable test-shaped path per the S3
    nonvacuity bar (README.md) — used only for check 7."""
    if not p or p.endswith("/"):
        return False  # a bare directory path is never itself a test file
    basename = p.rstrip("/").rsplit("/", 1)[-1]
    if basename.endswith(".md"):
        return False  # sealed-verdict / any markdown record — never RED-able
    if _BARE_COMPOSE_SPEC_RE.search(basename):
        return False  # bare file-existence compose-spec — explicitly denylisted
    if not _LIVE_TEST_PATH_RE.search(basename):
        return False
    if _is_fs_existence_only_test(repo_root, p):
        return False  # P3-58 rename-bypass: fs.*-only content, regardless of filename
    return True


def check_s3_covered_nonvacuity_is_live(cells, repo_root):
    """Check 7: every S3 cell with status=covered must have >=1 nonvacuity
    entry that is a live, re-executable, composition-behavioral-shaped test
    path — not solely a sealed-verdict .md, not solely a bare file-existence
    compose-spec, and not solely an fs.existsSync/readFileSync-only file under
    any OTHER filename (P3-58 rename-bypass closure). Closes docs/BACKLOG.md
    P2-29 and P3-58."""
    findings = []
    for cell in cells:
        if cell.get("tier") != "S3" or cell.get("status") != "covered":
            continue
        nonvac = cell.get("nonvacuity") or []
        if not any(_is_live_s3_nonvacuity_path(repo_root, p) for p in nonvac):
            findings.append(
                f"{cell.get('id')}: status=covered but no nonvacuity entry is a live "
                f"re-executable test path (*Test.java / *IT.java / *.vitest.* / *.spec.*, "
                f"excluding bare *-compose.spec.* file-existence artifacts, .md "
                f"sealed-verdict records, and any fs.existsSync/readFileSync-only file "
                f"regardless of filename) — S3 composition nonvacuity bar violation "
                f"(README.md, P2-29/P3-58): {nonvac!r}"
            )
    return findings


# BACKLOG P3-60 — S1/S2 `.md`-only nonvacuity floor. Prophylactic (see module
# docstring check 8): 0 of the 70 currently-covered S1/S2 cells are `.md`-only today.
def check_s1_s2_covered_nonvacuity_not_md_only(cells):
    """Check 8: every S1/S2 cell with status=covered must have >=1 nonvacuity
    entry that is NOT a `.md` file. BACKLOG P3-60."""
    findings = []
    for cell in cells:
        if cell.get("tier") not in ("S1", "S2") or cell.get("status") != "covered":
            continue
        nonvac = cell.get("nonvacuity") or []
        non_md = [p for p in nonvac if p and not p.rstrip("/").endswith(".md")]
        if not non_md:
            findings.append(
                f"{cell.get('id')}: status=covered but every nonvacuity entry is a "
                f".md file (or empty) — S1/S2 .md-only nonvacuity floor violation "
                f"(README.md, P3-60): {nonvac!r}"
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
    findings += check_s3_covered_nonvacuity_is_live(cells, repo_root)
    findings += check_s1_s2_covered_nonvacuity_not_md_only(cells)
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
          f"no covered-without-nonvacuity, all weights match the canonical tier/concern schedule, "
          f"all S3-covered cells cite a live composition-behavioral-shaped (non-rename-bypassed) "
          f"nonvacuity path, all S1/S2-covered cells cite >=1 non-.md nonvacuity entry.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
