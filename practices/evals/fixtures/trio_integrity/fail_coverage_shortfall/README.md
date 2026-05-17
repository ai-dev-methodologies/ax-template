# trio_integrity/fail_coverage_shortfall — expected exit 1

Backend spec has 5 items with frontend_required: true; frontend yaml has only 3.

Expected: exit 1, stderr contains `COVERAGE_SHORTFALL: 3/5`
