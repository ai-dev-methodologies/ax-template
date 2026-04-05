#!/bin/bash
set -e
./verify/run-all.sh
echo "[+] Frontend Build..."
(cd frontend && npm run build -s)
echo "  ✓ Frontend build passed"
echo "[+] Frontend Tests..."
(cd frontend && npm run test -s)
echo "  ✓ Frontend tests passed"
echo ""
echo "CI GATE: ALL PASSED ✓"
