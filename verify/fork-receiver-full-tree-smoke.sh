#!/usr/bin/env bash
# verify/fork-receiver-full-tree-smoke.sh
# SP12: Fork-receiver smoke test for the FULL templates/ tree (L1+L2+L3+L4).
#
# Simulates a fresh fork receiver consuming all template layers:
#   1. Copies templates/L1+L2+L3+L4 to a temp dir (mirrors how a fork receiver
#      would lay them out alongside each other)
#   2. Static path-existence check — every relative import in L4/ resolves to
#      a real file within the copied tree (catches dead references)
#   3. No cross-domain L4 imports (each L4 domain only imports from L1/L2/L3)
#   4. Structural completeness — all required Next.js entry files present
#
# Why no full Next.js build?
#   A `npm install + next build` of a full L4 domain takes 3-5 minutes and
#   requires network access. The static checks here are sufficient to catch
#   the most common fork-receiver breakage: broken relative imports and
#   missing entry points.
#
# Exit 0 = all template layers are self-consistent and portable
# Exit 1 = any structural violation with specific error message
#
# Budget: 300s (static checks; no network / npm install)
# Usage: bash verify/fork-receiver-full-tree-smoke.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATES="$REPO_ROOT/templates"
SMOKE_DIR="$(mktemp -d /tmp/ax-full-tree-smoke-XXXXXX)"
TIMEOUT_SECS=300
START_TS=$(date +%s)

cleanup() { rm -rf "$SMOKE_DIR"; }
trap cleanup EXIT

echo "[SP12] Fork-receiver full-tree smoke: L1+L2+L3+L4 portability check"
echo "[SP12] Smoke dir: $SMOKE_DIR"

# ── Step 1: Copy all template layers ─────────────────────────────────────────
echo ""
echo "[1] Copying templates/L{1,2,3,4}/ → smoke dir..."

for layer in L1 L2 L3 L4; do
  if [ -d "$TEMPLATES/$layer" ]; then
    cp -r "$TEMPLATES/$layer" "$SMOKE_DIR/$layer"
    echo "  copied $layer/"
  else
    echo "FAIL: templates/$layer/ not found at $TEMPLATES/$layer" >&2
    exit 1
  fi
done

# ── Step 2: L4 directory structure completeness ───────────────────────────────
echo ""
echo "[2] Checking L4 domain structure completeness..."

STRUCTURE_FAIL=0
for domain in auth crud payment practices; do
  L4_DOMAIN="$SMOKE_DIR/L4/$domain"
  if [ ! -d "$L4_DOMAIN" ]; then
    echo "  MISSING: L4/$domain/ directory"
    STRUCTURE_FAIL=$((STRUCTURE_FAIL + 1))
    continue
  fi
  # Each L4 domain must have app/ directory and next.config.ts
  if [ ! -d "$L4_DOMAIN/app" ]; then
    echo "  MISSING: L4/$domain/app/"
    STRUCTURE_FAIL=$((STRUCTURE_FAIL + 1))
  fi
  if [ ! -f "$L4_DOMAIN/next.config.ts" ]; then
    echo "  MISSING: L4/$domain/next.config.ts"
    STRUCTURE_FAIL=$((STRUCTURE_FAIL + 1))
  fi
  if [ ! -f "$L4_DOMAIN/README.md" ]; then
    echo "  MISSING: L4/$domain/README.md"
    STRUCTURE_FAIL=$((STRUCTURE_FAIL + 1))
  fi
  echo "  OK: L4/$domain/ (app/, next.config.ts, README.md)"
done

if [ "$STRUCTURE_FAIL" -gt 0 ]; then
  echo ""
  echo "[SP12] FAIL: $STRUCTURE_FAIL L4 structure violation(s)"
  exit 1
fi

# ── Step 3: L4 → L2/L3 relative import resolution ────────────────────────────
echo ""
echo "[3] Checking relative import resolution (L4→L2/L3 paths exist in tree)..."

IMPORT_FAIL=0
IMPORT_CHECK=0

python3 - "$SMOKE_DIR" << 'PYEOF'
import sys, pathlib, re, os

smoke_dir = pathlib.Path(sys.argv[1]).resolve()
IMPORT_RE = re.compile(r"""from\s+['"]([^'"]+)['"]""")

violations = []
checked = 0

