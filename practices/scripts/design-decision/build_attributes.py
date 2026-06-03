#!/usr/bin/env python3
"""
build_attributes.py — reproducible "ax에 맞게 재구성" data foundation.

Derives attributes.json (the supply layer that design_decide.py / compose.py score against)
from a populated 21st.dev crawl. Pure stdlib; run where the (gitignored) crawl exists:

    python3 build_attributes.py            # writes ../../../.crawl-21st/attributes.json
                                           # + refreshes the shipped attributes.sample.json (40 comps)

Per component it derives: category[] (from the 62 crawled category pages + slug-keyword inference),
lines, has_motion, icon_lib, uses_cva, hardcoded_hex, has_aria, is_client, ext_libs[].
This is the deterministic transform behind the recommendation system — not the components themselves,
which stay in the ignored crawl; this codifies them into a scored catalog the recommender consumes.
"""
import json, os, re, glob, collections

HERE = os.path.dirname(os.path.abspath(__file__))
CRAWL = os.path.join(HERE, "..", "..", "..", ".crawl-21st")
CODE = os.path.join(CRAWL, "code")
PAGES = os.path.join(CRAWL, "pages")
OUT = os.path.join(CRAWL, "attributes.json")
SAMPLE = os.path.join(HERE, "attributes.sample.json")

# slug-token -> canonical category (fills the long tail the 62 category pages miss)
VOCAB = {"chart","graph","table","hero","button","btn","card","form","input","calendar","date",
 "menu","sidebar","nav","navbar","command","palette","terminal","console","code","editor","avatar",
 "badge","pill","tag","stat","metric","kpi","pricing","price","plan","testimonial","review","feature",
 "features","gallery","carousel","slider","feed","timeline","shader","wave","gradient","text","title",
 "heading","image","img","photo","dialog","modal","toast","tooltip","tabs","tab","accordion","dropdown",
 "select","switch","toggle","checkbox","radio"}
ALIAS = {"btn":"button","nav":"menu","navbar":"menu","graph":"chart","price":"pricing","plan":"pricing",
 "metric":"stat","kpi":"stat","review":"testimonial","feature":"features","img":"image","photo":"image",
 "date":"calendar","console":"terminal","editor":"code","tag":"badge","pill":"badge","wave":"shader",
 "gradient":"shader","title":"text","heading":"text","carousel":"gallery","slider":"gallery","tab":"tabs"}


def category_map():
    m = collections.defaultdict(set)
    for pf in glob.glob(os.path.join(PAGES, "*.html")):
        cat = os.path.basename(pf)[:-5]
        html = open(pf, errors="ignore").read()
        for mt in re.finditer(r'cdn\.21st\.dev/([A-Za-z0-9._-]+)/([A-Za-z0-9._-]+)/code\.', html):
            m[f"{mt.group(1)}/{mt.group(2)}"].add(cat)
    return m


def slug_categories(slug):
    out = set()
    for t in set(slug.lower().replace("_", "-").split("-")):
        if t in VOCAB:
            out.add(ALIAS.get(t, t))
    return out


def build():
    catmap = category_map()
    attrs = {}
    for f in glob.glob(os.path.join(CODE, "*.tsx")):
        base = os.path.basename(f)[:-4]
        if "__" not in base:
            continue
        user, slug = base.split("__", 1)
        key = f"{user}/{slug}"
        src = open(f, errors="ignore").read()
        libs = set(re.findall(r"from ['\"]([^.'/][^'\"]*)['\"]", src))
        cats = set(catmap.get(key, set())) | slug_categories(slug)
        cats.discard("uncategorized")
        attrs[key] = {
            "category": sorted(cats) or ["uncategorized"],
            "lines": src.count("\n") + 1,
            "has_motion": bool({"framer-motion", "motion/react", "motion"} & libs),
            "icon_lib": "lucide-react" if "lucide-react" in libs else (
                "@hugeicons/react" if any("hugeicons" in l for l in libs) else None),
            "uses_cva": "class-variance-authority" in libs,
            "hardcoded_hex": len(set(re.findall(r"#[0-9a-fA-F]{3,6}", src))),
            "has_aria": "aria-" in src,
            "is_client": src[:60].lstrip().startswith(('"use client"', "'use client'")),
            "ext_libs": sorted(l for l in libs if not l.startswith("@/") and l != "react")[:6],
        }
    return attrs


def main():
    attrs = build()
    json.dump(attrs, open(OUT, "w"))
    # refresh the shipped sample: 40 components that carry a real (non-uncategorized) category
    sample = {}
    for k, v in attrs.items():
        if [c for c in v["category"] if c != "uncategorized"]:
            sample[k] = v
        if len(sample) >= 40:
            break
    json.dump(sample, open(SAMPLE, "w"), indent=0)
    cats = collections.Counter(c for a in attrs.values() for c in a["category"])
    print(f"attributes.json: {len(attrs)} components -> {OUT}")
    print(f"attributes.sample.json: {len(sample)} -> {SAMPLE}")
    print("top categories:", dict(cats.most_common(10)))
    print(f"motion={sum(1 for a in attrs.values() if a['has_motion'])} "
          f"hex={sum(1 for a in attrs.values() if a['hardcoded_hex'])} "
          f"aria={sum(1 for a in attrs.values() if a['has_aria'])} "
          f"uncategorized={cats['uncategorized']}")


if __name__ == "__main__":
    main()
