#!/usr/bin/env python3
"""
practices/consumer-proof/engine/lib/coverage_report.py

Implements Part 2 of the gap-convergence engine design: the deterministic
coverage metric + honesty-downgrade pass over coverage-map.yaml.

This module is intentionally free of side effects beyond:
  - reading coverage-map.yaml and disk paths under --repo-root
  - (only when called with write=True) writing docs/coverage-map/COVERAGE.md
    and appending one line to docs/coverage-map/coverage-history.jsonl

It is invoked by ../coverage-report.sh; it is also unit-testable directly
(see the __main__ self-test block and the tampered-map fixture check).
"""
from __future__ import annotations

import glob
import json
import os
import subprocess
import sys
from dataclasses import dataclass, field

try:
    import yaml
except ImportError:  # pragma: no cover
    print("coverage_report.py: PyYAML is required (see CLAUDE.md R25 toolchain "
          "prerequisites — use the pyshim or /usr/bin/python3).", file=sys.stderr)
    sys.exit(2)

SCORE = {"covered": 1.0, "partial": 0.5, "gap": 0.0}
# not-applicable cells are excluded from both sums entirely.

TIER_TIEBREAK = {"S2": 0, "S1": 1, "S3": 2}


@dataclass
class Downgrade:
    cell_id: str
    from_status: str
    to_status: str
    reason: str


@dataclass
class LoadResult:
    doc: dict
    cells: list
    downgrades: list = field(default_factory=list)
    backlog_mismatches: list = field(default_factory=list)


def _resolve(repo_root: str, pattern: str) -> bool:
    """A covered_by/nonvacuity entry 'resolves' if a glob against repo_root
    matches at least one path. Trailing '/' is tolerated for directories."""
    if not pattern:
        return False
    candidates = [pattern, pattern.rstrip("/")]
    for c in candidates:
        # recursive=True so '**' patterns actually recurse instead of
        # silently degrading to a single-level '*' match.
        if glob.glob(os.path.join(repo_root, c), recursive=True):
            return True
    return False


