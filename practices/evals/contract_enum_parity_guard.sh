#!/usr/bin/env bash
# practices/evals/contract_enum_parity_guard.sh
# P2-33 — contract↔code enum parity, EXHAUSTIVE BY CONSTRUCTION.
#
# THE INVARIANT: an OpenAPI `enum:` block is a promise about the bytes on the
# wire. Nothing mechanically bound those promises to the Java enums that actually
# serialize/deserialize them, so a contract could advertise a vocabulary the code
# can never emit (client switch-cases a dead branch) or omit one the code DOES
# emit (a strict client rejects a real response). P2-34 is the same defect class
# one layer out: an L4 fork-copy carrying a vocabulary that is not the canonical
# one at all.
#
# TWO ENTRY KINDS (`practices/evals/contract-enum-map.yaml`):
#
#   kind: contract_enum   — one `enum:` block, addressed by contract path +
#                           RFC-6901 JSON pointer. EXACTLY ONE of:
#                             java_enum: <FQCN>   — the block is bound to a Java
#                                                   enum; constant sets must match.
#                             wire_only: <reason> — no Java enum backs the block
#                                                   (free-form string on the code
#                                                   side); reason is mandatory.
#                           Modifiers on a `java_enum` entry (each needs `reason`):
#                             wire_extra:   [T…]  — tokens on the wire that are NOT
#                                                   Java constants (e.g. the `ALL`
#                                                   filter sentinel).
#                             wire_missing: [T…]  — Java constants deliberately not
#                                                   offered in THIS block (e.g. a
#                                                   creation ack that can only be
#                                                   PENDING).
#                             wire_case: lower    — the wire spells the constants in
#                                                   lower case (compared fold-wise).
#                           Both modifier lists are checked for NON-REDUNDANCY: a
#                           listed token that is not actually in the corresponding
#                           difference FAILS. A stale allowance cannot rot silently.
#
#   kind: vocab_scan      — surfaces the contract_enum schema cannot express (an L4
#                           fork-copy's TS union, a java skeleton, a README table).
#                             file:        <path>
#                             canonical:   [T…]      the legal vocabulary
#                             require_all: true      every canonical token must appear
#                             forbidden:   [T…]      none of these may appear
#                           Matching is WORD-BOUNDARY token grep — `SUCCESS` does not
#                           match inside `SUCCEEDED`, `FAILED` does not match inside
#                           `FAILED_PERMANENT`.
#
# EXHAUSTIVENESS (the property that makes this non-heuristic): every `enum:` block
# found under `contracts/*.yaml` MUST appear in the manifest. An unclassified block
# FAILS — there is no name inference, no "looks like" matching, no default. The
# reverse also FAILS: a manifest entry addressing a block that no longer exists is a
# stale entry.
#
# NON-VACUITY: zero discovered blocks, zero contract_enum entries, or zero
# vocab_scan entries all FAIL — the gate cannot be emptied into a silent pass.
#
# Exit: 0 PASS · 1 violation · 2 usage/parse error.
#
# Usage:
#   bash practices/evals/contract_enum_parity_guard.sh
#   bash practices/evals/contract_enum_parity_guard.sh --root DIR
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        --repo-root) REPO_ROOT="$2"; shift 2 ;;
        --repo-root=*) REPO_ROOT="${1#--repo-root=}"; shift ;;
        *) echo "contract_enum_parity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

python3 - "$REPO_ROOT" <<'PY'
import sys, os, re, glob

repo = sys.argv[1]
MANIFEST = os.path.join(repo, 'practices', 'evals', 'contract-enum-map.yaml')

try:
    import yaml
except ImportError:
    print("contract_enum_parity: PyYAML is required (R25 toolchain preflight)", file=sys.stderr)
    sys.exit(2)

# ── 1. discover every enum: block in contracts/*.yaml ────────────────────────
def esc(tok):
    return str(tok).replace('~', '~0').replace('/', '~1')

def walk(node, path, out):
    if isinstance(node, dict):
        for k, v in node.items():
            if k == 'enum' and isinstance(v, list):
                out.append((path, [str(x) for x in v]))
            else:
                walk(v, path + '/' + esc(k), out)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            walk(v, path + '/' + str(i), out)

discovered = {}   # (relpath, pointer) -> [tokens]
for f in sorted(glob.glob(os.path.join(repo, 'contracts', '*.yaml'))):
    rel = os.path.relpath(f, repo)
    try:
        doc = yaml.safe_load(open(f, encoding='utf-8'))
    except Exception as ex:
        print(f"FAIL: {rel} is not parseable YAML: {ex}")
        sys.exit(1)
    found = []
    walk(doc, '', found)
    for ptr, toks in found:
        discovered[(rel, ptr)] = toks

