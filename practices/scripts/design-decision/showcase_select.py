#!/usr/bin/env python3
"""
showcase_select.py — the agent SELECTS which codified blocks each persona's showcase renders.

Turns the flat /showcase gallery into a PERSONA-DRIVEN one: for each of the 6 personas, pick the
subset of the shipped codified blocks that fit the persona's design direction (personas.yaml
affinity/avoid), and emit the persona theme so the frontend re-skins per persona. This is the link
the UI/UX audit said was missing — the recommender/agent driving the rendered showcase.

Fit rule (per block, per persona):
  fit  ⟺  NO block category is in the persona's `avoid`  AND
          ( some block category is in the persona's `affinity`  OR  the block is a universal primitive )
Universal primitives (badge/status/button/card/text) are usable by any persona; specialized blocks
(hero/shader/image/chart/gallery) need an affinity match. Blocks are ordered affinity-match-first.

  python3 showcase_select.py   # writes frontend/src/components/showcase/personas-showcase.json (the /showcase routes import it)
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import design_decide as dd

HERE = os.path.dirname(os.path.abspath(__file__))

# the 18 shipped codified blocks -> their categories (matches templates/L2/blocks + frontend mirror)
BLOCK_MANIFEST = {
    "status-badge":            ["status", "badge"],
    "animated-badge":          ["badge"],
    "prime-button":            ["button"],
    "animated-arrow-button":   ["button"],
    "social-button":           ["button"],
    "interfaces-card":         ["card"],
    "animated-feature-card":   ["card", "features"],
    "auto-layout-card":        ["card"],
    "category-bar-chart":      ["chart", "stat"],
    "split-text-effect":       ["text"],
    "image-swiper":            ["image", "gallery"],
    "ai-image-generator-hero": ["hero", "image"],
    "cybernetic-grid-shader":  ["shader"],
    "futurastic-hero-section": ["hero", "shader"],
    # per-persona thinness closure (form/table/avatar/code surfaces the audit found missing)
    "form-field":              ["form", "input"],
    "data-grid":               ["table", "stat"],
    "avatar-group":            ["avatar"],
    "code-snippet":            ["code", "terminal"],
}
UNIVERSAL = {"badge", "status", "button", "card", "text"}

DISPLAY = {
    "enterprise-operator": "Enterprise Operator",
    "consumer-delight": "Consumer Delight",
    "editorial-luxury": "Editorial / Luxury",
    "developer-tool": "Developer Tool",
    "fintech-trust": "Fintech Trust",
    "playful-creator": "Playful Creator",
}


def select(persona):
    p = dd.PERSONAS[persona]
    affinity, avoid = p["affinity"], p["avoid"]
    picks = []
    for slug, cats in BLOCK_MANIFEST.items():
        if any(c in avoid for c in cats):
            continue                                   # persona avoids this block's direction
        aff = max([affinity.get(c, 0) for c in cats] + [0])
        if aff == 0 and not any(c in UNIVERSAL for c in cats):
            continue                                   # specialized + no affinity -> skip
        picks.append((aff, slug))
    picks.sort(key=lambda t: (-t[0], t[1]))            # affinity-match first
    return [slug for _, slug in picks]


def main():
    out = {}
    for persona in dd.PERSONAS:
        p = dd.PERSONAS[persona]
        out[persona] = {
            "name": DISPLAY[persona],
            "blocks": select(persona),
            "theme": p["theme"],                       # radius / accent_saturation / elevation / (mode)
            "motion_budget": p["motion_budget"],
            "motion_level": p["motion"],
            "typography": p["typo"],
        }
    # Write DIRECTLY to the file the frontend imports (frontend/src/components/showcase/
    # personas-showcase.json, hyphen) — not a throwaway copy in this dir — so re-running the
    # recommender after a BLOCK_MANIFEST/PERSONAS edit actually reskins the live showcase.
    path = os.path.normpath(
        os.path.join(HERE, "..", "..", "..", "frontend", "src", "components", "showcase", "personas-showcase.json")
    )
    with open(path, "w") as fh:
        json.dump(out, fh, indent=1)
    print(f"{os.path.relpath(path)} -> {len(out)} personas")
    for persona, v in out.items():
        avoid = sorted(dd.PERSONAS[persona]["avoid"])
        print(f"  {persona:20s} {len(v['blocks']):2d} blocks  (avoid {avoid})")
        print(f"      {', '.join(v['blocks'])}")


if __name__ == "__main__":
    main()
