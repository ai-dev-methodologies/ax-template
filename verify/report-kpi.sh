#!/bin/bash
echo "========================================="
echo "  KPI Report"
echo "========================================="
echo ""
echo "[1] Verification Pass Rate:"
cd backend && RESULT=$(./gradlew testAsvs 2>&1 | tail -1) && cd ..
echo "  testAsvs: $RESULT"
echo ""
echo "[2] Rework Rate:"
TOTAL=$(git log --oneline | wc -l | tr -d ' ')
FIXES=$(git log --oneline | grep -c "^[a-f0-9]* fix" || echo 0)
echo "  Total commits: $TOTAL"
echo "  Fix commits: $FIXES"
echo "  Rework rate: $(python3 -c "print(f'{$FIXES/$TOTAL*100:.1f}%')")"
echo ""
echo "[3] Implementation Lead Time:"
FIRST=$(git log --reverse --format="%ai" | head -1)
LAST=$(git log --format="%ai" | head -1)
echo "  First commit: $FIRST"
echo "  Latest commit: $LAST"
