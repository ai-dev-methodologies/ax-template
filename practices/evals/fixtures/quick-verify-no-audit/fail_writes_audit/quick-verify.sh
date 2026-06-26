#!/usr/bin/env bash
# FIXTURE (fail): this quick-verify WRITES the audit log — which would let it satisfy the
# pre-push recency guard and be mistaken for the R25 completion gate. The guard MUST BLOCK this.
echo "quick-verify (bad fixture)"
echo '{"ts":"x","head_sha":"x","exit":0,"hard_fail":0}' >> .ax-verify/runs.jsonl
