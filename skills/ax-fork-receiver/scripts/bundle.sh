#!/usr/bin/env bash
# skills/ax-fork-receiver/scripts/bundle.sh
# SP22: Create ax-template catalog tarball for fork receivers.
#
# Usage:
#   bash skills/ax-fork-receiver/scripts/bundle.sh <output-tarball>
#   bash skills/ax-fork-receiver/scripts/bundle.sh  # defaults to dist/ax-template-catalog-<sha>.tar.gz
#
# Output: tarball at <output-tarball> + SHA256 printed to stdout.
# Exit 0 on success, 1 on failure.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# ── Determine output path ─────────────────────────────────────────────────────
if [ $# -ge 1 ] && [ -n "$1" ]; then
    OUTPUT_TARBALL="$1"
else
    SHA=$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo "unknown")
    mkdir -p "$REPO_ROOT/dist"
    OUTPUT_TARBALL="$REPO_ROOT/dist/ax-template-catalog-${SHA}.tar.gz"
fi

echo "[bundle] ax-template catalog → $OUTPUT_TARBALL"

# Ensure output directory exists
OUTPUT_DIR="$(dirname "$OUTPUT_TARBALL")"
mkdir -p "$OUTPUT_DIR"

# ── Build tar exclusion args (POSIX-compatible) ───────────────────────────────
# We construct the tarball from REPO_ROOT with explicit exclusions.
# Portable: use --exclude for both macOS (BSD tar) and Linux (GNU tar).

EXCLUDES=(
    # Version control + dev-time artifacts
    ".git"
    ".omc"
    "docs/superpowers"
    "dist"
    # Frontend build artifacts (receiver wires their own app)
    "frontend/.next"
    "frontend/node_modules"
    "frontend/src"
    # Backend build artifacts (receiver wires their own app)
    "backend/build"
    "backend/.gradle"
    "backend/src"
    # Large external Spring portability fixtures (each 60-70MB gradle projects)
    # These are download-on-demand; fork receivers run guards without them.
    "practices/evals/fixtures/spring-realworld"
    "practices/evals/fixtures/spring-petclinic"
    "practices/evals/fixtures/spring-modulith-example"
    # practices-react node_modules (receiver installs their own)
    "practices-react/eslint-plugin-ax/node_modules"
    # Nested build artifacts inside any fixture
    "practices/evals/fixtures/*/build"
    "practices/evals/fixtures/*/.gradle"
    # IDE + OS noise
    ".DS_Store"
    "*.class"
    "*.jar"
)

# Build --exclude flags array
EXCLUDE_FLAGS=()
for excl in "${EXCLUDES[@]}"; do
    EXCLUDE_FLAGS+=("--exclude=./$excl")
done

# ── Select what to include (explicit allowlist) ───────────────────────────────
# We include only the catalog assets. This avoids accidentally pulling in
# top-level noise even if new dirs appear.

INCLUDE_PATHS=(
    "./templates"
    "./skills"
    "./practices"
    "./practices-react"
    "./specs"
    "./contracts"
    "./blueprints"
    "./verify"
    "./METHODOLOGY.md"
    "./CLAUDE.md"
)

# Frontend config only (not src/)
FRONTEND_CONFIGS=(
    "./frontend/package.json"
    "./frontend/eslint.config.mjs"
    "./frontend/tsconfig.json"
    "./frontend/next.config.ts"
)

# Backend config only (not src/)
BACKEND_CONFIGS=(
    "./backend/build.gradle.kts"
    "./backend/settings.gradle.kts"
    "./backend/gradle.properties"
)

# Optional files (include if they exist)
OPTIONAL_FILES=(
    "./README.md"
    "./LICENSE"
    "./frontend/vitest.config.ts"
    "./frontend/playwright.config.ts"
    "./frontend/tailwind.config.ts"
    "./frontend/postcss.config.mjs"
    "./frontend/components.json"
    "./templates/L1/PEER_DEPS.md"
)

# ── Build include list ────────────────────────────────────────────────────────
INCLUDE_LIST=()
for p in "${INCLUDE_PATHS[@]}"; do
    if [ -e "$REPO_ROOT/$p" ] || [ -d "$REPO_ROOT/$p" ]; then
        INCLUDE_LIST+=("$p")
    fi
done
for p in "${FRONTEND_CONFIGS[@]}" "${BACKEND_CONFIGS[@]}" "${OPTIONAL_FILES[@]}"; do
    # Strip leading ./
    rel="${p#./}"
    if [ -f "$REPO_ROOT/$rel" ]; then
        INCLUDE_LIST+=("$p")
    fi
done

echo "[bundle] including ${#INCLUDE_LIST[@]} top-level paths"

# ── Create tarball ────────────────────────────────────────────────────────────
echo "[bundle] creating tarball (this may take a moment)..."

cd "$REPO_ROOT"

# BSD tar (macOS) and GNU tar (Linux) both support --exclude and -czf.
# We avoid -C with a list file for maximum portability.
tar \
    "${EXCLUDE_FLAGS[@]}" \
    -czf "$OUTPUT_TARBALL" \
    "${INCLUDE_LIST[@]}" \
    2>/dev/null

if [ $? -ne 0 ]; then
    echo "[bundle] ERROR: tar failed" >&2
    exit 1
fi

# ── Print size + SHA256 ───────────────────────────────────────────────────────
TARBALL_BYTES=$(wc -c < "$OUTPUT_TARBALL" | tr -d ' ')
TARBALL_MB=$(( TARBALL_BYTES / 1024 / 1024 ))

echo "[bundle] size: ${TARBALL_MB}MB (${TARBALL_BYTES} bytes)"

# SHA256 — macOS uses shasum -a 256, Linux uses sha256sum
if command -v sha256sum >/dev/null 2>&1; then
    SHA256=$(sha256sum "$OUTPUT_TARBALL" | awk '{print $1}')
elif command -v shasum >/dev/null 2>&1; then
    SHA256=$(shasum -a 256 "$OUTPUT_TARBALL" | awk '{print $1}')
else
    SHA256="(sha256 unavailable)"
fi

echo "[bundle] SHA256: $SHA256"
echo "[bundle] tarball: $OUTPUT_TARBALL"
echo "bundle.output=$OUTPUT_TARBALL"
echo "bundle.sha256=$SHA256"
echo "[bundle] DONE"
exit 0
