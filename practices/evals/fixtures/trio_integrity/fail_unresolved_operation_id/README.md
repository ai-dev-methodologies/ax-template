# trio_integrity/fail_unresolved_operation_id — expected exit 1

UI Contract references backend_operation_id: bogusOp which is not present in the OpenAPI doc.

Expected: exit 1, stderr contains `UNRESOLVED_OPERATION_ID` or `bogusOp`
