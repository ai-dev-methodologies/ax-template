#!/usr/bin/env bash
# verify/fork-receiver-smoke.sh
# SP5.5: Fork-receiver smoke test for templates/L1/
#
# Simulates a fresh fork receiver consuming templates/L1/ in isolation:
#   1. Copies L1/ to a temp dir
#   2. Static path-leak scan (no relative import escapes L1/)
#   3. Writes package.json with deps from PEER_DEPS.md
#   4. npm install --legacy-peer-deps
#   5. tsc --noEmit
#
# Exit 0 = L1 is self-contained and portable
# Exit 1 = PATH_LEAK or install/typecheck failure (with specific error)
#
# Budget: 300s
# Usage: bash verify/fork-receiver-smoke.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
L1_SRC="$REPO_ROOT/templates/L1"
SMOKE_DIR="$(mktemp -d /tmp/ax-fork-smoke-XXXXXX)"
TIMEOUT_SECS=300
START_TS=$(date +%s)

cleanup() { rm -rf "$SMOKE_DIR"; }
trap cleanup EXIT

echo "[SP5.5] Fork-receiver smoke: L1 portability check"
echo "[SP5.5] Smoke dir: $SMOKE_DIR"

# --- Step 1: Copy only templates/L1/ (what a fork receiver gets) ---
echo ""
echo "[1] Copying templates/L1/ → smoke dir..."
cp -r "$L1_SRC" "$SMOKE_DIR/L1"

# --- Step 2: Static path-leak scan (no network required) ---
echo ""
echo "[2] Path-leak scan (relative imports that escape L1/)..."
LEAK_COUNT=0
while IFS= read -r file; do
  relative_file="${file#$SMOKE_DIR/L1/}"
  while IFS= read -r import_path; do
    # Python resolves the path robustly on both macOS and Linux
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
      echo "PATH_LEAK: $relative_file imports '$import_path' (resolves outside L1/)"
      LEAK_COUNT=$((LEAK_COUNT + 1))
    fi
  done < <(grep -oE "from ['\"](\./|\.\./)([^'\"]+)['\"]" "$file" 2>/dev/null \
             | sed "s/^from ['\"]//;s/['\"]$//" || true)
done < <(find "$SMOKE_DIR/L1" \( -name "*.ts" -o -name "*.tsx" \) \
           ! -path "*/node_modules/*")

if [[ $LEAK_COUNT -gt 0 ]]; then
  echo ""
  echo "[SP5.5] FAIL: $LEAK_COUNT path leak(s) — remove cross-layer imports from L1"
  exit 1
fi
echo "  PATH_LEAK: 0 — all relative imports are within L1/"

# --- Step 3: Minimal package.json (deps from PEER_DEPS.md) ---
echo ""
echo "[3] Writing package.json (PEER_DEPS.md pinned deps)..."
cat > "$SMOKE_DIR/package.json" << 'PKGJSON'
{
  "name": "ax-l1-fork-receiver-smoke",
  "version": "0.0.1",
  "private": true,
  "dependencies": {
    "@radix-ui/react-accordion": "^1.2.0",
    "@radix-ui/react-alert-dialog": "^1.1.0",
    "@radix-ui/react-aspect-ratio": "^1.1.0",
    "@radix-ui/react-avatar": "^1.1.0",
    "@radix-ui/react-checkbox": "^1.1.0",
    "@radix-ui/react-collapsible": "^1.1.0",
    "@radix-ui/react-dialog": "^1.1.0",
    "@radix-ui/react-dropdown-menu": "^2.1.0",
    "@radix-ui/react-hover-card": "^1.1.0",
    "@radix-ui/react-label": "^2.1.0",
    "@radix-ui/react-popover": "^1.1.0",
    "@radix-ui/react-progress": "^1.1.0",
    "@radix-ui/react-radio-group": "^1.2.0",
    "@radix-ui/react-scroll-area": "^1.1.0",
    "@radix-ui/react-select": "^2.1.0",
    "@radix-ui/react-separator": "^1.1.0",
    "@radix-ui/react-slider": "^1.2.0",
    "@radix-ui/react-slot": "^1.1.0",
    "@radix-ui/react-switch": "^1.1.0",
    "@radix-ui/react-tabs": "^1.1.0",
    "@radix-ui/react-tooltip": "^1.1.0",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.1.0",
    "cmdk": "^1.0.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-hook-form": "^7.53.0",
    "react-resizable-panels": "^2.1.0",
    "sonner": "^1.7.0",
    "tailwind-merge": "^2.3.0"
  },
  "devDependencies": {
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "typescript": "^5.6.0"
  }
}
PKGJSON

# --- Step 4: Minimal tsconfig.json ---
echo "[3] Writing tsconfig.json..."
cat > "$SMOKE_DIR/tsconfig.json" << 'TSCJSON'
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": false,
    "skipLibCheck": true,
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "react-jsx"
  },
  "include": ["L1/**/*.ts", "L1/**/*.tsx"]
}
TSCJSON

# --- Step 5: npm install ---
echo ""
echo "[4] npm install (L1 peer deps, budget: ${TIMEOUT_SECS}s)..."
cd "$SMOKE_DIR"
INSTALL_START=$(date +%s)
if ! timeout $TIMEOUT_SECS npm install --no-audit --no-fund --legacy-peer-deps --silent; then
  echo "[SP5.5] FAIL: npm install failed (see above for details)"
  exit 1
fi
INSTALL_END=$(date +%s)
echo "  install: $((INSTALL_END - INSTALL_START))s"

# --- Step 6: tsc --noEmit ---
echo ""
echo "[5] tsc --noEmit (type-checking L1/ in isolation)..."
TSC_START=$(date +%s)
if ! timeout $TIMEOUT_SECS npx --yes tsc --noEmit; then
  echo ""
  echo "[SP5.5] FAIL: tsc type errors found — L1 components have type issues"
  exit 1
fi
TSC_END=$(date +%s)
echo "  tsc: $((TSC_END - TSC_START))s"

# --- Done ---
END_TS=$(date +%s)
DURATION=$((END_TS - START_TS))
echo ""
echo "fork.receiver.smoke.duration=${DURATION}s"
echo "fork.path.leak.count=0"
echo "[SP5.5] PASS: fork-receiver smoke exits 0 (${DURATION}s total)"
