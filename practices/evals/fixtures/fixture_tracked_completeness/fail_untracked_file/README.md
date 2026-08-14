# fail_untracked_file

Identical to pass_all_tracked except `sub/ghost.txt` exists on disk while
`.ax-fixture-tracked.txt` does not list it — the exact shape of the observed
defect (`frontend/package-lock.json` present locally, absent from the
repository, with `npm ci` depending on it). Expected: exit 1.
