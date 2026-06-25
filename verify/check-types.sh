#!/bin/bash
set -e
echo "=== OpenAPI → TypeScript Type Sync Check ==="
ISSUES=0

# Extract request/response field names from OpenAPI
CONTRACT_FIELDS=$(python3 -c "
import yaml
spec = yaml.safe_load(open('contracts/auth-openapi.yaml'))
schemas = spec.get('components',{}).get('schemas',{})
for name, schema in schemas.items():
    props = schema.get('properties',{})
    for p in props:
        print(f'{name}.{p}')
" 2>/dev/null | sort)

# Check key types exist in TypeScript client (moved into the @ax/core package
# when the frontend became an npm-workspaces monorepo).
CLIENT_FILE="frontend/packages/core/src/api/authClient.ts"
if [ ! -f "$CLIENT_FILE" ]; then
  echo "  ✗ authClient.ts not found"
  exit 1
fi

# Verify critical interface fields are present
for field in "accessToken" "tokenType" "expiresIn" "userId" "email" "role" "emailVerified" "message"; do
  if grep -q "$field" "$CLIENT_FILE"; then
    true
  else
    echo "  ⚠ Field '$field' not found in authClient.ts"
    ISSUES=$((ISSUES+1))
  fi
done

if [ $ISSUES -gt 0 ]; then
  echo "  FAIL: $ISSUES type sync issues"
  exit 1
fi
echo "  ✓ Type sync check passed"
