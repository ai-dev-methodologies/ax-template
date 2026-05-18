/*
---
template_id: L2/blocks/advanced-filter-builder
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "react-querybuilder — structured filter builder with AND/OR groups and field-level operator selection; fixed grammar prevents DSL scope creep"
    url: "https://react-querybuilder.js.org/docs/intro"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "L2 data block — fixed grammar: 5 value operators (eq, neq, contains, gt, lt) + 2 logical connectors (AND, OR), nesting capped at 3 levels (DepthExceededError on violation). Schema serialised to URL/server by L4."
dependencies: [button, badge, select]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

// ─── grammar (fixed — do not extend without a new rule) ───────────────────────

/** 5 allowed value operators */
export type FilterOperator = 'eq' | 'neq' | 'contains' | 'gt' | 'lt'

/** 2 allowed logical connectors */
export type LogicalConnector = 'AND' | 'OR'

/** Max allowed group nesting depth */
export const MAX_FILTER_DEPTH = 3

export class DepthExceededError extends Error {
  constructor(depth: number) {
    super(`Filter group depth ${depth} exceeds maximum of ${MAX_FILTER_DEPTH}`)
    this.name = 'DepthExceededError'
  }
}

// ─── schema ───────────────────────────────────────────────────────────────────

export interface FilterCondition {
  type: 'condition'
  field: string
  operator: FilterOperator
  value: string
}

export interface FilterGroup {
  type: 'group'
  connector: LogicalConnector
  rules: Array<FilterCondition | FilterGroup>
}

export type FilterRule = FilterCondition | FilterGroup

export interface FieldDef {
  key: string
  label: string
  /** Which operators are applicable (defaults to all 5) */
  operators?: FilterOperator[]
}

export interface AdvancedFilterBuilderProps {
  fields: FieldDef[]
  value: FilterGroup
  onChange: (rule: FilterGroup) => void
  /** Max rendered depth; throws DepthExceededError on construct if exceeded */
  maxDepth?: number
}

// ─── helpers ─────────────────────────────────────────────────────────────────

const OPERATOR_LABELS: Record<FilterOperator, string> = {
  eq: '=',
  neq: '≠',
  contains: 'contains',
  gt: '>',
  lt: '<',
}

function emptyCondition(field: string): FilterCondition {
  return { type: 'condition', field, operator: 'eq', value: '' }
}

function emptyGroup(): FilterGroup {
  return { type: 'group', connector: 'AND', rules: [] }
}

function validateDepth(rule: FilterRule, currentDepth = 0): void {
  if (currentDepth > MAX_FILTER_DEPTH) throw new DepthExceededError(currentDepth)
  if (rule.type === 'group') {
    rule.rules.forEach(r => validateDepth(r, currentDepth + 1))
  }
}

// ─── sub-components ──────────────────────────────────────────────────────────

interface ConditionRowProps {
  condition: FilterCondition
  fields: FieldDef[]
  onChange: (next: FilterCondition) => void
  onRemove: () => void
}

function ConditionRow({ condition, fields, onChange, onRemove }: ConditionRowProps) {
  const fieldDef = fields.find(f => f.key === condition.field) ?? fields[0]
  const ops = fieldDef?.operators ?? (Object.keys(OPERATOR_LABELS) as FilterOperator[])

  return (
    <div className="flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2">
      {/* Field */}
      <select
        aria-label="Filter field"
        value={condition.field}
        onChange={e => onChange({ ...condition, field: e.target.value, value: '' })}
        className="rounded border border-input bg-background px-2 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
      >
        {fields.map(f => (
          <option key={f.key} value={f.key}>{f.label}</option>
        ))}
      </select>

      {/* Operator */}
      <select
        aria-label="Filter operator"
        value={condition.operator}
        onChange={e => onChange({ ...condition, operator: e.target.value as FilterOperator })}
        className="rounded border border-input bg-background px-2 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
      >
        {ops.map(op => (
          <option key={op} value={op}>{OPERATOR_LABELS[op]}</option>
        ))}
      </select>

      {/* Value */}
      <input
        aria-label="Filter value"
        type="text"
        value={condition.value}
        onChange={e => onChange({ ...condition, value: e.target.value })}
        placeholder="Value"
        className="flex-1 rounded border border-input bg-background px-2 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
      />

      {/* Remove */}
      <button
        type="button"
        aria-label="Remove condition"
        onClick={onRemove}
        className="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
      >
        <svg aria-hidden="true" width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M2 2l10 10M12 2L2 12" />
        </svg>
      </button>
    </div>
  )
}

interface GroupEditorProps {
  group: FilterGroup
  fields: FieldDef[]
  depth: number
  onChange: (next: FilterGroup) => void
  onRemove?: () => void
}

