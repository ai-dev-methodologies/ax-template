#!/bin/bash
MAX_RETRIES=2
echo "========================================="
echo "  Verify with Auto-Retry ($MAX_RETRIES max)"
echo "========================================="

for i in $(seq 1 $MAX_RETRIES); do
  echo ""
  echo "[Attempt $i/$MAX_RETRIES]"
  if ./verify/run-all.sh 2>&1; then
    echo ""
    echo "✓ PASSED on attempt $i"
    exit 0
  fi
  echo ""
  echo "✗ FAILED attempt $i — attempting auto-fix..."
  # Basic auto-fix: rebuild
  (cd backend && ./gradlew build -q 2>/dev/null)
  (cd frontend && npm run build -s 2>/dev/null)
done

echo ""
echo "========================================="
echo "  HITL REQUIRED"
echo "  $MAX_RETRIES retries exhausted."
echo "  Manual review needed."
echo "========================================="
exit 1
