/*
---
template_id: L2/blocks/payment-method-picker
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 payment block — renders available payment methods as selectable cards; method list from props."
dependencies: [card, badge]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/
import * as React from 'react'

export interface PaymentMethod {
  id: string
  label: string
  /** Short description or provider name */
  description?: string
  /** Icon or logo node */
  iconSlot?: React.ReactNode
  disabled?: boolean
}

export interface PaymentMethodPickerProps {
  methods: PaymentMethod[]
  selected?: string
  onSelect: (methodId: string) => void
}

export default function PaymentMethodPicker({
  methods,
  selected,
  onSelect,
}: PaymentMethodPickerProps) {
  return (
    <fieldset className="space-y-2">
      <legend className="text-sm font-medium text-muted-foreground mb-2">
        Payment method
      </legend>

      {methods.map(method => {
        const isSelected = method.id === selected
        return (
          <label
            key={method.id}
            className={[
              'flex cursor-pointer items-center gap-3 rounded-md border p-3 transition-colors',
              isSelected
                ? 'border-primary bg-primary/5'
                : 'border-border bg-background hover:bg-muted/50',
              method.disabled ? 'opacity-50 cursor-not-allowed' : '',
            ].join(' ')}
          >
            <input
              type="radio"
              name="payment-method"
              value={method.id}
              checked={isSelected}
              disabled={method.disabled}
              onChange={() => onSelect(method.id)}
              className="h-4 w-4 border-border text-primary focus:ring-primary"
            />

            {method.iconSlot && (
              <span aria-hidden="true">{method.iconSlot}</span>
            )}

            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium">{method.label}</p>
              {method.description && (
                <p className="text-xs text-muted-foreground">{method.description}</p>
              )}
            </div>
          </label>
        )
      })}
    </fieldset>
  )
}
