#!/bin/bash
set -e
echo "=== Contract Compliance Check ==="
CONTRACT_PATHS=$(python3 -c "import yaml; d=yaml.safe_load(open('contracts/auth-openapi.yaml')); [print(p) for p in sorted(d['paths'].keys())]")
CONTROLLERS=$(grep -rn "@GetMapping\|@PostMapping\|@DeleteMapping\|@PutMapping" backend/src/main/java/ --include="*.java" | grep -v test | grep -v Test)
MISSING=0
while IFS= read -r path; do
  CLEAN=$(echo "$path" | sed 's/{[^}]*}/.*/g')
  if ! echo "$CONTROLLERS" | grep -q "$CLEAN" 2>/dev/null; then
    if ! echo "$CONTROLLERS" | grep -q "$(basename $path)" 2>/dev/null; then
      echo "  ⚠ Contract path not found in controllers: $path"
      MISSING=$((MISSING+1))
    fi
  fi
done <<< "$CONTRACT_PATHS"
if [ $MISSING -gt 0 ]; then
  echo "  FAIL: $MISSING contract paths missing from controllers"
  exit 1
fi
echo "  ✓ All contract paths have controllers"
