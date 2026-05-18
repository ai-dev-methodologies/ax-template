/*
---
template_id: L2/blocks/MappingEditor
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — maps CSV/Excel source columns to schema target fields; onChange injected by L4 for controlled state."
dependencies: [select, button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface SchemaField {
  key: string
  label: string
  required: boolean
}

export interface ColumnMapping {
  sourceColumn: string
  targetField: string | null
}

export interface MappingEditorProps {
  /** Columns detected in the uploaded file */
  sourceColumns: string[]
  /** Fields in the target schema */
  schemaFields: SchemaField[]
  /** Current mapping state — controlled by L4 */
  mappings: ColumnMapping[]
  onChange: (updatedMappings: ColumnMapping[]) => void
  onConfirm: () => void
  onBack: () => void
}

/**
 * Column-to-field mapping editor for CSV/Excel imports.
 * Fully controlled — no API calls. L4 owns the mapping state.
 */
export function MappingEditor({
  sourceColumns,
  schemaFields,
  mappings,
  onChange,
  onConfirm,
  onBack,
}: MappingEditorProps) {
  const unmappedRequired = schemaFields.filter(
    (field) =>
      field.required &&
      !mappings.some((m) => m.targetField === field.key),
  )

  function handleMappingChange(
    sourceColumn: string,
    e: React.ChangeEvent<HTMLSelectElement>,
  ) {
    const targetField = e.target.value || null
    const updated = mappings.map((m) =>
      m.sourceColumn === sourceColumn ? { ...m, targetField } : m,
    )
    onChange(updated)
  }

  return (
    <div className="mapping-editor">
      <header className="mapping-editor__header">
        <h2 className="mapping-editor__title">Map Columns</h2>
        <p className="mapping-editor__description">
          Match each file column to the corresponding field in the schema.
        </p>
      </header>

      <table className="mapping-editor__table" aria-label="Column mapping">
        <thead>
          <tr>
            <th scope="col">File column</th>
            <th scope="col">Schema field</th>
          </tr>
        </thead>
        <tbody>
          {sourceColumns.map((col) => {
            const mapping = mappings.find((m) => m.sourceColumn === col)
            return (
              <tr key={col}>
                <td className="mapping-editor__source">{col}</td>
                <td className="mapping-editor__target">
                  <select
                    aria-label={`Map "${col}" to`}
                    value={mapping?.targetField ?? ''}
                    onChange={(e) => handleMappingChange(col, e)}
                  >
                    <option value="">— skip —</option>
                    {schemaFields.map((field) => (
                      <option key={field.key} value={field.key}>
                        {field.label}
                        {field.required ? ' *' : ''}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      {unmappedRequired.length > 0 && (
        <p className="mapping-editor__warning" role="alert">
          Required fields not yet mapped:{' '}
          {unmappedRequired.map((f) => f.label).join(', ')}
        </p>
      )}

      <footer className="mapping-editor__actions">
        <button type="button" className="mapping-editor__back" onClick={onBack}>
          Back
        </button>
        <button
          type="button"
          className="mapping-editor__confirm"
          onClick={onConfirm}
          disabled={unmappedRequired.length > 0}
        >
          Continue
        </button>
      </footer>
    </div>
  )
}
