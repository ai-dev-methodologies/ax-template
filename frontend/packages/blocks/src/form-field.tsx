/*
---
template_id: L2/blocks/form-field
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 form field codified to fill the form/input gap the persona/role UI/UX audit found (enterprise/fintech/developer personas were thin on form primitives). Self-contained (no @/ alias), semantic-token color (danger via --ax-status-danger-fg, never raw palette), and WCAG-correct: label associated via htmlFor/id, the required asterisk is aria-hidden (the input's required attribute carries the semantics), aria-invalid + aria-describedby on error, role=alert on the error message; helper and error coexist (describedby references both) so instructions survive an error (WCAG 3.3.2). Caller props are spread FIRST so the block's id/required/aria-* and label association always win (a caller-supplied aria-describedby cannot silently detach the error). Typed props (no enum). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
import type { InputHTMLAttributes } from "react";

export interface TextFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "id"> {
  id: string;
  label: string;
  helper?: string;
  error?: string;
}

// A labeled text input with helper/error states. Label is associated via htmlFor/id; on error the
// input gets aria-invalid + aria-describedby and the message is a role="alert" live region. Helper
// and error can coexist — describedBy references whichever are present so instructions persist.
export function TextField({ id, label, helper, error, required, className, ...input }: TextFieldProps) {
  const describedBy =
    [helper ? `${id}-helper` : null, error ? `${id}-error` : null].filter(Boolean).join(" ") || undefined;
  return (
    <div className="flex w-full max-w-xs flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
        {required ? (
          <span aria-hidden="true" className="text-[var(--ax-status-danger-fg)]">
            {" *"}
          </span>
        ) : null}
      </label>
      <input
        {...input}
        id={id}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={[
          "rounded-md border bg-transparent px-3 py-2 text-sm outline-none",
          "focus-visible:ring-2 focus-visible:ring-ring",
          error ? "border-[var(--ax-status-danger-fg)]" : "",
          className,
        ]
          .filter(Boolean)
          .join(" ")}
      />
      {helper ? (
        <p id={`${id}-helper`} className="text-xs text-muted-foreground">
          {helper}
        </p>
      ) : null}
      {error ? (
        <p id={`${id}-error`} role="alert" className="text-xs text-[var(--ax-status-danger-fg)]">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export default TextField;
