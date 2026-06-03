#!/usr/bin/env python3
"""
recommend.py — the DESIGN-COMPOSER AGENT entrypoint (one command = the agent).

Pipeline: service_plans.yaml  ->  compose (multiple variants/page)  ->  validate (binary gate)  ->  verdict.
Only variants that PASS every hard validation check are surfaced as RECOMMENDED; failing variants are
reported with their failed checks (honest, not hidden). This is the agent's contract: a recommendation
is only emitted if it is mechanically valid against the catalog — the ax "single command, binary verdict"
philosophy applied to design composition.

  python3 recommend.py                       # recommend + validate every plan in service_plans.yaml
  python3 recommend.py "<persona>" "<page>"  # ad-hoc single page
  exit 0 = every recommended composition is validation-GREEN · exit 1 = a plan has no valid variant
"""
import json, os, sys
import yaml
import compose as C
import validate_composition as V
import design_decide as dd

HERE = os.path.dirname(os.path.abspath(__file__))
PLANS = os.path.join(HERE, "service_plans.yaml")


def load_plans():
    if len(sys.argv) >= 3:
        return [{"name": f"adhoc:{sys.argv[2]}", "persona": sys.argv[1], "pages": [sys.argv[2]]}]
    return yaml.safe_load(open(PLANS))["plans"]


def recommend(plan, attrs):
    composed = C.compose_plan(plan, attrs)          # multiple variants/page
    verdict = V.validate_plan(composed, attrs)      # binary gate per variant
    # join: attach the validation pass/fail to each variant, mark the recommended pick
    out = {"service_plan": plan["name"], "persona": plan["persona"],
           "theme": composed["theme"], "typography": composed["typography"],
           "pages": {}, "pass": verdict["pass"], "over_reuse": verdict["over_reuse"]}
    for page, variants in composed["pages"].items():
        out["pages"][page] = {}
        for vname, v in variants.items():
            vres = verdict["variants"][f"{page}/{vname}"]
            out["pages"][page][vname] = {
                "intent": v["intent"], "motion_budget_level": v["motion_budget_level"],
                "recommended": vres["pass"],
                "failed_checks": [c for c, ok in vres["checks"].items() if not ok],
                "composition": [{"need": need, "component": recs[0]["component"],
                                 "score": recs[0]["score"], "codify": recs[0]["codify_action"]}
                                for need, recs in v["needs"].items() if recs],
                "warnings": vres["warnings"],
            }
    return out


def main():
    attrs = json.load(open(dd.ATTRS))
    plans = load_plans()
    results = [recommend(p, attrs) for p in plans]
    json.dump(results, open(os.path.join(HERE, "recommendations.json"), "w"), indent=1)

    all_green = True
    for r in results:
        verdict = "GREEN" if r["pass"] else "RED"
        print(f"\n╔══ {r['service_plan']}  ·  persona={r['persona']}  ·  {verdict}")
        print(f"║   theme={r['theme'].get('radius')}/{r['theme'].get('accent_saturation')}  type={r['typography'][:48]}")
        for page, variants in r["pages"].items():
            print(f"║   ┌ page: {page}")
            for vname, v in variants.items():
                mark = "✓ RECOMMENDED" if v["recommended"] else f"✗ rejected ({','.join(v['failed_checks'])})"
                comp = "  ".join(f"{c['need']}={c['component'].split('/')[-1]}" for c in v["composition"])
                print(f"║   │  [{vname:10s} motion{v['motion_budget_level']}] {mark}")
                print(f"║   │     {comp}")
        if r["over_reuse"]:
            for c, needs in r["over_reuse"].items():
                print(f"║   ⚠ over-reuse (low category confidence): {c.split('/')[-1]} across {needs}")
        all_green &= r["pass"]
    n_plans = len(results)
    n_green = sum(1 for r in results if r["pass"])
    print(f"\n╚══ AGENT VERDICT: {n_green}/{n_plans} service plans fully validation-GREEN "
          f"-> {'ALL GREEN' if all_green else 'has RED'} (recommendations.json)")
    sys.exit(0 if all_green else 1)


if __name__ == "__main__":
    main()
