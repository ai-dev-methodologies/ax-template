# fail_digest_mismatch

Log's `artifact_digests.java-archunit-dep` is ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff, which does not
match the sha256 the guard recomputes from
`skills/ax-install-java-enforcement/SKILL.md`'s actual marker body
(427f5f72d2aff41568fe722a86fff586c70bf5c6cc16b2f3bc5c39cc6f211763) — simulating a SKILL.md edited after verify-downstream.sh ran.
Expected: exit 1, AX_DOWNSTREAM_DIGEST_MISMATCH.
