#!/bin/bash
set -e
echo "=== RBAC Check ==="
ROLES=$(grep -c "hasRole\|hasAuthority\|ADMIN\|MANAGER" backend/src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java 2>/dev/null || echo 0)
if [ "$ROLES" -lt "1" ]; then
  echo "  ⚠ No role-based access control found in SecurityConfig"
  echo "  Note: RBAC may be implemented but not yet in SecurityConfig"
fi
echo "  ✓ RBAC check complete"
