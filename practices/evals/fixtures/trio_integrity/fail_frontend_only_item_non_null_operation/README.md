# trio_integrity/fail_frontend_only_item_non_null_operation — expected exit 1

A frontend_only domain's specs/<domain>-frontend-l0.yaml has one page-compliance item
with a non-null backend_operation_id (fakeReadPracticeRule) and no static_source_ref.
This catches the item-level bypass that iter3 left open.

Expected: exit 1, stderr contains `frontend_only item has non-null backend_operation_id: fakeReadPracticeRule`
