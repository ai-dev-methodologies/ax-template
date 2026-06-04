/*
---
template_id: L2/blocks/data-grid
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 data grid (showcase token system) codified to fill the table/stat gap the persona/role UI/UX audit surfaced — enterprise/fintech personas had no self-contained table primitive in the persona-driven showcase (the L1 data-table uses the L1 --color-* token system, undefined under the showcase shadcn/--ax-status-* tokens). Semantic <table> with <caption>, <th scope='col'>, tabular-nums on numeric columns, and a status cell that maps to --ax-status-* tokens (never raw palette). Prop-driven, self-contained (no @/ alias). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
import type { ReactNode } from "react";

export type DataGridStatus = "success" | "warning" | "danger" | "info" | "neutral";

export interface DataGridColumn {
  key: string;
  header: string;
  /** right-align + tabular-nums for numeric columns */
  numeric?: boolean;
}

export interface DataGridProps {
  caption?: string;
  columns: DataGridColumn[];
  rows: ReadonlyArray<Record<string, ReactNode>>;
}

const STATUS_CLASS: Record<DataGridStatus, string> = {
  success: "bg-[var(--ax-status-success-bg)] text-[var(--ax-status-success-fg)]",
  warning: "bg-[var(--ax-status-warning-bg)] text-[var(--ax-status-warning-fg)]",
  danger: "bg-[var(--ax-status-danger-bg)] text-[var(--ax-status-danger-fg)]",
  info: "bg-[var(--ax-status-info-bg)] text-[var(--ax-status-info-fg)]",
  neutral: "bg-muted text-muted-foreground",
};

// A status pill for use inside a grid cell; colors come from --ax-status-* tokens, never raw palette.
export function GridStatus({ status, children }: { status: DataGridStatus; children: ReactNode }) {
  return (
    <span
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
// tabular-nums + right-align. Rows/columns are prop-driven so it carries no data of its own.
export function DataGrid({ caption, columns, rows }: DataGridProps) {
  return (
    <div className="w-full overflow-x-auto rounded-lg border">
      <table className="w-full border-collapse text-sm">
        {caption ? (
          <caption className="px-3 py-2 text-left text-xs text-muted-foreground">{caption}</caption>
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
                  {row[c.key]}
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
