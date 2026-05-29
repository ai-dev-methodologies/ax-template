#!/usr/bin/env bash
# practices/evals/name_collision_guard.sh — IMW2-C cross-package name-collision guard.
#
# THE GAP THIS CLOSES (IDW2 dogfood 2026-05-29)
# ---------------------------------------------
# A new domain that reuses a generic simple name — e.g. selleradmin.Product
# alongside the existing ecommerce.Product, or a second ProductService /
# ProductRepository in a different package — breaks Spring context boot:
#   * two @Entity classes mapping the same JPA entity name      →
#       org.hibernate.DuplicateMappingException
#   * two @Service / @Repository / @Controller beans inferred to the same
#     default bean name (the de-capitalised simple class name) →
#       org.springframework.context.annotation.ConflictingBeanDefinitionException
# This is a RUNTIME-ONLY failure. ddl-auto regenerates the schema at boot and
# bean names are inferred at boot, so the break never surfaces until the
# ApplicationContext actually starts. NONE of the existing static guards parse
# class-level stereotype annotations, so the deviation predicted nothing — all
# three IDW2 personas hit the ConflictingBeanDefinitionException / Hibernate
# DuplicateMappingException at runtime.
#
# WHAT THIS GUARD ENFORCES
# ------------------------
# Scan every *.java under
#   backend/src/main/java/com/ax/template/authblueprint/**
# for classes annotated with a class-level stereotype:
#   @Entity                                            (JPA entity name)
#   @Service / @Repository / @Component                (Spring bean)
#   @RestController / @Controller                      (Spring controller bean)
# and FAIL when two classes in DIFFERENT packages resolve to the SAME effective
# registration name for the SAME kind. A class's effective name is its explicit
# override if it declares one:
#   @Entity(name = "...")                    for the entity kind
#   @Service("...") / @Repository("...") /
#   @Component("...") / @Controller("...") /
#   @RestController("...")                   for the bean kinds
# otherwise it is the inferred default (Hibernate's entity name == the simple
# class name; Spring's bean name == the de-capitalised simple class name). An
# explicit unique name= / bean-name string is exactly how Spring + Hibernate let
# two same-simple-name classes coexist, so giving ONE of the two copies a
# distinct override resolves the collision and the guard passes. (An override
# whose value equals the other copy's effective name is still a collision.)
#
# KIND GROUPING
# -------------
#   entity      ← @Entity                  (Hibernate entity-name namespace)
#   bean        ← @Service @Repository @Component
#                                          (Spring singleton bean-name namespace)
#   controller  ← @RestController @Controller
#                                          (also Spring beans, but grouped on
#                                           their own so the message is precise)
# @Service/@Repository/@Component share ONE bean-name namespace in Spring, so a
# ProductService in two packages collides regardless of which of the three
# stereotypes each uses. They are grouped together as the "bean" kind.
#
# SUBSTRING TRAPS DELIBERATELY AVOIDED
# ------------------------------------
#   * @EntityListeners / @EntityGraph        — not the @Entity stereotype.
#   * {@code @Component("x")} inside javadoc  — comments are stripped before
#     parsing, so documentation examples never register a class.
#   * @Component on a non-class (rare)        — only the class/interface/enum
#     declaration that the annotation block precedes is registered.
#
# CALIBRATION (current tree, 2026-05-29)
# --------------------------------------
# Recon confirmed ZERO cross-package simple-name collisions for any kind
# (Product lives only in ecommerce/; no duplicate Service/Repository/Controller
# simple names; the one explicit bean name @Component("rawPaymentProvider") in
# payment/MockProvider is unique). This guard therefore exits 0 on the current
# tree. Introduce a second-package Product / ProductService / ProductController
# without a name override and it exits 1, naming the kind + simple name + both
# colliding packages — catching the deviation before context boot does.
#
# Exit codes:
#   0 — no cross-package same-kind same-simple-name collisions (or each is
#       resolved by an explicit name override).
#   1 — at least one unresolved collision (each printed: kind, simple name,
#       colliding packages).
#   2 — usage / environment error (paths missing, python3 missing).
#
# Usage:
#   bash practices/evals/name_collision_guard.sh
#   bash practices/evals/name_collision_guard.sh --root DIR
#   bash practices/evals/name_collision_guard.sh --verbose
#
# Bash 3.2 compatible. Fast: pure file scan via python3 (a repo dependency). No gradle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
VERBOSE=0

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        --verbose|-v) VERBOSE=1; shift ;;
        *) echo "name_collision_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$REPO_ROOT" ]; then
    echo "name_collision_guard: --root '$REPO_ROOT' is not a directory" >&2
    exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
    echo "name_collision_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

