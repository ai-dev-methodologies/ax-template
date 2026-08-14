# fail_missing_assertion_key

Ten of the eleven declared assertions are recorded and every one of them is
true — A8 (the GH #91 gate-isolation assertion) is simply absent. A run that
silently stopped measuring one thing must not read as a complete run, so a
missing key BLOCKS exactly like a false one.
Expected: exit 1, AX_DOWNSTREAM_ASSERTION_SET_MISMATCH.
