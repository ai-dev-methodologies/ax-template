#!/usr/bin/env python3
"""
_explain_helper.py -- Python backend for explain.sh (F15).

Invoked as:
  python3 _explain_helper.py list   <rules_dir> <format>
  python3 _explain_helper.py explain <rules_dir> <query> <format>
"""
import sys
import re
import json
import pathlib
import textwrap


def extract_fm_value(fm: str, key: str, default: str = "") -> str:
    """Extract a scalar YAML value from front-matter text."""
    found = re.search(rf"^{key}:\s*(.+)$", fm, re.MULTILINE)
    if not found:
        return default
    return found.group(1).strip().strip("\"'")


def extract_fm_list(fm: str, key: str) -> list:
    """Extract a YAML block-list value from front-matter text."""
    found = re.search(rf"^{key}:\n((?:  - .+\n)*)", fm, re.MULTILINE)
    if not found:
        return []
    return [
        line.strip().lstrip("- ").strip("\"'")
        for line in found.group(1).strip().split("\n")
        if line.strip()
    ]


def parse_rule(rule_path: pathlib.Path) -> dict | None:
    """Parse YAML front-matter from a rule file. Returns dict or None."""
    text = rule_path.read_text(encoding="utf-8")
    m = re.match(r"^---\n(.*?)\n---\n?(.*)", text, re.DOTALL)
    if not m:
        return None
    fm, body = m.group(1), m.group(2)

    raw_spec = extract_fm_value(fm, "spec_ref")
    spec_id = raw_spec.split("#")[-1] if "#" in raw_spec else raw_spec

    return {
        "file": rule_path.name,
        "spec_id": spec_id,
        "spec_ref": raw_spec,
        "title": extract_fm_value(fm, "title"),
        "impact": extract_fm_value(fm, "impact"),
        "impact_desc": extract_fm_value(fm, "impactDescription"),
        "tags": extract_fm_list(fm, "tags"),
        "upstream": extract_fm_list(fm, "upstream"),
        "_body": body,
        "_fm": fm,
    }


def iter_rules(rules_dir: pathlib.Path):
    for p in sorted(rules_dir.glob("*.md")):
        if p.name.startswith("_") or p.name == ".gitkeep":
            continue
        r = parse_rule(p)
        if r:
            yield r


# ---------------------------------------------------------------------------
# list mode
# ---------------------------------------------------------------------------
def cmd_list(rules_dir: pathlib.Path, fmt: str) -> int:
    rows = []
    for r in iter_rules(rules_dir):
        rows.append((r["spec_id"] or r["file"], r["title"], r["impact"]))

    if fmt == "json":
        print(json.dumps([{"spec_id": s, "title": t, "impact": i} for s, t, i in rows], indent=2))
    else:
        col_w = 40
        print(f"\n{'RULE ID':{col_w}}  {'IMPACT':6}  TITLE")
        print("-" * 100)
        for spec_id, title, impact in rows:
            print(f"  {spec_id:{col_w-2}}  [{impact:6}]  {title}")
    return 0


# ---------------------------------------------------------------------------
# explain mode
# ---------------------------------------------------------------------------
def find_rule(rules_dir: pathlib.Path, query: str) -> dict | None:
    query_up = query.strip().upper()
    best = None
    best_score = 0

    for r in iter_rules(rules_dir):
        spec_id_up = r["spec_id"].upper()
        title_up = r["title"].upper()
        fname_up = r["file"].upper()
        fm_lower = r["_fm"].lower()

        # Exact spec_id match
        if query_up == spec_id_up:
            return r

        # Suffix match (e.g. PERS-005 -> PRACTICES-PERS-005)
        if spec_id_up.endswith(query_up) and len(query_up) >= 4:
            score = 80
        elif query.lower().replace("-", " ") in title_up.lower():
            score = 60
        elif query.lower().replace("-", " ").replace(" ", "-") in fname_up.lower():
            score = 50
        elif query.lower() in fm_lower:
            score = 30
        else:
            continue

        if score > best_score:
            best_score = score
            best = r

    return best