SCAN_DIR="$REPO_ROOT/backend/src/main/java/com/ax/template/authblueprint"
if [ ! -d "$SCAN_DIR" ]; then
    # No backend Java tree → nothing to scan. Not an error (a fork-receiver may
    # have a frontend-only or as-yet-unpopulated workload).
    echo "name_collision_guard: no backend source tree at $SCAN_DIR — nothing to check"
    exit 0
fi

python3 - "$SCAN_DIR" "$VERBOSE" <<'PYEOF'
import pathlib
import re
import sys

scan_dir = pathlib.Path(sys.argv[1])
verbose = sys.argv[2] == "1"

# --- comment stripping -----------------------------------------------------
# Remove /* ... */ block comments and // line comments so that javadoc
# examples like {@code @Component("x")} never register a phantom class.
# String literals are NOT specially handled: a stereotype annotation never
# legitimately lives inside a string literal in production code, and removing
# comments is sufficient to eliminate the documented false-positive class.
BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.S)
LINE_COMMENT_RE = re.compile(r"//[^\n]*")

def strip_comments(text: str) -> str:
    text = BLOCK_COMMENT_RE.sub(lambda m: "\n" * m.group(0).count("\n"), text)
    text = LINE_COMMENT_RE.sub("", text)
    return text

# --- stereotype annotation matchers ----------------------------------------
# Anchored at start-of-line (after optional whitespace), matched as a WHOLE TOKEN
# via a trailing word boundary (\b). The \b still excludes @EntityListeners /
# @EntityGraph (no boundary between "Entity" and "Graph"/"Listeners") and keeps
# @Controller distinct from @RestController (the line must START with @Controller).
# IMW3-followup (IDW3 G4 audit): the previous anchor `@<name>\s*(\(|$)` matched only
# own-line or `@<name>(` forms, so an INLINE pair like `@Service @Transactional`
# (the stereotype followed by a space + another annotation on one line) ESCAPED
# detection — the exact false-negative class fixed in entity_migration_guard.
# Empirically reproduced: two packages each with an inline `@Service @Transactional`
# FooService used to PASS (collision missed). The \b form now detects them.
def stereotype_re(name: str) -> re.Pattern:
    return re.compile(r"^[ \t]*@" + name + r"\b", re.M)

ENTITY_RE = stereotype_re("Entity")
SERVICE_RE = stereotype_re("Service")
REPOSITORY_RE = stereotype_re("Repository")
COMPONENT_RE = stereotype_re("Component")
RESTCONTROLLER_RE = stereotype_re("RestController")
CONTROLLER_RE = stereotype_re("Controller")

# First type declaration (class/interface/enum) in the (comment-stripped) file.
TYPE_DECL_RE = re.compile(
    r"\b(?:public\s+|final\s+|abstract\s+)*(?:class|interface|enum)\s+(\w+)"
)

# Explicit name override extractors.
#   @Entity(name = "foo")            → entity name override
#   @Service("foo") / @Repository("foo") / @Component("foo") /
#   @Controller("foo") / @RestController("foo")  → bean name override
ENTITY_NAME_RE = re.compile(r'@Entity\s*\(\s*name\s*=\s*"([^"]+)"')
BEAN_NAME_RE = re.compile(
    r'@(?:Service|Repository|Component|Controller|RestController)\s*\(\s*"([^"]+)"'
)


def detect_kind(text: str):
    """Return the stereotype kind for a class, or None.

    Priority: entity, then controller (Rest/plain), then bean. A controller is
    grouped separately from plain beans purely for message precision; both live
    in Spring's bean-name namespace.
    """
    if ENTITY_RE.search(text):
        return "entity"
    if RESTCONTROLLER_RE.search(text) or CONTROLLER_RE.search(text):
        return "controller"
    if SERVICE_RE.search(text) or REPOSITORY_RE.search(text) or COMPONENT_RE.search(text):
        return "bean"
    return None


def explicit_name(text: str, kind: str):
    """The explicit name override declared on the class, or None.

    @Entity(name = "foo")  → "foo" (entity kind);  @Service("foo") and the
    other bean stereotypes → "foo" (bean / controller kinds). None means the
    class uses Spring's / Hibernate's inferred default name.
    """
    if kind == "entity":
        m = ENTITY_NAME_RE.search(text)
    else:
        m = BEAN_NAME_RE.search(text)
    return m.group(1) if m else None


# --- collect annotated classes ---------------------------------------------
# registry[kind][simple_name] = list of (package, relpath, explicit_override|None)
registry = {"entity": {}, "controller": {}, "bean": {}}
scanned = 0
registered = 0

