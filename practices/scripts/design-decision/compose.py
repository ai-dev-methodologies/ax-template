#!/usr/bin/env python3
"""
compose.py — the COMPOSITION RECOMMENDER (각 서비스 기획에 맞게 여러가지 컴포넌트 구성 추천).

design_decide.py picks ONE component per need. compose.py goes a layer up: for a whole SERVICE PLAN
(a 서비스 기획: persona + the pages the product needs), it emits MULTIPLE composition VARIANTS per page
— a deliberately different design strategy each — so a product team can compare options:

  · lean        — MVP: only the essential needs, simplest/most-conformant components, minimal motion
  · conversion  — growth: full funnel, CTA/social-proof/pricing boosted, motion allowed
  · premium     — brand: hero/media/atmosphere boosted, richest visual + highest motion budget

Each variant resolves to a concrete ax component set (from the 3719-component catalog), the persona
theme, a motion budget, and the per-component codify actions. Reuses design_decide's scoring.

  python3 compose.py                          # compose the built-in service plans -> compositions.json
  python3 compose.py <persona> <page_type>    # ad-hoc single page, all variants
"""
import json, os, sys
import design_decide as dd

HERE = os.path.dirname(os.path.abspath(__file__))

# A variant = a design STRATEGY: which needs to include, which categories to boost, motion tolerance.
VARIANTS = {
    "lean": dict(
        intent="MVP — ship the essential surface, cheapest to codify, calm",
        keep_first=3,                    # only the first N essential needs of the page
        boost={},                        # no category boosting — take the cleanest pick
        prefer_conformant=0.15,          # extra reward for already-conformant (low codify cost)
        motion_delta=-1),                # pull motion down a notch
    "conversion": dict(
        intent="Growth — full funnel, CTA + social proof + pricing emphasized",
        keep_first=99,
        add_needs=["primary-cta", "social-proof"],
        boost={"button": 0.18, "testimonial": 0.15, "pricing": 0.15, "card": 0.08},
        prefer_conformant=0.0,
        motion_delta=0),
    "premium": dict(
        intent="Brand — hero/media forward, richest visual, highest motion budget",
        keep_first=99,
        add_needs=["headline", "media", "atmosphere"],
        boost={"hero": 0.18, "image": 0.15, "text": 0.12, "shader": 0.10, "gallery": 0.10},
        prefer_conformant=-0.05,         # tolerate a bit more codify work for richness
        motion_delta=+1),
}


def _variant_score(key, a, persona, category, variant):
    base = dd.score(a, persona, category)
    if base is None:
        return None
    v = VARIANTS[variant]
    avoid = dd.PERSONAS[persona]["avoid"]
    s = base
    # boost the strategy's categories — but NEVER a category the persona avoids (respect avoid over variant)
    if category not in avoid:
        s += v["boost"].get(category, 0.0)
    # slug-confidence: when the need's category keyword is literally in the slug, the category tag is
    # high-confidence (disambiguates noisy category-page memberships, e.g. a 'tabs' block winning 'metric')
    slug = key.split("/", 1)[1].lower()
    if category in slug.replace("_", "-").split("-"):
        s += 0.12
    if v["prefer_conformant"] and dd._codify_action(a) == ["conformant"]:
        s += v["prefer_conformant"]
    # motion nudge: reward components whose motion matches the variant-shifted budget
    target = max(0, min(3, dd.PERSONAS[persona]["motion"] + v["motion_delta"]))
    s += 0.05 * (1.0 - abs(dd._component_motion(a) - target) / 3.0)
    # category-breadth penalty: a component tagged into many categories is likely MIS-tagged
    # (low-confidence supply); demote it so a focused, slug-confident pick wins the need
    s -= 0.04 * max(0, len(a.get("category", [])) - 4)
    return round(s, 4)


def _needs_for(page_type, persona, variant):
    needs = list(dd.PAGE_BLUEPRINTS.get(page_type, []))
    avoid = dd.PERSONAS[persona]["avoid"]
    v = VARIANTS[variant]
    for n in v.get("add_needs", []):
        if n in needs or n not in dd.NEED_CATEGORIES:
            continue
        # don't add a strategy need whose every candidate category is persona-avoided
        if all(c in avoid for c in dd.NEED_CATEGORIES[n]):
            continue
        needs.append(n)
    return needs[: v["keep_first"]]


def compose_page(page_type, persona, attrs, variant, top_k=2):
    needs = _needs_for(page_type, persona, variant)
    avoid = dd.PERSONAS[persona]["avoid"]
    out = {}
    for need in needs:
        cands = []
        for cat in dd.NEED_CATEGORIES.get(need, []):
            if cat in avoid:                      # hard avoid: never offer an avoided-category pick
                continue
            for key, a in attrs.items():
                if set(a.get("category", [])) & avoid:   # the component itself carries an avoided tag
                    continue
                s = _variant_score(key, a, persona, cat, variant)
                if s is not None:
                    cands.append((s, key, cat, a))
        cands.sort(key=lambda t: (-t[0], t[1]))
        if not cands:                              # no clean pick for this persona -> omit, don't fabricate
            continue
        out[need] = [{"component": k, "category": cat, "score": s,
                      "codify_action": dd._codify_action(a)}
                     for (s, k, cat, a) in cands[:top_k]]
    return out


def compose_plan(plan, attrs):
    persona = plan["persona"]
    p = dd.PERSONAS[persona]
    result = {"service_plan": plan["name"], "persona": persona,
              "theme": {"base_spec": "specs/react-practices-l0.yaml#REACT-PRACTICES-UX-001", **p["theme"]},
              "typography": p["typo"], "pages": {}}
    for page in plan["pages"]:
        result["pages"][page] = {}
        for variant in VARIANTS:
            tgt = max(0, min(3, p["motion"] + VARIANTS[variant]["motion_delta"]))
            result["pages"][page][variant] = {
                "intent": VARIANTS[variant]["intent"],
                "motion_budget_level": tgt,
                "needs": compose_page(page, persona, attrs, variant),
            }
    return result


# Built-in 서비스 기획 — overridden by service_plans.yaml when present (see G3 / recommender agent)
SAMPLE_PLANS = [
    {"name": "B2B analytics console", "persona": "enterprise-operator",
     "pages": ["admin-dashboard", "auth"]},
    {"name": "D2C storefront", "persona": "consumer-delight",
     "pages": ["marketing-landing", "pricing"]},
    {"name": "fintech remittance", "persona": "fintech-trust",
     "pages": ["checkout", "admin-dashboard"]},
]


def main():
    attrs = json.load(open(dd.ATTRS))
    if len(sys.argv) >= 3:
        plans = [{"name": f"adhoc:{sys.argv[2]}", "persona": sys.argv[1], "pages": [sys.argv[2]]}]
    else:
        plans = SAMPLE_PLANS
    out = [compose_plan(pl, attrs) for pl in plans]
    json.dump(out, open(os.path.join(HERE, "compositions.json"), "w"), indent=1)
    for r in out:
        print(f"\n=== {r['service_plan']}  (persona: {r['persona']}) ===")
        for page, variants in r["pages"].items():
            print(f"  page: {page}")
            for vname, v in variants.items():
                picks = " · ".join(
                    f"{need}:{recs[0]['component'].split('/')[-1]}" for need, recs in v["needs"].items() if recs)
                print(f"    [{vname:10s} motion{v['motion_budget_level']}] {picks}")


if __name__ == "__main__":
    main()
