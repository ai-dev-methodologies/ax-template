# fail_forged_single_assertion

The reproduced hole, frozen as a fixture. Everything the guard used to check is
satisfied — head_sha matches, tree_clean is true, `artifact_digests` matches the
recompute, verdict is `pass`, override is empty — and the entire `assertions`
object is one hand-typed key:

```
"assertions": {"forged-single": true}
```

Before the completeness check, that passed the release gate ("every value that
is present is true" is vacuously satisfied by one invented key). Now the key set
must equal the harness's declared manifest
(`.ax-downstream/expected_assertions.txt` here; the `# ax:assertions` line of
practices/scripts/verify-downstream.sh in live mode).
Expected: exit 1, AX_DOWNSTREAM_ASSERTION_SET_MISMATCH.