# ── 2. java enum constant extraction (grep/parse — no JVM) ───────────────────
def strip_comments(s):
    out = []; i = 0; n = len(s)
    while i < n:
        c = s[i]
        if c == '/' and i + 1 < n and s[i+1] == '*':
            j = s.find('*/', i + 2); i = n if j < 0 else j + 2; continue
        if c == '/' and i + 1 < n and s[i+1] == '/':
            j = s.find('\n', i); i = n if j < 0 else j + 1; out.append('\n'); continue
        if c in '"\'':
            q = c; out.append(c); i += 1
            while i < n:
                out.append(s[i])
                if s[i] == '\\':
                    out.append(s[i+1] if i + 1 < n else ''); i += 2; continue
                if s[i] == q:
                    i += 1; break
                i += 1
            continue
        out.append(c); i += 1
    return ''.join(out)

def split_top(s):
    parts = []; depth = 0; cur = []
    for ch in s:
        if ch in '([{': depth += 1
        elif ch in ')]}': depth -= 1
        if ch == ',' and depth == 0:
            parts.append(''.join(cur)); cur = []
        else:
            cur.append(ch)
    parts.append(''.join(cur))
    return parts

def enums_in_source(text):
    """(simple-name, [constants]) for every enum declared in a java source.

    Handles the three shapes that occur in this tree:
      plain              `public enum WebhookDeliveryStatus { A, /** doc */ B }`
      constructor-arg    `CSV("text/csv", ".csv"), XLSX(...);`
      nested-in-type     `public class X { public enum Status { … } }`
    """
    src = strip_comments(text)
    res = []
    for m in re.finditer(r'\benum\s+(\w+)\s*(?:implements\s+[\w.<>,\s]+?)?\{', src):
        name = m.group(1)
        i = src.index('{', m.end() - 1)
        depth = 0; j = i
        for j in range(i, len(src)):
            if src[j] == '{': depth += 1
            elif src[j] == '}':
                depth -= 1
                if depth == 0: break
        body = src[i+1:j]
        head = body.split(';')[0] if ';' in body else body
        consts = []
        for part in split_top(head):
            t = part.strip()
            if not t: continue
            mm = re.match(r'^(?:@\w+(?:\([^)]*\))?\s*)*([A-Za-z_$][\w$]*)', t)
            if mm and re.fullmatch(r'[A-Z][A-Z0-9_]*', mm.group(1)):
                consts.append(mm.group(1))
        res.append((name, consts))
    return res

def scan_java(root):
    enums = {}
    for f in glob.glob(os.path.join(root, 'backend/src/main/java/**/*.java'), recursive=True):
        text = open(f, encoding='utf-8', errors='ignore').read()
        pm = re.search(r'^\s*package\s+([\w.]+);', strip_comments(text), re.M)
        pkg = pm.group(1) if pm else ''
        outer = os.path.basename(f)[:-5]
        for name, consts in enums_in_source(text):
            fq = f"{pkg}.{name}" if name == outer else f"{pkg}.{outer}.{name}"
            enums[fq] = consts
    return enums

java_enums = scan_java(repo)

# ── 3. manifest ──────────────────────────────────────────────────────────────
if not os.path.exists(MANIFEST):
    print(f"FAIL: manifest not found: {os.path.relpath(MANIFEST, repo)}")
    sys.exit(1)
manifest = yaml.safe_load(open(MANIFEST, encoding='utf-8')) or {}
entries = manifest.get('contract_enums') or []
scans = manifest.get('vocab_scans') or []

violations = []

# ── 4. exhaustiveness, both directions ───────────────────────────────────────
claimed = {}
for idx, e in enumerate(entries):
    key = (e.get('contract'), e.get('pointer'))
    if key in claimed:
        violations.append(f"manifest: duplicate entry for {key[0]}#{key[1]}")
    claimed[key] = e

for key in sorted(discovered):
    if key not in claimed:
        toks = discovered[key]
        violations.append(
            f"UNCLASSIFIED enum block {key[0]}#{key[1]} = {toks} — every enum: block in "
            f"contracts/ MUST be classified in practices/evals/contract-enum-map.yaml "
            f"(java_enum: <FQCN> | wire_only: <reason>)")
for key in sorted(claimed):
    if key not in discovered:
        violations.append(
            f"STALE manifest entry {key[0]}#{key[1]} — no such enum: block on disk "
            f"(the contract moved or was edited; re-point or delete the entry)")

# ── 5. per-entry parity ──────────────────────────────────────────────────────
def as_list(v):
    return list(v) if isinstance(v, list) else []

