#!/bin/bash
echo "========================================="
echo "  Checklist Verification Report"
echo "========================================="
PASS=0; FAIL=0; TOTAL=0
while IFS= read -r line; do
  ID=$(echo "$line" | python3 -c "import sys,yaml; d=yaml.safe_load(sys.stdin); print(d.get('id',''))" 2>/dev/null)
  [ -z "$ID" ] && continue
  DESC=$(echo "$line" | python3 -c "import sys,yaml; d=yaml.safe_load(sys.stdin); print(d.get('description',''))" 2>/dev/null)
  CMD=$(echo "$line" | python3 -c "import sys,yaml; d=yaml.safe_load(sys.stdin); print(d.get('script',''))" 2>/dev/null)
  TOTAL=$((TOTAL+1))
  if eval "$CMD" > /dev/null 2>&1; then
    echo "  ✓ [$ID] $DESC"
    PASS=$((PASS+1))
  else
    echo "  ✗ [$ID] $DESC"
    FAIL=$((FAIL+1))
  fi
done < <(python3 -c "import yaml; [print(yaml.dump(c)) for c in yaml.safe_load(open('verify/checklist.yaml'))['checks']]")
echo ""
echo "Results: $PASS/$TOTAL passed, $FAIL failed"
[ $FAIL -eq 0 ] && echo "ALL CHECKS PASSED ✓" || echo "SOME CHECKS FAILED ✗"
exit $FAIL
