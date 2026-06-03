#!/usr/bin/env python3
"""
validate_composition.py — the VALIDATION HARNESS (검증할 수 있게).

The recommender (compose.py) proposes component compositions; this harness mechanically validates
every recommendation, ax-style: ONE command, binary PASS/FAIL, with a per-check breakdown. This is
what makes the recommendation agent *trustworthy* — a recommendation that fails any hard check is
not shippable, exactly like a domain that fails ./gradlew test{Domain}.

HARD checks (any FAIL -> the variant FAILs -> exit 1):
  exist          every recommended component key is a real catalog component (attributes.json)
  codify-ready   every pick has a known normalization path (codify_action present)
  needs-covered  every need in the variant resolved to >= 1 component (no empty slot)
  persona-fit    every top pick scores >= PERSONA_FIT_MIN (a genuine fit, not a desperate fallback)
  avoid-respect  no pick's category intersects the persona's avoid set (validates the avoid policy)

SOFT checks (WARN, surfaced not blocking — the honest heuristic-categorization limits):
  over-reuse     a component chosen for >= 3 distinct needs across the plan (low category confidence)
  low-confidence a pick whose need-category is not present in its slug (heuristic tag, not slug-proven)

  python3 validate_composition.py            # validate compositions.json -> validation_report.json
  exit 0 = all variants PASS · exit 1 = at least one hard FAIL
"""
import json, os, sys
import design_decide as dd

HERE = os.path.dirname(os.path.abspath(__file__))
COMPOS = os.path.join(HERE, "compositions.json")
ATTRS = dd.ATTRS
PERSONA_FIT_MIN = 0.42   # calibrated: an affinity-2 need (0.27 affinity term) + baseline quality floors here;
                         # below this a pick is a desperation fallback, not a genuine fit
OVER_REUSE_N = 3


def validate_plan(plan, attrs):
    persona = plan["persona"]
    avoid = dd.PERSONAS[persona]["avoid"]
    report = {"service_plan": plan["service_plan"], "persona": persona, "variants": {}}
    reuse = {}  # component -> set(needs) across the whole plan
    for page, variants in plan["pages"].items():
        for vname, v in variants.items():
            vkey = f"{page}/{vname}"
            checks = {"exist": True, "codify-ready": True, "needs-covered": True,
                      "persona-fit": True, "avoid-respect": True}
            warns = []
            details = []
            for need, recs in v["needs"].items():
                if not recs:
                    checks["needs-covered"] = False
                    details.append(f"{need}: NO candidate")
                    continue
                pick = recs[0]
                comp = pick["component"]
                reuse.setdefault(comp, set()).add(need)
                a = attrs.get(comp)
                if a is None:
                    checks["exist"] = False
                    details.append(f"{need}: {comp} NOT in catalog")
                    continue
                if not pick.get("codify_action"):
                    checks["codify-ready"] = False
                if pick.get("score", 0) < PERSONA_FIT_MIN:
                    checks["persona-fit"] = False
                    details.append(f"{need}: {comp} fit {pick['score']} < {PERSONA_FIT_MIN}")
                if set(a.get("category", [])) & avoid:
                    checks["avoid-respect"] = False
                    details.append(f"{need}: {comp} category {sorted(set(a['category']) & avoid)} in persona.avoid")
                # soft: slug confidence
                slug = comp.split("/", 1)[1].lower().replace("_", "-").split("-")
                if pick["category"] not in slug:
                    warns.append(f"{need}:{comp.split('/')[-1]} (category '{pick['category']}' not slug-proven)")
            passed = all(checks.values())
            report["variants"][vkey] = {"pass": passed, "checks": checks,
                                        "warnings": warns, "details": details}
    # plan-level over-reuse warning
    over = {c: sorted(n) for c, n in reuse.items() if len(n) >= OVER_REUSE_N}
    report["over_reuse"] = over
    report["pass"] = all(v["pass"] for v in report["variants"].values())
    return report


def main():
    attrs = json.load(open(ATTRS))
    compos = json.load(open(COMPOS))
    reports = [validate_plan(p, attrs) for p in compos]
    json.dump(reports, open(os.path.join(HERE, "validation_report.json"), "w"), indent=1)

    all_pass = True
    hard_checks = ["exist", "codify-ready", "needs-covered", "persona-fit", "avoid-respect"]
    n_variants = n_pass = 0
    for r in reports:
        print(f"\n=== {r['service_plan']} (persona: {r['persona']}) — plan {'PASS' if r['pass'] else 'FAIL'} ===")
        for vkey, v in r["variants"].items():
            n_variants += 1
            n_pass += 1 if v["pass"] else 0
            mark = "PASS" if v["pass"] else "FAIL"
            failed = [c for c in hard_checks if not v["checks"][c]]
            print(f"  [{mark}] {vkey:34s} {'all hard checks' if not failed else 'FAILED: ' + ','.join(failed)}")
            for d in v["details"]:
                print(f"          ! {d}")
        if r["over_reuse"]:
            for c, needs in r["over_reuse"].items():
                print(f"  WARN over-reuse: {c} chosen for {len(needs)} needs {needs}")
        all_pass &= r["pass"]

    print(f"\n=== VALIDATION SUMMARY: {n_pass}/{n_variants} variants PASS hard checks "
          f"across {len(reports)} service plans -> {'GREEN' if all_pass else 'RED'} ===")
    sys.exit(0 if all_pass else 1)


if __name__ == "__main__":
    main()
