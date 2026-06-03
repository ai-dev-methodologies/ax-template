#!/usr/bin/env python3
"""
design_decide.py — the 21st→ax DESIGN-DECISION ALGORITHM.

Input : a TARGET  = { page_type, persona, content_needs?, mode? }
Supply: .crawl-21st/attributes.json  (3696 crawled components w/ derived attributes)
Policy: personas.yaml                (audience -> design direction; here as the PERSONAS projection)
Output: a DESIGN DECISION SHEET:
        - theme (tokens + overrides), typography, motion budget  (from persona)
        - per content_need: ranked candidate components from the live catalog, scored

Pure-stdlib (json only) so it runs on the resource-constrained mac with no installs.
Deterministic: same target -> same ranking (ties broken by component key).

  python3 design_decide.py                      # run the built-in sample targets
  python3 design_decide.py <persona> <page>     # ad-hoc decision, prints sheet
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ATTRS = os.path.join(HERE, "attributes.json")
if not os.path.exists(ATTRS):
    ATTRS = os.path.join(HERE, "attributes.sample.json")  # shipped 40-component sample (full set: regenerate from the 21st crawl)

# components whose CODIFIED output fails the ax own-blocks lint (fed back by gen_verify.py's
# verification loop) — never recommend them, so every recommendation generates lint-clean code.
_BL = os.path.join(HERE, "lint_blocklist.json")
BLOCKLIST = set(json.load(open(_BL))) if os.path.exists(_BL) else set()

# ── PERSONAS: machine projection of personas.yaml (decision-relevant fields) ──
PERSONAS = {
    "enterprise-operator": dict(motion=1, density="compact", a11y=3,
        affinity={"table":3,"form":3,"input":3,"sidebar":2,"menu":2,"chart":2,"command":2,"button":2,"card":1},
        avoid={"shader","confetti","hero","glass","image"},
        theme=dict(radius="6px", accent_saturation="low", elevation="flat"),
        typo="geometric sans, low scale-contrast, 14px body", motion_budget="state-feedback only"),
    "consumer-delight": dict(motion=2, density="airy", a11y=2,
        affinity={"hero":3,"card":3,"button":3,"features":2,"testimonial":2,"pricing":2,"image":2,"badge":1},
        avoid={"table","terminal","code"},
        theme=dict(radius="20px", accent_saturation="high", elevation="layered"),
        typo="rounded humanist, high scale-contrast, 16px body", motion_budget="entrance + hover spring"),
    "editorial-luxury": dict(motion=2, density="airy", a11y=2,
        affinity={"hero":3,"image":3,"text":3,"gallery":2,"shader":2,"card":1,"menu":1},
        avoid={"table","form","dashboard","chart"},
        theme=dict(radius="0px", accent_saturation="low", elevation="flat"),
        typo="high-contrast serif display, extreme scale-contrast", motion_budget="refined reveal / parallax"),
    "developer-tool": dict(motion=1, density="compact", a11y=3,
        affinity={"code":3,"terminal":3,"command":3,"table":2,"menu":2,"button":2,"input":2,"card":1},
        avoid={"shader","confetti","image","hero"},
        theme=dict(radius="4px", accent_saturation="low", elevation="flat", mode="dark"),
        typo="mono code surfaces + grotesk prose, 14px", motion_budget="minimal"),
    "fintech-trust": dict(motion=1, density="comfortable", a11y=3,
        affinity={"stat":3,"chart":3,"table":3,"form":3,"card":2,"button":2,"input":2,"badge":2,
                  "sidebar":2,"menu":2,"command":1},  # a fintech console still needs navigation
        avoid={"shader","confetti","glass"},
        theme=dict(radius="8px", accent_saturation="low", elevation="subtle", tabular_nums=True),
        typo="tabular-figure grotesk, medium scale-contrast, 15px", motion_budget="conservative"),
    "playful-creator": dict(motion=3, density="comfortable", a11y=2,
        affinity={"card":3,"avatar":3,"feed":2,"badge":2,"button":2,"image":2,"hero":2,"shader":1,
                  "menu":2,"command":1},  # a social/feed app still needs navigation (tab/avatar menu)
        avoid={"table","terminal","code"},
        theme=dict(radius="24px", accent_saturation="high", elevation="layered"),
        typo="chunky rounded display, high scale-contrast, 16px", motion_budget="cinematic / reactions"),
}

# ── content need -> catalog categories that satisfy it ──
NEED_CATEGORIES = {
    "primary-cta":   ["button"],
    "headline":      ["hero", "text"],
    "value-props":   ["features", "card"],
    "social-proof":  ["testimonial", "card"],
    "pricing":       ["pricing", "card"],
    "data-grid":     ["table"],
    "metric":        ["stat", "card", "chart"],
    "chart":         ["chart"],
    "data-entry":    ["form", "input"],
    "navigation":    ["sidebar", "menu", "command"],
    "media":         ["image", "gallery"],
    "code-surface":  ["code", "terminal"],
    "identity":      ["avatar", "badge"],
    "atmosphere":    ["shader"],
}

# ── page_type -> ordered content_needs (the layout intent) ──
PAGE_BLUEPRINTS = {
    "marketing-landing": ["headline","value-props","social-proof","primary-cta"],
    "pricing":           ["headline","pricing","social-proof","primary-cta"],
    "admin-dashboard":   ["navigation","metric","chart","data-grid"],
    "auth":              ["headline","data-entry","primary-cta"],
    "feed":              ["navigation","identity","media","primary-cta"],
    "docs":              ["navigation","code-surface","headline"],
    "portfolio":         ["headline","media","atmosphere"],
    "checkout":          ["data-entry","metric","primary-cta"],
}

W = dict(affinity=0.40, quality=0.25, motion=0.20, density=0.10, avoid=0.05)
DENSITY_TARGET_LINES = {"compact": 90, "comfortable": 180, "airy": 320}


def _component_motion(a):       # 0 or 1 from attributes -> 0..3 scale proxy
    return 2 if a.get("has_motion") else 0


def score(a, persona, category):
    p = PERSONAS[persona]
    cats = a.get("category", [])
    # affinity: best matching category weight (0..3) normalized to 0..1
    aff = max([p["affinity"].get(c, 0) for c in cats] + [0]) / 3.0
    # the need-category itself must be reachable; if component isn't in `category`, 0
    if category not in cats:
        return None
    # quality: a11y present + codify-readiness (few/zero hardcoded hex) + typed(cva proxy)
    q = (0.5 if a.get("has_aria") else 0.0)
    q += 0.4 * (1.0 - min(a.get("hardcoded_hex", 0), 10) / 10.0)
    q += 0.1 if a.get("uses_cva") else 0.0
    # motion fit: closeness of component motion to persona budget
    mfit = 1.0 - abs(_component_motion(a) - p["motion"]) / 3.0
    # density fit: component complexity vs persona target
    tgt = DENSITY_TARGET_LINES[p["density"]]
    dfit = 1.0 - min(abs(a.get("lines", tgt) - tgt) / tgt, 1.0)
    base = W["affinity"]*aff + W["quality"]*q + W["motion"]*mfit + W["density"]*dfit
    # avoid penalty: multiplicative knockdown per avoid-signal hit
    hits = sum(1 for c in cats if c in p["avoid"])
    base *= (1.0 - W["avoid"]) ** hits
    return round(base, 4)


def decide(target, attrs, top_k=3):
    persona = target["persona"]
    p = PERSONAS[persona]
    needs = target.get("content_needs") or PAGE_BLUEPRINTS.get(target["page_type"], [])
    sheet = {"target": target, "persona": persona,
             "theme": {"base_spec": "specs/ux/ux-design-tokens-l0.yaml", **p["theme"]},
             "typography": p["typo"], "motion_budget": p["motion_budget"],
             "needs": {}}
    for need in needs:
        cands = []
        for cat in NEED_CATEGORIES.get(need, []):
            for key, a in attrs.items():
                if key in BLOCKLIST:                 # codified output fails ax block-lint
                    continue
                s = score(a, persona, cat)
                if s is not None:
                    cands.append((s, key, cat, a))
        cands.sort(key=lambda t: (-t[0], t[1]))
        sheet["needs"][need] = [
            {"component": k, "category": cat, "score": s,
             "codify_action": _codify_action(a)}
            for (s, k, cat, a) in cands[:top_k]
        ]
    return sheet


def _codify_action(a):
    acts = []
    if a.get("hardcoded_hex", 0) > 0:
        acts.append(f"tokenize {a['hardcoded_hex']} hex")
    if not a.get("has_aria"):
        acts.append("add role/aria")
    if not a.get("uses_cva"):
        acts.append("type variants")
    return acts or ["conformant"]


def main():
    attrs = json.load(open(ATTRS))
    if len(sys.argv) >= 3:
        targets = [{"persona": sys.argv[1], "page_type": sys.argv[2]}]
    else:
        targets = [
            {"persona": "enterprise-operator", "page_type": "admin-dashboard"},
            {"persona": "consumer-delight",    "page_type": "marketing-landing"},
            {"persona": "fintech-trust",       "page_type": "checkout"},
            {"persona": "editorial-luxury",    "page_type": "portfolio"},
            {"persona": "developer-tool",      "page_type": "docs"},
        ]
    out = [decide(t, attrs) for t in targets]
    json.dump(out, open(os.path.join(HERE, "recommendations.sample.json"), "w"), indent=1)
    for sheet in out:
        print(f"\n### {sheet['persona']}  ×  {sheet['target']['page_type']}")
        print(f"    theme={sheet['theme']}  motion={sheet['motion_budget']}")
        for need, recs in sheet["needs"].items():
            top = recs[0] if recs else None
            if top:
                print(f"    {need:13s} -> {top['component']:42s} score={top['score']}  codify={top['codify_action']}")
            else:
                print(f"    {need:13s} -> (no catalog candidate)")


if __name__ == "__main__":
    main()
