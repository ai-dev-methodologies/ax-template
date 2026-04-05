#!/bin/bash
PROVIDER=${1:-google}
echo "=== OAuth Full Flow: $PROVIDER ==="
echo "  이 검증은 실제 브라우저에서 $PROVIDER 로그인을 수행합니다."
echo "  Requires: backend (8080) + frontend (5173) running + OAuth keys configured"
echo ""

# Check servers
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
  echo "  ✗ Backend not running"; exit 1
fi
if ! curl -s http://localhost:5173 > /dev/null 2>&1; then
  echo "  ✗ Frontend not running"; exit 1
fi

# Check OAuth key
PROVIDER_UPPER=$(echo $PROVIDER | tr '[:lower:]' '[:upper:]')
KEY_VAR="${PROVIDER_UPPER}_CLIENT_ID"
KEY_VAL="${!KEY_VAR}"
if [ -z "$KEY_VAL" ] || echo "$KEY_VAL" | grep -q "dummy"; then
  echo "  ✗ ${KEY_VAR} not configured"
  echo "  → docs/OAUTH-SETUP-GUIDE.md 참조"
  exit 1
fi

# Run Playwright E2E for this provider
cd frontend && npx playwright test tests/e2e-auth.spec.ts -g "$PROVIDER" 2>&1 | tail -5
RESULT=${PIPESTATUS[0]}

if [ $RESULT -eq 0 ]; then
  echo ""
  echo "  ✓ $PROVIDER OAuth redirect verified"
  echo "  ⚠ 전체 로그인 플로우(콜백→JWT)는 수동 검증 필요:"
  echo "    1. 브라우저에서 http://localhost:5173/login 접속"
  echo "    2. '$PROVIDER 로그인' 버튼 클릭"
  echo "    3. $PROVIDER 계정으로 로그인"
  echo "    4. 콜백 후 대시보드에 프로필 표시 확인"
else
  echo "  ✗ $PROVIDER OAuth test FAILED"
  exit 1
fi
