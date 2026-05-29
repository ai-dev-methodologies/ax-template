#!/usr/bin/env bash
# practices/evals/entity_migration_guard.sh
# IMW1-C (IDW1 dogfood 2026-05-29) — entity↔migration drift guard.
#
# THE GAP THIS CLOSES
# -------------------
# backend/src/main/resources/application.yml sets `ddl-auto: create-drop`, so
# Hibernate regenerates the H2 reference schema from the @Entity classes on
# every boot. The Flyway V###__*.sql migrations are therefore NEVER exercised
# by the test suite. Consequence: an @Entity can ship with NO migration, and
# entity↔migration drift is completely invisible to all 49 existing guards
# (none of them parse SQL). The IDW1 dogfood found exactly this slipping past.
#
# WHAT THIS GUARD ENFORCES
# ------------------------
# For every real @Entity class under
#   backend/src/main/java/com/ax/template/authblueprint/<domain>/
# there MUST be at least one
#   backend/src/main/resources/db/migration/V*.sql
# whose DDL creates (or alters) the entity's table.
#
# The entity's table name is resolved as:
#   1. the @Table(name = "...") value (single-line OR multi-line @Table(...)), else
#   2. the snake_case of the class name (Hibernate's implicit naming strategy).
#
# COVERAGE PREDICATE (deliberately strict — NO substring false-positives)
# -----------------------------------------------------------------------
# A table counts as backed ONLY when it appears as the target of a
#   CREATE TABLE [IF NOT EXISTS] <table>     or
#   ALTER  TABLE                 <table>
# statement (case-insensitive, word-boundary table name). A bare substring
# match would falsely PASS:
#   * `items`  — appears only in a V015 *comment* ("sum(items.unit_price ...)"),
#                while crud/ItemEntity declares @Table(name="items") with no
#                real migration. `items` is also a substring of `cart_items` /
#                `order_items`.
#   * `parent` — appears only in a V025 *comment* about FK design, while
#                practices/Parent has no migration.
# Both would be hidden by a lenient predicate. The CREATE/ALTER TABLE anchor
# eliminates that whole false-positive class.
#
# ALLOWLIST
# ---------
# practices/evals/.entity-migration-allowlist.txt holds entity table names
# that are intentionally migration-free (e.g. pure JPA-pattern demo entities
# that are never persisted to a migrated schema). One table name per line;
# blank lines and lines starting with '#' are ignored. The file ships EMPTY
# so the guard exempts nothing silently — the integrator adds entries with a
# justification comment when they consciously decide an entity needs no
# migration.
#
# CALIBRATION (current tree, 2026-05-29)
# --------------------------------------
# 48 real @Entity classes; 40 are backed by a CREATE TABLE migration.
# 8 lack any CREATE/ALTER TABLE migration:
#   refresh_tokens (auth/RefreshToken)        verification_tokens (auth/VerificationToken)
#   provider_links (user/ProviderLink)        users (user/UserEntity)
#   items (crud/ItemEntity)                   child (practices/Child)
#   parent (practices/Parent)                 versioned_account (practices/VersionedAccount)
# With an empty allowlist this guard exits 1 on the current tree and names
# those 8 — they are genuine entity↔migration drift, NOT false positives.
#
# Exit codes:
#   0 — every @Entity has a CREATE/ALTER TABLE migration (or is allowlisted).
#   1 — at least one @Entity lacks a backing migration.
#   2 — usage / environment error (paths missing, python3 missing).
#
# Usage:
#   bash practices/evals/entity_migration_guard.sh
#   bash practices/evals/entity_migration_guard.sh --root DIR
#
# Bash 3.2 compatible. Fast: pure file scan, no gradle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "entity_migration_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "entity_migration_guard: cannot cd to $REPO_ROOT" >&2; exit 2; }

ENTITY_DIR="backend/src/main/java/com/ax/template/authblueprint"
MIG_DIR="backend/src/main/resources/db/migration"
ALLOWLIST="practices/evals/.entity-migration-allowlist.txt"

# Missing source trees are an environment/usage error, not a silent pass:
# this guard exists precisely because these trees must stay in sync.
if [ ! -d "$ENTITY_DIR" ]; then
    echo "entity_migration_guard: missing entity dir $ENTITY_DIR" >&2
    exit 2
