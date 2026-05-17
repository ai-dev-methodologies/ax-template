#!/bin/bash
set -e
echo "=== Security Check ==="
ISSUES=0
SECRETS=$(grep -rn 'password.*=.*"[a-zA-Z0-9]\{12,\}"' archive/backend-reference/src/main/java/ --include="*.java" 2>/dev/null | grep -v test | grep -v Test | grep -v encode | grep -v matches | grep -v "dummy" | wc -l | tr -d ' ')
if [ "$SECRETS" -gt 0 ]; then
  echo "  ✗ Hardcoded secrets found: $SECRETS"
  ISSUES=$((ISSUES+1))
fi
CSRF=$(grep -c "ignoringRequestMatchers" archive/backend-reference/src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java 2>/dev/null || echo 0)
if [ "$CSRF" -eq 0 ]; then
  echo "  ✗ CSRF not configured for API endpoints"
  ISSUES=$((ISSUES+1))
fi
COOKIE=$(grep -rc "HttpOnly\|httpOnly\|Secure\|sameSite\|SameSite" archive/backend-reference/src/main/java/com/ax/template/authblueprint/auth/ 2>/dev/null | awk -F: '{s+=$2}END{print s}')
if [ "$COOKIE" -lt 2 ]; then
  echo "  ✗ Cookie security flags missing"
  ISSUES=$((ISSUES+1))
fi
if [ $ISSUES -gt 0 ]; then echo "  FAIL: $ISSUES security issues"; exit 1; fi
echo "  ✓ Security checks passed"
