# fail_audit_log_dirty_tree_end

Fixture for `completion_checklist_recency_guard.sh`.

FAIL shape: the tree was the committed tree of `head_sha` when the run STARTED
(`tree_clean: true`) but not when its last step finished (`tree_clean_end: false`).

The opening snapshot is taken before the first step runs, so on its own it
certifies nothing about the code the later steps verified. A run that began clean
and picked up an uncommitted change partway through verified two different trees;
neither push-eligibility question ("was this the pushed commit's tree?") can be
answered `yes` for the whole run.

Expected guard exit: 1 (`AUDIT_DIRTY_TREE_EVIDENCE` — the clean-endpoints check
fires first; `tree_stable` is `false` here too, which check 8 would also catch).
