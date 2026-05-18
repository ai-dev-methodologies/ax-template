// FAIL fixture — SP15 shell without @deprecated or re-export.
// Should cause SHELL_SUPERSEDE_INCOMPLETE guard to exit 1.
// (Both @deprecated and the extended import are intentionally absent.)

import * as React from 'react'

// Old shell with TODO — not yet superseded
export default function FieldArray() {
  // TODO: implement
  return <div>FieldArray shell — placeholder</div>
}
