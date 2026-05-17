#!/bin/bash
set -e
echo "=== E2E Browser Test ==="
echo "  Requires: backend (8080) + frontend (5173) running"

# Check servers
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
  echo "  ✗ Backend not running on :8080"
  echo "  Start: cd archive/backend-reference && ./gradlew bootRun"
  exit 1
fi
if ! curl -s http://localhost:5173 > /dev/null 2>&1; then
  echo "  ✗ Frontend not running on :5173"
  echo "  Start: cd frontend && npm run dev"
  exit 1
fi

cd frontend && npx playwright test tests/e2e-auth.spec.ts 2>&1 | tail -3
RESULT=${PIPESTATUS[0]}
if [ $RESULT -eq 0 ]; then
  echo "  ✓ E2E tests passed"
else
  echo "  ✗ E2E tests FAILED"
  exit 1
fi
