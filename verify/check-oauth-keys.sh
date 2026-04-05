#!/bin/bash
echo "=== OAuth Key Status ==="
ISSUES=0

for provider in GOOGLE KAKAO NAVER; do
  ID_VAR="${provider}_CLIENT_ID"
  SECRET_VAR="${provider}_CLIENT_SECRET"
  ID_VAL="${!ID_VAR}"
  SECRET_VAL="${!SECRET_VAR}"
  
  if [ -z "$ID_VAL" ] || [ "$ID_VAL" = "dummy-$(echo $provider | tr '[:upper:]' '[:lower:]')-id" ]; then
    echo "  ❌ ${ID_VAR}: not set (using dummy)"
    ISSUES=$((ISSUES+1))
  else
    echo "  ✅ ${ID_VAR}: configured"
  fi
  
  if [ -z "$SECRET_VAL" ] || [ "$SECRET_VAL" = "dummy-$(echo $provider | tr '[:upper:]' '[:lower:]')-secret" ]; then
    echo "  ❌ ${SECRET_VAR}: not set (using dummy)"
    ISSUES=$((ISSUES+1))
  else
    echo "  ✅ ${SECRET_VAR}: configured"
  fi
done

echo ""
if [ $ISSUES -gt 0 ]; then
  echo "⚠ $ISSUES OAuth keys missing. 이메일 로그인은 동작하지만 SNS 로그인은 불가합니다."
  echo "  → docs/OAUTH-SETUP-GUIDE.md 참조"
else
  echo "✓ All OAuth keys configured"
fi
