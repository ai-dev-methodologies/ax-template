# trio_integrity/fail_frontend_only_unreachable_route — expected exit 1

static_source_ref lists a path that resolves to zero files (does-not-exist-*.md).

Expected: exit 1, stderr contains `static_source_ref resolves to zero files`
