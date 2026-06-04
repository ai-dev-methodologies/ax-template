/*
---
template_id: L2/blocks/data-grid
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 data grid (showcase token system) codified to fill the table/stat gap the persona/role UI/UX audit surfaced — enterprise/fintech personas had no self-contained table primitive in the persona-driven showcase (the L1 data-table uses the L1 --color-* token system, undefined under the showcase shadcn/--ax-status-* tokens). Semantic <table> with <caption>, <th scope='col'>, tabular-nums on numeric columns; the scrollable wrapper is a keyboard-operable role='region' named by the caption via aria-labelledby (WCAG 2.1.1, no duplicated label text). The status cell (GridStatus) maps to --ax-status-* tokens (never raw palette) and carries role='status' + aria-label for WCAG 4.1.3 — matching the gold-standard status-badge. rows are keyed to the column keys (Record<K>): a caller that types its columns `as const` gets a compile error on a missing cell, and row[c.key] ?? null keeps a missing cell from rendering undefined at runtime. Prop-driven, self-contained (no @/ alias). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
import { useId } from "react";
import type { ReactNode } from "react";

export type DataGridStatus = "success" | "warning" | "danger" | "info" | "neutral";

export interface DataGridColumn<K extends string = string> {
  key: K;
  header: string;
  /** right-align + tabular-nums for numeric columns */
  numeric?: boolean;
}

export interface DataGridProps<K extends string = string> {
  caption?: string;
  columns: ReadonlyArray<DataGridColumn<K>>;
  rows: ReadonlyArray<Record<K, ReactNode>>;
}

const STATUS_CLASS: Record<DataGridStatus, string> = {
  success: "bg-[var(--ax-status-success-bg)] text-[var(--ax-status-success-fg)]",
  warning: "bg-[var(--ax-status-warning-bg)] text-[var(--ax-status-warning-fg)]",
  danger: "bg-[var(--ax-status-danger-bg)] text-[var(--ax-status-danger-fg)]",
  info: "bg-[var(--ax-status-info-bg)] text-[var(--ax-status-info-fg)]",
  neutral: "bg-muted text-muted-foreground",
};

// A status pill for use inside a grid cell; colors come from --ax-status-* tokens, never raw palette.
// role="status" + aria-label expose the status meaning to assistive tech (WCAG 4.1.3), per the
// governing rule and the gold-standard status-badge. The announced name defaults to the visible text
// (so WCAG 2.5.3 Label-in-Name holds), then to `label`, then to the status keyword when children are
// not plain text (e.g. an icon).
export function GridStatus({
  status,
  children,
  label,
}: {
  status: DataGridStatus;
  children: ReactNode;
  label?: string;
}) {
  const name = label ?? (typeof children === "string" ? children : status);
  return (
    <span
      role="status"
      aria-label={name}
      className={[
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
        STATUS_CLASS[status],
      ].join(" ")}
    >
      {children}
    </span>
  );
}

// A semantic data grid: <caption> names the table, <th scope="col"> headers, numeric cells get
// tabular-nums + right-align. Rows/columns are prop-driven so it carries no data of its own; rows are
// keyed to the declared column keys (Record<K>) so a column typed `as const` makes a missing cell a
// compile error. The scrollable wrapper is a keyboard-operable region named by the caption.
export function DataGrid<K extends string>({ caption, columns, rows }: DataGridProps<K>) {
  const capId = useId();
  return (
    <div
      role="region"
      aria-labelledby={caption ? capId : undefined}
      aria-label={caption ? undefined : "Data table"}
      tabIndex={0}
      className="w-full overflow-x-auto rounded-lg border focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <table className="w-full border-collapse text-sm">
        {caption ? (
          <caption id={capId} className="px-3 py-2 text-left text-xs text-muted-foreground">
            {caption}
          </caption>
        ) : null}
        <thead className="border-b bg-muted/50">
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                scope="col"
                className={c.numeric ? "px-3 py-2 text-right font-medium" : "px-3 py-2 text-left font-medium"}
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} className="border-b last:border-0">
              {columns.map((c) => (
                <td key={c.key} className={c.numeric ? "px-3 py-2 text-right tabular-nums" : "px-3 py-2"}>
                  {row[c.key] ?? null}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default DataGrid;
