#!/usr/bin/env bash
# templates/L1/_check-shadcn-drift.sh
# Drift probe: verifies all 32 blessed L1 component files are present.
# Upstream snapshot: practices-react/upstream/shadcn-registry-2026-05.snapshot.md
#
# EXIT 0 — all 32 files present
# EXIT 1 — one or more files missing (drift detected)
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BLESSED_COMPONENTS=(
  # Form primitives (10)
  "components/button.tsx"
  "components/input.tsx"
  "components/textarea.tsx"
  "components/label.tsx"
  "components/select.tsx"
  "components/checkbox.tsx"
  "components/radio-group.tsx"
  "components/switch.tsx"
  "components/slider.tsx"
  "components/form.tsx"
  # Display primitives (8)
  "components/card.tsx"
  "components/badge.tsx"
  "components/avatar.tsx"
  "components/separator.tsx"
  "components/skeleton.tsx"
  "components/progress.tsx"
  "components/aspect-ratio.tsx"
  "components/scroll-area.tsx"
  # Layout primitives (4)
  "components/tabs.tsx"
  "components/accordion.tsx"
  "components/collapsible.tsx"
  "components/resizable.tsx"
  # Overlay primitives (6)
  "components/dialog.tsx"
  "components/alert-dialog.tsx"
  "components/popover.tsx"
  "components/tooltip.tsx"
  "components/hover-card.tsx"
  "components/sheet.tsx"
  # Feedback primitives (4)
  "components/sonner.tsx"
  "components/alert.tsx"
  "components/command.tsx"
  "components/dropdown-menu.tsx"
)

missing=0
for component in "${BLESSED_COMPONENTS[@]}"; do
    if [ ! -f "$SCRIPT_DIR/$component" ]; then
        echo "  MISSING: $component" >&2
        missing=$((missing + 1))
    fi
done

if [ "$missing" -gt 0 ]; then
    echo "shadcn-drift: $missing blessed component(s) missing — drift detected" >&2
    exit 1
fi

echo "shadcn-drift: all ${#BLESSED_COMPONENTS[@]} blessed components present"
exit 0