for jf in sorted(scan_dir.rglob("*.java")):
    scanned += 1
    raw = jf.read_text(encoding="utf-8", errors="replace")
    text = strip_comments(raw)

    kind = detect_kind(text)
    if kind is None:
        continue

    decl = TYPE_DECL_RE.search(text)
    if not decl:
        continue
    simple = decl.group(1)

    # package = directory of the file relative to the scan root, dotted. The
    # top-level scan dir itself is the root package (empty → "<root>").
    rel = jf.relative_to(scan_dir)
    pkg = ".".join(rel.parent.parts) if rel.parent.parts else "<root>"

    override = explicit_name(text, kind)
    registry[kind].setdefault(simple, []).append((pkg, rel.as_posix(), override))
    registered += 1
    if verbose:
        ex = f" [name=\"{override}\"]" if override else ""
        print(f"  {kind:10s} {simple:32s} {pkg}{ex}")

# --- evaluate collisions ----------------------------------------------------
# Spring/Hibernate break at boot only when two classes resolve to the SAME
# effective name in the SAME namespace. The effective name of a class is its
# explicit override if present, else the inferred default (the simple class
# name — Hibernate's default entity name, and the de-capitalised simple name is
# Spring's default bean name; either way two same-simple-name classes collide
# on the default). So: an override RESOLVES the collision by making the
# effective names distinct — even if only ONE of the two classes carries it.
# We therefore flag a (kind, simple_name) group only when 2+ classes in
# DIFFERENT packages still share the same effective name.
# (Same package + same simple name is a Java compile error, never two files.)
KIND_LABEL = {
    "entity": "@Entity (Hibernate entity-name)",
    "controller": "@RestController/@Controller (Spring bean)",
    "bean": "@Service/@Repository/@Component (Spring bean)",
}


def default_name(kind: str, simple: str) -> str:
    """The inferred default name a class registers under when it has NO override.

    * entity kind  → Hibernate's default entity name == the simple class name
      verbatim.
    * bean/controller kind → Spring's AnnotationBeanNameGenerator default ==
      java.beans.Introspector.decapitalize(simple): lowercase the first letter
      UNLESS the first two letters are both uppercase (then leave unchanged, so
      "URLService" stays "URLService"). Matching this exactly lets us catch the
      case where an explicit @Component("productService") collides with another
      class's INFERRED default "productService".
    """
    if kind == "entity":
        return simple
    if len(simple) > 1 and simple[0].isupper() and simple[1].isupper():
        return simple
    return simple[:1].lower() + simple[1:]


collisions = []  # (kind, simple, [(pkg, rel, override), ...])
for kind, by_name in registry.items():
    for simple, entries in by_name.items():
        if len({e[0] for e in entries}) < 2:
            continue  # all in one package → no cross-package collision
        # Effective name per class: the override string if present, else the
        # kind-correct inferred default. Two classes break boot only when their
        # effective names are byte-identical. Group by effective name.
        eff = {}  # effective_name -> [entries]
        for e in entries:
            pkg, rel, override = e
            name = override if override else default_name(kind, simple)
            eff.setdefault(name, []).append(e)
        for name, group in eff.items():
            if len({g[0] for g in group}) >= 2:
                # 2+ classes in different packages share this effective name.
                collisions.append((kind, simple, group))
            elif verbose:
                print(
                    f"  resolved-by-override: {kind} '{simple}' — effective "
                    f"name '{name}' is unique across packages"
                )

if collisions:
    print(
        "VIOLATION: cross-package simple-name collisions that break Spring "
        "context boot (ConflictingBeanDefinitionException / Hibernate "
        "DuplicateMappingException) — IMW2-C:",
        file=sys.stderr,
    )
    for kind, simple, entries in sorted(collisions, key=lambda c: (c[0], c[1])):
        pkgs = ", ".join(sorted(e[0] for e in entries))
        print(
            f"  [{KIND_LABEL[kind]}] simple name '{simple}' declared in "
            f"DIFFERENT packages: {pkgs}",
            file=sys.stderr,
        )
        for pkg, rel, override in sorted(entries):
            tag = f'  (effective name "{override}")' if override else "  (default name)"
            print(f"      - {scan_dir.as_posix()}/{rel}{tag}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Fix policy: rename one of the colliding classes to a domain-prefixed "
        "simple name (e.g. SellerProduct instead of a second Product), OR give "
        "at least one of them an explicit unique name override — "
        '@Entity(name = "seller_product") for entities, or @Service("...") / '
        '@Repository("...") / @Component("...") / @Controller("...") / '
        '@RestController("...") for beans.',
        file=sys.stderr,
    )
    print(
        f"name_collision_guard: {len(collisions)} unresolved cross-package "
        "name collision(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"name_collision_guard: PASS — no cross-package same-kind name collisions "
    f"({registered} stereotype-annotated class(es) across {scanned} .java file(s))"
)
sys.exit(0)
PYEOF
