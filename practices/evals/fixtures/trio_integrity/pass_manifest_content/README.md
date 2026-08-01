# trio_integrity/pass_manifest_content — expected exit 0

Control for the P2-59 manifest-content checks: the manifest declares a surface, an
operation id, a spec_item backlink and a render_boundary page/view, and ALL FOUR
resolve (operation published by contracts/auth-openapi.yaml, item id present in
specs/auth-frontend-l0.yaml, files on disk, path published by contracts/auth-ui.yaml).

Expected: exit 0. Without this control the three fail fixtures below could be passing
for the wrong reason (a check that rejects every manifest).
