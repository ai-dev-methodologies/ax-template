# trio_integrity/fail_manifest_unparseable — expected exit 1

The manifest exists — which is ALL the pre-P2-59 guard required — but is not parseable YAML.
An unparseable policy manifest enforces nothing, so 'the file is there' was never the
property worth asserting.

Expected: exit 1, `MANIFEST_UNPARSEABLE`.
