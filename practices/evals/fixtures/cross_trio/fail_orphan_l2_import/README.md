# cross_trio/fail_orphan_l2_import — expected exit 1

L4/auth imports templates/L2/blocks/Foo.tsx which carries no `evidence:` block.

Expected: exit 1, stderr contains `ORPHAN_EVIDENCE: templates/L2/blocks/Foo.tsx`