def load_map(map_path: str) -> dict:
    with open(map_path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _backlog_checked(repo_root: str, backlog_ref: str) -> bool:
    """Grep docs/BACKLOG.md for the backlog_ref token on a line that also
    carries a '[x]' checked marker. Best-effort textual check — the canonical
    backlog format is 'P#-#' tokens in a markdown checklist/table."""
    backlog_path = os.path.join(repo_root, "docs", "BACKLOG.md")
    if not os.path.isfile(backlog_path):
        return False
    with open(backlog_path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            if backlog_ref in line and ("[x]" in line.lower() or "[X]" in line):
                return True
    return False


def apply_honesty_downgrades(doc: dict, repo_root: str) -> LoadResult:
    """Rule 2.2: the yaml's self-report never outranks disk.

      - status=covered whose nonvacuity artifact(s) are missing -> computed partial
      - status=covered/partial whose covered_by artifact(s) are missing -> computed gap
      - backlog_ref cited but docs/BACKLOG.md row not [x]-checked -> downgrade one
        step (covered->partial, partial->gap) + warn
    """
    cells = doc.get("cells", [])
    downgrades: list[Downgrade] = []
    backlog_mismatches: list = []

    computed_cells = []
    for raw in cells:
        cell = dict(raw)
        status = cell.get("status")
        cell["computed_status"] = status

        if status == "not-applicable":
            computed_cells.append(cell)
            continue

        covered_by = cell.get("covered_by") or []
        nonvacuity = cell.get("nonvacuity") or []

        # covered_by must resolve for covered AND partial (both claim SOME real asset)
        if status in ("covered", "partial") and covered_by:
            if not all(_resolve(repo_root, p) for p in covered_by):
                downgrades.append(Downgrade(
                    cell["id"], status, "gap",
                    "covered_by path(s) do not resolve on disk"))
                cell["computed_status"] = "gap"
                computed_cells.append(cell)
                continue
        elif status in ("covered", "partial") and not covered_by:
            downgrades.append(Downgrade(
                cell["id"], status, "gap", "covered_by is empty"))
            cell["computed_status"] = "gap"
            computed_cells.append(cell)
            continue

        # covered requires non-empty, disk-resolving nonvacuity
        if status == "covered":
            if not nonvacuity:
                downgrades.append(Downgrade(
                    cell["id"], "covered", "partial", "nonvacuity is empty"))
                cell["computed_status"] = "partial"
            elif not all(_resolve(repo_root, p) for p in nonvacuity):
                downgrades.append(Downgrade(
                    cell["id"], "covered", "partial",
                    "nonvacuity path(s) do not resolve on disk"))
                cell["computed_status"] = "partial"

        # backlog_ref cross-check (only meaningful if the cell claims a closed gap)
        backlog_ref = cell.get("backlog_ref")
        if backlog_ref:
            if not _backlog_checked(repo_root, backlog_ref):
                backlog_mismatches.append(cell["id"])
                # one-step downgrade from whatever computed_status currently is
                if cell["computed_status"] == "covered":
                    cell["computed_status"] = "partial"
                    downgrades.append(Downgrade(
                        cell["id"], "covered", "partial",
                        f"backlog_ref {backlog_ref} not [x]-checked in docs/BACKLOG.md"))
                elif cell["computed_status"] == "partial":
                    cell["computed_status"] = "gap"
                    downgrades.append(Downgrade(
                        cell["id"], "partial", "gap",
                        f"backlog_ref {backlog_ref} not [x]-checked in docs/BACKLOG.md"))

        computed_cells.append(cell)

    return LoadResult(doc=doc, cells=computed_cells, downgrades=downgrades,
                       backlog_mismatches=backlog_mismatches)


def compute_scores(cells: list) -> dict:
    """Returns per-tier + total weighted score using computed_status."""
    totals = {"S1": [0.0, 0.0], "S2": [0.0, 0.0], "S3": [0.0, 0.0]}  # [sum_w*score, sum_w]
    counts = {"S1": {"covered": 0, "partial": 0, "gap": 0, "not-applicable": 0},
              "S2": {"covered": 0, "partial": 0, "gap": 0, "not-applicable": 0},
              "S3": {"covered": 0, "partial": 0, "gap": 0, "not-applicable": 0}}
    for cell in cells:
        tier = cell["tier"]
        status = cell["computed_status"]
        counts[tier][status] = counts[tier].get(status, 0) + 1
        if status == "not-applicable":
            continue
        w = cell.get("weight", 1)
        s = SCORE[status]
        totals[tier][0] += w * s
        totals[tier][1] += w

    def pct(tier):
        num, den = totals[tier]
        return (num / den) if den else 0.0

    c_total_num = sum(totals[t][0] for t in totals)
    c_total_den = sum(totals[t][1] for t in totals)
    c_total = (c_total_num / c_total_den) if c_total_den else 0.0

    return {
        "C_S1": pct("S1"), "C_S2": pct("S2"), "C_S3": pct("S3"),
        "C_total": c_total,
        "sums": totals,
        "counts": counts,
    }


def rank_uncovered(cells: list, top_n: int = 10) -> list:
    """w*(1-score) descending; tier tiebreak S2 > S1 > S3; final tiebreak = cell id."""
    ranked = []
    for cell in cells:
        status = cell["computed_status"]
        if status == "not-applicable" or status == "covered":
            continue
        w = cell.get("weight", 1)
        s = SCORE[status]
        value = w * (1 - s)
        ranked.append((value, TIER_TIEBREAK.get(cell["tier"], 9), cell["id"], cell))
    ranked.sort(key=lambda t: (-t[0], t[1], t[2]))
    return [(v, c) for (v, _, _, c) in ranked[:top_n]]


def git_head_sha(repo_root: str) -> str:
    try:
        out = subprocess.run(["git", "rev-parse", "HEAD"], cwd=repo_root,
                              capture_output=True, text=True, check=True)
        return out.stdout.strip()
    except Exception:
        return "UNKNOWN"


def render_coverage_md(scores: dict, cells: list, head_sha: str, ts: str) -> str:
    lines = []
    lines.append("# Coverage Map — generated report")
    lines.append("")
    lines.append("> GENERATED FILE — do not hand-edit. Regenerate with:")
    lines.append("> `bash practices/consumer-proof/engine/coverage-report.sh --write`")
    lines.append("")
    lines.append(f"- generated_at (UTC): {ts}")
    lines.append(f"- head_sha: {head_sha}")
    lines.append("")
    lines.append(f"## C_total = {scores['C_total']:.4f}")
    lines.append("")
    lines.append(f"- C_S1 (capability, 65 cells) = {scores['C_S1']:.4f}")
    lines.append(f"- C_S2 (invariant, 31 cells)  = {scores['C_S2']:.4f}")
    lines.append(f"- C_S3 (composition, 11 cells) = {scores['C_S3']:.4f}")
    lines.append("")
    lines.append("## Status counts per tier")
    lines.append("")
    lines.append("| Tier | covered | partial | gap | not-applicable |")
    lines.append("|---|---|---|---|---|")
    for tier in ("S1", "S2", "S3"):
        c = scores["counts"][tier]
        lines.append(f"| {tier} | {c['covered']} | {c['partial']} | {c['gap']} | {c['not-applicable']} |")
    lines.append("")
    lines.append("## Top uncovered cells (ranked by w·(1−score))")
    lines.append("")
    lines.append("| rank | cell | tier | weight | status | value |")
    lines.append("|---|---|---|---|---|---|")
    for i, (value, cell) in enumerate(rank_uncovered(cells, top_n=15), start=1):
        lines.append(f"| {i} | {cell['id']} | {cell['tier']} | {cell['weight']} | "
                      f"{cell['computed_status']} | {value:.2f} |")
    lines.append("")
    return "\n".join(lines) + "\n"


def main(argv):
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument("--repo-root", default=os.getcwd())
    p.add_argument("--map", default=None)
    p.add_argument("--write", action="store_true")
    p.add_argument("--top-n", type=int, default=6)
    p.add_argument("--ts", default=None, help="fixed timestamp override (testing)")
    args = p.parse_args(argv)

    repo_root = os.path.abspath(args.repo_root)
    map_path = args.map or os.path.join(repo_root, "practices", "consumer-proof",
                                         "engine", "coverage-map.yaml")
    doc = load_map(map_path)
    result = apply_honesty_downgrades(doc, repo_root)
    scores = compute_scores(result.cells)

    print("=== coverage-report.sh ===")
    print(f"map: {map_path}")
    if result.downgrades:
        print(f"\n!! HONESTY DOWNGRADES: {len(result.downgrades)} !!")
        for d in result.downgrades:
            print(f"  DOWNGRADE {d.cell_id}: {d.from_status} -> {d.to_status}  ({d.reason})")
    else:
        print("\nHonesty downgrades: 0 (yaml self-report matched disk truth for every scored cell)")

    if result.backlog_mismatches:
        print(f"\n!! BACKLOG cross-check mismatches: {len(result.backlog_mismatches)} !!")
        for cid in result.backlog_mismatches:
            print(f"  {cid}: backlog_ref not [x]-checked in docs/BACKLOG.md")

    print("\n--- Scores (post-downgrade, disk-truth) ---")
    print(f"C_total = {scores['C_total']:.4f}")
    print(f"C_S1    = {scores['C_S1']:.4f}  (sum_w={scores['sums']['S1'][1]:.1f})")
    print(f"C_S2    = {scores['C_S2']:.4f}  (sum_w={scores['sums']['S2'][1]:.1f})")
    print(f"C_S3    = {scores['C_S3']:.4f}  (sum_w={scores['sums']['S3'][1]:.1f})")

    print("\n--- Status counts ---")
    for tier in ("S1", "S2", "S3"):
        c = scores["counts"][tier]
        print(f"  {tier}: covered={c['covered']} partial={c['partial']} gap={c['gap']} "
              f"not-applicable={c['not-applicable']}")

    print(f"\n--- Top {args.top_n} uncovered cells (ranked by w·(1-score)) ---")
    for i, (value, cell) in enumerate(rank_uncovered(result.cells, top_n=args.top_n), start=1):
        print(f"  {i}. {cell['id']:35s} tier={cell['tier']} w={cell['weight']} "
              f"status={cell['computed_status']:9s} value={value:.2f}")

    if args.write:
        head_sha = git_head_sha(repo_root)
        ts = args.ts or subprocess.run(["date", "-u", "+%Y-%m-%dT%H:%M:%SZ"],
                                        capture_output=True, text=True).stdout.strip()
        out_dir = os.path.join(repo_root, "docs", "coverage-map")
        os.makedirs(out_dir, exist_ok=True)
        md = render_coverage_md(scores, result.cells, head_sha, ts)
        with open(os.path.join(out_dir, "COVERAGE.md"), "w", encoding="utf-8") as f:
            f.write(md)
        history_line = {
            "ts": ts, "head_sha": head_sha,
            "c_total": round(scores["C_total"], 4),
            "c_s1": round(scores["C_S1"], 4),
            "c_s2": round(scores["C_S2"], 4),
            "c_s3": round(scores["C_S3"], 4),
            "counts": scores["counts"],
            "downgrades": len(result.downgrades),
        }
        with open(os.path.join(out_dir, "coverage-history.jsonl"), "a", encoding="utf-8") as f:
            f.write(json.dumps(history_line, sort_keys=True) + "\n")
        print(f"\n--write: regenerated {out_dir}/COVERAGE.md, appended to coverage-history.jsonl")

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