def cmd_explain(rules_dir: pathlib.Path, query: str, fmt: str) -> int:
    rule = find_rule(rules_dir, query)
    if not rule:
        print(f"explain: no rule found matching '{query}'", file=sys.stderr)
        print("Hint: use --list to see all rule IDs", file=sys.stderr)
        return 1

    body = rule["_body"]

    # Extract code blocks
    correct_blocks = re.findall(
        r"(?:Correct|CORRECT)[^\n]*\n+```[^\n]*\n(.*?)```",
        body,
        re.DOTALL,
    )
    incorrect_blocks = re.findall(
        r"(?:Incorrect|INCORRECT)[^\n]*\n+```[^\n]*\n(.*?)```",
        body,
        re.DOTALL,
    )

    if fmt == "json":
        obj = {
            "spec_id": rule["spec_id"],
            "title": rule["title"],
            "impact": rule["impact"],
            "impact_description": rule["impact_desc"],
            "spec_ref": rule["spec_ref"],
            "tags": rule["tags"],
            "upstream": rule["upstream"],
            "correct_examples": len(correct_blocks),
            "incorrect_examples": len(incorrect_blocks),
        }
        print(json.dumps(obj, indent=2))
        return 0

    # Text format
    width = 80
    print("=" * width)
    print(f"  {rule['spec_id']}")
    print(f"  {rule['title']}")
    print("=" * width)
    print(f"  Impact  : [{rule['impact']}] {rule['impact_desc']}")
    print(f"  Spec    : {rule['spec_ref']}")
    tags_str = ", ".join(rule["tags"])
    print(f"  Tags    : {tags_str}")
    if rule["upstream"]:
        print(f"  Upstream: {rule['upstream'][0]}")
        for u in rule["upstream"][1:]:
            print(f"            {u}")
    print()

    # Body excerpt (first 600 chars of non-code content)
    body_text = re.sub(r"```.*?```", "", body, flags=re.DOTALL)
    body_text = re.sub(r"\n{3,}", "\n\n", body_text).strip()
    excerpt = body_text[:600]
    if len(body_text) > 600:
        excerpt += " ..."
    for line in excerpt.split("\n"):
        print("  " + line)

    if correct_blocks:
        print()
        print("  -- Correct pattern ------------------------------------------")
        for block in correct_blocks[:1]:
            for line in textwrap.dedent(block).strip().split("\n")[:20]:
                print("  " + line)

    if incorrect_blocks:
        print()
        print("  -- Incorrect pattern ----------------------------------------")
        for block in incorrect_blocks[:1]:
            for line in textwrap.dedent(block).strip().split("\n")[:20]:
                print("  " + line)

    print()
    print(f"  File: {rule['file']}")
    return 0


# ---------------------------------------------------------------------------
# entry point
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: _explain_helper.py list <rules_dir> [format]", file=sys.stderr)
        print("       _explain_helper.py explain <rules_dir> <query> [format]", file=sys.stderr)
        sys.exit(2)

    cmd = sys.argv[1]
    rules_dir = pathlib.Path(sys.argv[2])

    if not rules_dir.is_dir():
        print(f"explain: rules directory not found: {rules_dir}", file=sys.stderr)
        sys.exit(2)

    if cmd == "list":
        fmt = sys.argv[3] if len(sys.argv) > 3 else "text"
        sys.exit(cmd_list(rules_dir, fmt))

    elif cmd == "explain":
        if len(sys.argv) < 4:
            print("explain: query argument required", file=sys.stderr)
            sys.exit(2)
        query = sys.argv[3]
        fmt = sys.argv[4] if len(sys.argv) > 4 else "text"
        sys.exit(cmd_explain(rules_dir, query, fmt))

    else:
        print(f"explain: unknown command '{cmd}'", file=sys.stderr)
        sys.exit(2)