for domain in ['auth', 'crud', 'payment', 'practices']:
    l4_domain = smoke_dir / 'L4' / domain
    if not l4_domain.exists():
        continue
    for ts_file in sorted(l4_domain.rglob('*.tsx')) + sorted(l4_domain.rglob('*.ts')):
        try:
            text = ts_file.read_text(encoding='utf-8')
        except Exception:
            continue
        for import_path in IMPORT_RE.findall(text):
            # Only check relative imports that go up layers (../../../L2/...)
            if not import_path.startswith('.'):
                continue
            resolved = (ts_file.parent / import_path).resolve()
            # Check if it claims to be in L1/L2/L3
            for layer in ('L1', 'L2', 'L3'):
                layer_abs = (smoke_dir / layer).resolve()
                try:
                    resolved.relative_to(layer_abs)
                    checked += 1
                    # Does the target file exist? (try with .tsx, .ts, /index.tsx, /index.ts)
                    candidates = [
                        resolved,
                        resolved.with_suffix('.tsx'),
                        resolved.with_suffix('.ts'),
                        resolved / 'index.tsx',
                        resolved / 'index.ts',
                    ]
                    if not any(c.exists() for c in candidates):
                        rel_from = ts_file.relative_to(smoke_dir)
                        violations.append(f"BROKEN_IMPORT [{rel_from}]: '{import_path}' → {resolved.relative_to(smoke_dir)} (not found)")
                    break
                except ValueError:
                    pass

        # Cross-L4 check: no L4 file imports from other L4 domains
        for import_path in IMPORT_RE.findall(text):
            if not import_path.startswith('.'):
                continue
            resolved = (ts_file.parent / import_path).resolve()
            l4_abs = (smoke_dir / 'L4').resolve()
            try:
                rel = resolved.relative_to(l4_abs)
                first_part = str(rel).split(os.sep)[0]
                if first_part != domain:
                    violations.append(f"CROSS_L4_IMPORT [{ts_file.relative_to(smoke_dir)}]: imports from L4/{first_part}/")
            except ValueError:
                pass

if violations:
    for v in violations:
        print(f"  {v}")
    sys.exit(1)

print(f"  import resolution: {checked} cross-layer refs checked — all valid")
print(f"  cross-L4: NONE")
sys.exit(0)
PYEOF

if [ $? -ne 0 ]; then
  echo ""
  echo "[SP12] FAIL: relative import violations found"
  exit 1
fi

# ── Step 4: L1 path-leak scan (same as fork-receiver-smoke.sh) ───────────────
echo ""
echo "[4] L1 path-leak scan (imports must stay within L1/)..."

LEAK_COUNT=0
while IFS= read -r file; do
  while IFS= read -r import_path; do
    leak_result=$(python3 - "$file" "$import_path" "$SMOKE_DIR/L1" << 'PYEOF'
import os, sys
file_path, import_path, l1root = sys.argv[1:]
base = os.path.dirname(file_path)
resolved = os.path.normpath(os.path.join(base, import_path))
l1norm = os.path.normpath(l1root)
if not resolved.startswith(l1norm + os.sep) and resolved != l1norm:
    print("LEAK:" + resolved)
PYEOF
    )
    if [[ "$leak_result" == LEAK:* ]]; then
      echo "  PATH_LEAK: ${file#$SMOKE_DIR/} imports '$import_path' outside L1/"
      LEAK_COUNT=$((LEAK_COUNT + 1))
    fi
  done < <(grep -oE "from ['\"](\./|\.\./)([^'\"]+)['\"]" "$file" 2>/dev/null \
             | sed "s/^from ['\"]//;s/['\"]$//" || true)
done < <(find "$SMOKE_DIR/L1" \( -name "*.ts" -o -name "*.tsx" \) \
           ! -path "*/node_modules/*")

if [[ $LEAK_COUNT -gt 0 ]]; then
  echo ""
  echo "[SP12] FAIL: $LEAK_COUNT path leak(s) in L1/"
  exit 1
fi
echo "  L1 path-leaks: 0 — all relative imports stay within L1/"

# ── Step 5: Count summary ─────────────────────────────────────────────────────
L1_FILES=$(find "$SMOKE_DIR/L1" \( -name "*.ts" -o -name "*.tsx" \) | wc -l | tr -d ' ')
L2_FILES=$(find "$SMOKE_DIR/L2" \( -name "*.ts" -o -name "*.tsx" \) | wc -l | tr -d ' ')
L3_FILES=$(find "$SMOKE_DIR/L3" \( -name "*.ts" -o -name "*.tsx" \) | wc -l | tr -d ' ')
L4_FILES=$(find "$SMOKE_DIR/L4" \( -name "*.ts" -o -name "*.tsx" \) | wc -l | tr -d ' ')

END_TS=$(date +%s)
DURATION=$((END_TS - START_TS))

echo ""
echo "=== Full-tree smoke summary ==="
echo "  L1: $L1_FILES source files"
echo "  L2: $L2_FILES source files"
echo "  L3: $L3_FILES source files"
echo "  L4: $L4_FILES source files (4 domains)"
echo "  elapsed: ${DURATION}s / ${TIMEOUT_SECS}s budget"
echo ""
echo "fork.full_tree.smoke.duration=${DURATION}s"
echo "fork.full_tree.smoke.l4.domains=4 (auth, crud, payment, practices)"
echo "[SP12] PASS: fork-receiver full-tree smoke exits 0 (${DURATION}s total)"
