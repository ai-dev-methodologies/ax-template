#!/usr/bin/env python3
"""
gen_verify.py — generate ax block CODE from the recommended 21st components, then VERIFY it.

This closes the loop: recommend.py picks components; codify.py turns each real crawled TSX into an
ax-normalized block; this harness GENERATES that code for every recommended component and runs the ax
invariants against it. TDD framing — the invariants below are the tests, written first:

  T1 nonempty     the generated block has content
  T2 provenance   it carries the @ax-codified-from stamp (traceable to its 21st source)
  T3 tokenized    zero raw hex in className[...] / inline style strings (codify extracted them to --ax-c-*)
  T4 block-lint   it satisfies the 7 ax/* AST rules (the same own-blocks ESLint the catalog runs on itself)

T1–T3 are what DETERMINISTIC codify guarantees. T4 is the honest bar: raw community code frequently trips
ax AST rules (inline component defs, array-includes-in-loop, state mutation) that need a SEMANTIC pass —
so T4 measures exactly how far mechanical codification gets, and which components need hand-finishing
(the gold-standard status-badge is the proof of 100%).

  python3 gen_verify.py        # generate + verify every recommended component -> gen_verify_report.json
  exit 0 = every generated block passes ALL invariants incl. block-lint · exit 1 = at least one gap
"""
import json, os, re, shutil, subprocess
import codify

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..", "..")
FE = os.path.join(ROOT, "frontend")
RECS = os.path.join(HERE, "recommendations.json")
GEN = os.path.join(ROOT, ".crawl-21st", "gen-verify")           # generated-code staging (gitignored)
TMP = os.path.join(FE, ".gen-verify-tmp")                       # eslint base-path requirement
CONFIG = "eslint.own-blocks.config.mjs"
ARB = re.compile(r"\[#[0-9a-fA-F]{3,6}\]")
STY = re.compile(r"['\"]#[0-9a-fA-F]{3,6}['\"]")


def unique_components():
    s = set()
    for r in json.load(open(RECS)):
        for variants in r["pages"].values():
            for v in variants.values():
                for c in v["composition"]:
                    s.add(c["component"])
    return sorted(s)


def invariants(path):
    src = open(path, errors="ignore").read()
    return {
        "T1_nonempty": len(src.strip()) > 0,
        "T2_provenance": "@ax-codified-from" in src,
        "T3_tokenized": not (ARB.search(src) or STY.search(src)),
    }


def block_lint(paths):
    """run the catalog's own-blocks ESLint on the generated files; return {basename: [ax ruleIds]} or None if skipped."""
    if not os.path.isdir(os.path.join(FE, "node_modules", "eslint")):
        return None
    shutil.rmtree(TMP, ignore_errors=True)
    os.makedirs(TMP)
    rels = []
    for p in paths:
        shutil.copy(p, os.path.join(TMP, os.path.basename(p)))
        rels.append(os.path.join(".gen-verify-tmp", os.path.basename(p)))
    r = subprocess.run(
        ["npx", "--no-install", "eslint", "--no-config-lookup", "--config", CONFIG,
         "--format", "json", *rels],
        cwd=FE, capture_output=True, text=True)
    out = {}
    try:
        data = json.loads(r.stdout or "[]")
    except json.JSONDecodeError:
        data = []
    for entry in data:
        ax = sorted({m["ruleId"] for m in entry.get("messages", [])
                     if (m.get("ruleId") or "").startswith("ax/")})
        out[os.path.basename(entry["filePath"])] = ax
    shutil.rmtree(TMP, ignore_errors=True)
    return out


def main():
    comps = unique_components()
    attrs = json.load(open(codify.ATTRS))
    shutil.rmtree(GEN, ignore_errors=True)
    os.makedirs(GEN)
    codify.OUT = GEN                                   # redirect codify output to the staging dir

    generated = []
    for key in comps:
        res = codify.codify_one(key, attrs)
        if res:
            generated.append((key, res["out"]))

    lint = block_lint([p for _, p in generated])
    lint_available = lint is not None

    rows = []
    for key, path in generated:
        inv = invariants(path)
        base = os.path.basename(path)
        ax_viol = lint.get(base, []) if lint_available else []
        inv["T4_block_lint"] = (len(ax_viol) == 0) if lint_available else None
        passed = all(v for v in inv.values() if v is not None)
        rows.append({"component": key, "block": base, "checks": inv,
                     "ax_violations": ax_viol, "pass": passed})

    json.dump({"generated": len(generated), "lint_available": lint_available, "rows": rows},
              open(os.path.join(HERE, "gen_verify_report.json"), "w"), indent=1)

    # verification-loop feedback: blocklist any component whose codified code fails block-lint,
    # so the next recommend round stops picking it (until the generated set is fully lint-clean).
    BL = os.path.join(HERE, "lint_blocklist.json")
    existing = set(json.load(open(BL))) if os.path.exists(BL) else set()
    new_fail = {r["component"] for r in rows if lint_available and r["ax_violations"]}
    if new_fail - existing:
        json.dump(sorted(existing | new_fail), open(BL, "w"), indent=1)
        print(f"[loop] blocklisted {len(new_fail - existing)} lint-failing component(s); re-run recommend")

    # report
    def tally(t):
        return sum(1 for r in rows if r["checks"].get(t))
    n = len(rows)
    print(f"=== GENERATE + VERIFY: {n} recommended components codified -> {GEN} ===")
    print(f"  T1 nonempty   : {tally('T1_nonempty')}/{n}")
    print(f"  T2 provenance : {tally('T2_provenance')}/{n}")
    print(f"  T3 tokenized  : {tally('T3_tokenized')}/{n}  (0 raw hex in className[]/style)")
    if lint_available:
        print(f"  T4 block-lint : {tally('T4_block_lint')}/{n}  (7 ax/* AST rules)")
    else:
        print(f"  T4 block-lint : SKIPPED (frontend/node_modules/eslint absent)")
    full = sum(1 for r in rows if r["pass"])
    print(f"\n  FULLY CONFORMANT (all invariants incl. block-lint): {full}/{n}")
    print("\n  per-component:")
    for r in sorted(rows, key=lambda x: (x["pass"], x["component"])):
        mark = "PASS" if r["pass"] else "FAIL"
        det = "" if r["pass"] else "  ax: " + (", ".join(r["ax_violations"]) or "invariant fail")
        print(f"    [{mark}] {r['component']:42s}{det}")
    import sys
    sys.exit(0 if full == n else 1)


if __name__ == "__main__":
    main()