for key in sorted(claimed):
    if key not in discovered:
        continue
    e = claimed[key]
    where = f"{key[0]}#{key[1]}"
    kind = e.get('kind', 'contract_enum')
    if kind != 'contract_enum':
        violations.append(f"{where}: kind must be contract_enum (got {kind!r})")
        continue
    has_java = 'java_enum' in e
    has_wire_only = 'wire_only' in e
    if has_java == has_wire_only:
        violations.append(
            f"{where}: exactly ONE of java_enum / wire_only is required "
            f"(java_enum={has_java}, wire_only={has_wire_only})")
        continue
    if has_wire_only:
        if not str(e.get('wire_only') or '').strip():
            violations.append(f"{where}: wire_only requires a non-empty reason")
        for mod in ('wire_extra', 'wire_missing', 'wire_case'):
            if mod in e:
                violations.append(f"{where}: {mod} is only valid on a java_enum entry")
        continue

    fq = e['java_enum']
    if fq not in java_enums:
        violations.append(
            f"{where}: java_enum {fq} not found in backend/src/main/java "
            f"(renamed/moved/deleted?)")
        continue
    java_set = set(java_enums[fq])
    wire_raw = discovered[key]
    lower = str(e.get('wire_case', '')).lower() == 'lower'
    if lower:
        bad = [t for t in wire_raw if t != t.lower()]
        if bad:
            violations.append(f"{where}: wire_case: lower declared but {bad} are not lower-case")
        wire_set = {t.upper() for t in wire_raw}
    else:
        bad = [t for t in wire_raw if t != t.upper()]
        if bad:
            violations.append(
                f"{where}: wire tokens {bad} are not upper-case — a Java enum serializes "
                f"as name(); declare `wire_case: lower` + reason if the wire really folds case")
        wire_set = set(wire_raw)

    extra = set(as_list(e.get('wire_extra')))
    missing = set(as_list(e.get('wire_missing')))
    if (extra or missing or 'wire_case' in e) and not str(e.get('reason') or '').strip():
        violations.append(f"{where}: wire_extra / wire_missing / wire_case require a `reason:`")

    real_extra = wire_set - java_set
    real_missing = java_set - wire_set
    stale_extra = extra - real_extra
    stale_missing = missing - real_missing
    if stale_extra:
        violations.append(
            f"{where}: wire_extra lists {sorted(stale_extra)} which ARE Java constants — "
            f"stale allowance, delete it")
    if stale_missing:
        violations.append(
            f"{where}: wire_missing lists {sorted(stale_missing)} which ARE on the wire — "
            f"stale allowance, delete it")
    undeclared_extra = real_extra - extra
    undeclared_missing = real_missing - missing
    if undeclared_extra or undeclared_missing:
        parts = []
        if undeclared_extra:
            parts.append(f"wire declares {sorted(undeclared_extra)} which {fq} cannot emit")
        if undeclared_missing:
            parts.append(f"{fq} can emit {sorted(undeclared_missing)} which the wire forbids")
        violations.append(
            f"{where}: ENUM DRIFT vs {fq} — " + "; ".join(parts) +
            " — fix the contract, fix the enum, or declare wire_extra/wire_missing + reason")

# ── 6. vocab_scan ────────────────────────────────────────────────────────────
for s in scans:
    kind = s.get('kind', 'vocab_scan')
    rel = s.get('file')
    if kind != 'vocab_scan':
        violations.append(f"vocab_scans: entry {rel!r} has kind {kind!r}, expected vocab_scan")
        continue
    path = os.path.join(repo, rel or '')
    if not rel or not os.path.isfile(path):
        violations.append(f"vocab_scan: file not found: {rel}")
        continue
    body = open(path, encoding='utf-8', errors='ignore').read()
    tokens = set(re.findall(r'[A-Za-z_][A-Za-z0-9_]*', body))
    canonical = as_list(s.get('canonical'))
    forbidden = as_list(s.get('forbidden'))
    if not canonical:
        violations.append(f"vocab_scan {rel}: `canonical` must be a non-empty list")
    hits = [t for t in forbidden if t in tokens]
    if hits:
        violations.append(
            f"vocab_scan {rel}: forbidden token(s) present: {hits} — this surface must speak "
            f"the canonical vocabulary {canonical}")
    if s.get('require_all'):
        absent = [t for t in canonical if t not in tokens]
        if absent:
            violations.append(
                f"vocab_scan {rel}: require_all — canonical token(s) missing: {absent}")

# ── 7. non-vacuity ───────────────────────────────────────────────────────────
if not discovered:
    violations.append("ZERO_SCAN — no enum: block found under contracts/*.yaml; the gate would be vacuous")
if not [e for e in entries if 'java_enum' in e]:
    violations.append("ZERO_BINDING — no contract_enum entry binds a java_enum; the gate would be vacuous")
if not scans:
    violations.append("ZERO_VOCAB_SCAN — no vocab_scan entry; the L4 vocabulary axis would be unguarded")

print(f"[contract_enum_parity] {len(discovered)} enum block(s) across "
      f"{len({k[0] for k in discovered})} contract file(s); "
      f"{len([e for e in entries if 'java_enum' in e])} java-bound, "
      f"{len([e for e in entries if 'wire_only' in e])} wire-only; "
      f"{len(scans)} vocab_scan surface(s); "
      f"{len(java_enums)} java enum(s) indexed")

if violations:
    print(f"FAIL: {len(violations)} contract↔code enum parity violation(s):")
    for v in violations:
        print(f"  {v}")
    sys.exit(1)

print("PASS — every contract enum block is classified and every bound block matches its Java enum")
PY
rc=$?
exit $rc
