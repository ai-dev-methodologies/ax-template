# pass_all_tracked

Fixture-shaped tree (not a git repository) whose every file appears in
`.ax-fixture-tracked.txt`, the stand-in for `git ls-tree -r HEAD` described in
the guard header. Nothing exists on disk that a clean clone would lack.
Expected: exit 0.
