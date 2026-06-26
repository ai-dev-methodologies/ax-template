#!/usr/bin/env bash
# FIXTURE (pass): an advisory quick-verify that does NOT write the audit log and prints the
# ITERATION-ONLY banner. The guard MUST PASS.
echo "  quick-verify — ITERATION-ONLY fast feedback. NOT the R25 completion gate."
( cd backend && ./gradlew build -x test )
bash practices/evals/run-all-guards.sh
echo "quick-verify: PASS (ITERATION-ONLY — run verify-completion.sh before declaring done)."