fi
if [ ! -d "$MIG_DIR" ]; then
    echo "entity_migration_guard: missing migration dir $MIG_DIR" >&2
    exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
    echo "entity_migration_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

python3 - "$ENTITY_DIR" "$MIG_DIR" "$ALLOWLIST" <<'PY'
import pathlib
import re
import sys

entity_dir = pathlib.Path(sys.argv[1])
mig_dir = pathlib.Path(sys.argv[2])
allowlist_path = pathlib.Path(sys.argv[3])

# --- helpers ---------------------------------------------------------------
def camel_to_snake(name: str) -> str:
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()

# A real @Entity is a line that is exactly `@Entity` or `@Entity(`.
# This deliberately excludes @EntityGraph / @EntityListeners (substring traps).
ENTITY_RE = re.compile(r"^\s*@Entity\s*(\(|$)", re.M)
TABLE_OPEN_RE = re.compile(r"@Table\s*\(")
NAME_RE = re.compile(r'name\s*=\s*"([^"]+)"')
CLASS_RE = re.compile(r"\b(?:public\s+)?(?:abstract\s+)?class\s+(\w+)")

# --- allowlist (intentionally migration-free tables) -----------------------
allow = set()
if allowlist_path.exists():
    for line in allowlist_path.read_text().splitlines():
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        allow.add(s.lower())

# --- build DDL target set: CREATE TABLE [IF NOT EXISTS] / ALTER TABLE -------
ddl_tables = set()
create_re = re.compile(
    r"create\s+table\s+(?:if\s+not\s+exists\s+)?[\"`]?(\w+)", re.I
)
alter_re = re.compile(r"alter\s+table\s+[\"`]?(\w+)", re.I)
for sql in sorted(mig_dir.glob("V*.sql")):
    txt = sql.read_text()
    for m in create_re.finditer(txt):
        ddl_tables.add(m.group(1).lower())
    for m in alter_re.finditer(txt):
        ddl_tables.add(m.group(1).lower())

# --- collect entities + resolve table name ---------------------------------
entities = []  # (relpath, table)
for jf in sorted(entity_dir.rglob("*.java")):
    txt = jf.read_text()
    if not ENTITY_RE.search(txt):
        continue
    table = None
    tm = TABLE_OPEN_RE.search(txt)
    if tm:
        nm = NAME_RE.search(txt, tm.end())
        if nm:
            table = nm.group(1)
    if not table:
        cm = CLASS_RE.search(txt)
        cls = cm.group(1) if cm else jf.stem
        table = camel_to_snake(cls)
    entities.append((jf.relative_to(entity_dir).as_posix(), table))

# --- evaluate coverage -----------------------------------------------------
total = len(entities)
backed = 0
allowlisted = 0
violations = []
for rel, table in entities:
    t = table.lower()
    if t in ddl_tables:
        backed += 1
    elif t in allow:
        allowlisted += 1
    else:
        violations.append((rel, table))

if violations:
    print(
        "VIOLATION: @Entity classes with NO CREATE/ALTER TABLE migration "
        "(entity↔migration drift, IMW1-C):",
        file=sys.stderr,
    )
    for rel, table in violations:
        print(
            f"  {entity_dir.as_posix()}/{rel}  ->  table '{table}' "
            f"has no V*.sql migration",
            file=sys.stderr,
        )
    print("", file=sys.stderr)
    print(
        "Fix policy: add a backend/src/main/resources/db/migration/V*.sql that "
        "CREATE TABLEs the missing table, OR — if the entity is intentionally "
        "migration-free (e.g. a JPA-pattern demo entity that is never persisted "
        "to a migrated schema) — add its table name with a justification to "
        f"{allowlist_path.as_posix()}.",
        file=sys.stderr,
    )
    print(
        f"entity_migration_guard: {len(violations)} of {total} entities lack a "
        "migration — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"entity_migration_guard: PASS — {backed}/{total} entities have a "
    f"CREATE/ALTER TABLE migration"
    + (f" ({allowlisted} allowlisted)" if allowlisted else "")
)
sys.exit(0)
PY
