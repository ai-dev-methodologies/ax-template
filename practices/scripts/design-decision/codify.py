#!/usr/bin/env python3
"""
codify.py — deterministic 21st.dev TSX -> ax-template normalizer + full work-order.

Two outputs, resource-safe (no network, no installs):
  1. codify_plan.json  — EVERY one of the 3696 crawled components gets a plan entry:
       { actions, target_path, hex, has_aria, uses_cva }.  Full-catalog coverage of the
       "전체 ax-template화" — every component is accounted for with its exact normalization work.
  2. codified/blocks/<slug>.tsx (+ .notes.md) — the transform actually run on a representative
       batch (the components the design-decision algorithm selected), proving it executes end-to-end.

Deterministic transform (behavior-preserving where safe; flags what needs a semantic pass):
  · prepend `@ax-codified-from` provenance + a normalization summary
  · hex EXTRACTION -> tokens: unique hex literals lifted into an --ax-c-N token block; tailwind
    arbitrary `[#hex]` and inline `style` hex rewritten to `var(--ax-c-N)` (the theme can override).
    JS-string hex is listed, not rewritten (can't prove it's a color without semantics).
  · a11y / typed-variants: NOT auto-inserted (needs semantics) — emitted as explicit NOTES actions.

  python3 codify.py            # build full plan + codify the algorithm-selected batch
  python3 codify.py <user/slug>...   # codify specific components
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..", "..")        # repo root (HERE = .crawl-21st/codified/design-system)
CODE = os.path.join(ROOT, ".crawl-21st", "code")
ATTRS = os.path.join(ROOT, ".crawl-21st", "attributes.json")
OUT = os.path.join(HERE, "..", "blocks")           # .crawl-21st/codified/blocks
PLAN = os.path.join(HERE, "..", "codify_plan.json")
RECS = os.path.join(HERE, "recommendations.sample.json")

HEX = re.compile(r"#[0-9a-fA-F]{6}\b|#[0-9a-fA-F]{3}\b")


def actions_for(a):
    acts = []
    if a.get("hardcoded_hex", 0):
        acts.append(f"tokenize:{a['hardcoded_hex']}-hex")
    if not a.get("has_aria"):
        acts.append("a11y:add-role-aria")
    if not a.get("uses_cva"):
        acts.append("types:string-literal-variants")
    return acts or ["conformant"]


def primary_cat(a):
    cats = [c for c in a.get("category", []) if c != "uncategorized"]
    return cats[0] if cats else "misc"


def build_plan(attrs):
    plan = {}
    for key, a in attrs.items():
        plan[key] = {
            "actions": actions_for(a),
            "target_path": f"templates/L2/blocks/{primary_cat(a)}/{key.split('/',1)[1]}.tsx",
            "hex": a.get("hardcoded_hex", 0),
            "has_aria": a.get("has_aria", False),
            "uses_cva": a.get("uses_cva", False),
        }
    json.dump(plan, open(PLAN, "w"), indent=0)
    return plan


def codify_one(key, attrs):
    user, slug = key.split("/", 1)
    src_path = os.path.join(CODE, f"{user}__{slug}.tsx")
    if not os.path.exists(src_path):
        return None
    src = open(src_path, errors="ignore").read()
    a = attrs.get(key, {})

    # --- deterministic hex -> token extraction ---
    uniq = []
    for h in HEX.findall(src):
        hl = h.lower()
        if hl not in uniq:
            uniq.append(hl)
    token_map = {h: f"--ax-c-{i+1}" for i, h in enumerate(uniq)}
    body = src
    for h, tok in token_map.items():
        # tailwind arbitrary [#hex] -> [var(--ax-c-N)]   (case-insensitive on the hex)
        body = re.sub(r"\[" + re.escape(h) + r"\]",
                      f"[var({tok})]", body, flags=re.IGNORECASE)
        # inline style / css string '#hex' or "#hex" -> var(--ax-c-N)
        body = re.sub(r"(['\"])" + re.escape(h) + r"\1",
                      f"\\1var({tok})\\1", body, flags=re.IGNORECASE)

    token_block = ""
    if token_map:
        decls = "\n".join(f" *   {tok}: {h};" for h, tok in token_map.items())
        token_block = (
            "/* ax design tokens extracted from hardcoded hex — bind these in your theme\n"
            " * (light/dark/brand) so this block re-skins without edits:\n"
            f"{decls}\n */\n")

    header = (
        "/**\n"
        f" * @ax-codified-from 21st.dev/{key}\n"
        " * @ax-layer L2/blocks/" + primary_cat(a) + "\n"
        " * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.\n"
        " * REMAINING semantic pass (see .notes.md): "
        + ", ".join(x for x in actions_for(a) if not x.startswith("tokenize")) + "\n"
        " */\n")

    os.makedirs(OUT, exist_ok=True)
    out_tsx = os.path.join(OUT, f"{slug}.tsx")
    open(out_tsx, "w").write(header + token_block + body)
    notes = (
        f"# codify notes — {key}\n\n"
        f"- target ax path: `templates/L2/blocks/{primary_cat(a)}/{slug}.tsx`\n"
        f"- hex tokenized: {len(token_map)} unique -> {', '.join(token_map.values()) or 'none'}\n"
        f"- a11y: {'has aria' if a.get('has_aria') else 'NEEDS role/aria (semantic pass)'}\n"
        f"- variants: {'typed (cva)' if a.get('uses_cva') else 'NEEDS string-literal union typing'}\n"
        f"- motion: {'has motion' if a.get('has_motion') else 'static'}\n")
    open(os.path.join(OUT, f"{slug}.notes.md"), "w").write(notes)
    return {"key": key, "hex_tokenized": len(token_map), "out": out_tsx,
            "remaining": [x for x in actions_for(a) if not x.startswith("tokenize")]}


def selected_keys():
    if not os.path.exists(RECS):
        return []
    seen, keys = set(), []
    for sheet in json.load(open(RECS)):
        for recs in sheet["needs"].values():
            for r in recs:
                k = r["component"]
                if k not in seen:
                    seen.add(k); keys.append(k)
    return keys


def main():
    attrs = json.load(open(ATTRS))
    plan = build_plan(attrs)
    # plan coverage stats
    tot = len(plan)
    need_token = sum(1 for p in plan.values() if p["hex"])
    need_aria = sum(1 for p in plan.values() if not p["has_aria"])
    need_types = sum(1 for p in plan.values() if not p["uses_cva"])
    conformant = sum(1 for p in plan.values() if p["actions"] == ["conformant"])
    print(f"codify_plan.json: {tot} components covered "
          f"(tokenize={need_token}, add-aria={need_aria}, type-variants={need_types}, "
          f"already-conformant={conformant})")

    keys = sys.argv[1:] or selected_keys()
    done = [r for k in keys if (r := codify_one(k, attrs))]
    print(f"codified batch: {len(done)} components -> {OUT}/")
    for r in done:
        print(f"  {r['key']:48s} hex->{r['hex_tokenized']:2d} tokens  remaining={r['remaining']}")


if __name__ == "__main__":
    main()
