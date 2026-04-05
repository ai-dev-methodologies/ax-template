#!/bin/bash
set -e
echo "========================================="
echo "  ax-template Full Verification Suite"
echo "========================================="
echo ""
echo "[1/5] Backend Build..."
(cd backend && ./gradlew build -q)
echo "  ✓ Build passed"
echo "[2/5] ASVS Compliance..."
(cd backend && ./gradlew testAsvs -q)
echo "  ✓ ASVS tests passed"
echo "[3/5] Contract Check..."
./verify/check-contract.sh
echo "[4/5] Security Check..."
./verify/check-security.sh
echo "[5/6] RBAC Check..."
./verify/check-rbac.sh
echo ""
echo "========================================="
echo "  ALL CHECKS PASSED ✓"
echo "========================================="