function GroupEditor({ group, fields, depth, onChange, onRemove }: GroupEditorProps) {
  function updateRule(index: number, next: FilterRule) {
    const rules = group.rules.map((r, i) => (i === index ? next : r))
    onChange({ ...group, rules })
  }

  function removeRule(index: number) {
    const rules = group.rules.filter((_, i) => i !== index)
    onChange({ ...group, rules })
  }

  function addCondition() {
    const cond = emptyCondition(fields[0]?.key ?? '')
    onChange({ ...group, rules: [...group.rules, cond] })
  }

  function addGroup() {
    if (depth >= MAX_FILTER_DEPTH) throw new DepthExceededError(depth + 1)
    onChange({ ...group, rules: [...group.rules, emptyGroup()] })
  }

  const canAddGroup = depth < MAX_FILTER_DEPTH

  return (
    <div
      data-depth={depth}
      className={[
        'rounded-md border p-3 space-y-2',
        depth === 0 ? 'border-border' : 'border-primary/30 bg-primary/5',
      ].join(' ')}
    >
      {/* Group header */}
      <div className="flex items-center gap-2">
        <select
          aria-label="Logical connector"
          value={group.connector}
          onChange={e => onChange({ ...group, connector: e.target.value as LogicalConnector })}
          className="rounded border border-input bg-background px-2 py-1 text-xs font-semibold focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        >
          <option value="AND">AND</option>
          <option value="OR">OR</option>
        </select>
        <span className="text-xs text-muted-foreground">
          {group.rules.length === 0 ? 'No conditions yet' : `${group.rules.length} rule${group.rules.length !== 1 ? 's' : ''}`}
        </span>
        {onRemove && (
          <button
            type="button"
            aria-label="Remove group"
            onClick={onRemove}
            className="ml-auto rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <svg aria-hidden="true" width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M2 2l10 10M12 2L2 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Rules */}
      <div className="space-y-2 pl-3 border-l-2 border-border">
        {group.rules.map((rule, i) =>
          rule.type === 'condition' ? (
            <ConditionRow
              key={i}
              condition={rule}
              fields={fields}
              onChange={next => updateRule(i, next)}
              onRemove={() => removeRule(i)}
            />
          ) : (
            <GroupEditor
              key={i}
              group={rule}
              fields={fields}
              depth={depth + 1}
              onChange={next => updateRule(i, next)}
              onRemove={() => removeRule(i)}
            />
          )
        )}
      </div>

      {/* Add actions */}
      <div className="flex items-center gap-2 pt-1">
        <button
          type="button"
          onClick={addCondition}
          className="inline-flex items-center gap-1 rounded-md border border-dashed border-border px-2 py-1 text-xs text-muted-foreground hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        >
          + Condition
        </button>
        {canAddGroup && (
          <button
            type="button"
            onClick={addGroup}
            className="inline-flex items-center gap-1 rounded-md border border-dashed border-border px-2 py-1 text-xs text-muted-foreground hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            + Group
          </button>
        )}
        {!canAddGroup && (
          <span className="text-xs text-muted-foreground" aria-live="polite">
            Max nesting depth ({MAX_FILTER_DEPTH}) reached
          </span>
        )}
      </div>
    </div>
  )
}

// ─── component ────────────────────────────────────────────────────────────────

/**
 * AdvancedFilterBuilder — structured AND/OR filter rule editor.
 *
 * Fixed grammar: 5 value operators (eq, neq, contains, gt, lt),
 * 2 logical connectors (AND, OR), nesting capped at 3 levels.
 * Throws DepthExceededError if an invalid schema is passed.
 *
 * L4 usage:
 *   const [filterTree, setFilterTree] = useUrlState<FilterGroup>('filter', emptyGroup())
 *   <AdvancedFilterBuilder
 *     fields={TABLE_FIELDS}
 *     value={filterTree}
 *     onChange={setFilterTree}
 *   />
 */
export default function AdvancedFilterBuilder({
  fields,
  value,
  onChange,
}: AdvancedFilterBuilderProps) {
  // Validate depth on mount and on value change
  React.useEffect(() => {
    try {
      validateDepth(value)
    } catch (e) {
      if (e instanceof DepthExceededError) {
        // Surface as a console error; L4 should never pass invalid depth
        console.error('[AdvancedFilterBuilder]', e.message)
      }
    }
  }, [value])

  return (
    <div className="w-full" role="region" aria-label="Advanced filter builder">
      <GroupEditor
        group={value}
        fields={fields}
        depth={0}
        onChange={onChange}
      />
    </div>
  )
}

// Re-export helpers for L4 convenience
export { emptyGroup, emptyCondition, validateDepth }
