/*
---
template_id: L2/blocks/field-wizard
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — step indicator (progressbar) for multi-step flows"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/meter/"
  - source_type: external
    citation: "React Hook Form docs — FormProvider enables multi-step form state sharing across steps"
    url: "https://react-hook-form.com/docs/formprovider"
  - source_type: internal
    rationale: "L2 multi-step form wizard — encapsulates step state machine, progress indicator, and Next/Back navigation. Promoted from SP20 wizard L3 pattern. Used by onboarding flows, checkout, KYC sequences."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface WizardStep {
  /** Unique identifier for this step */
  id: string
  /** Display title shown in the step indicator */
  title: string
  /** Optional description shown under the title */
  description?: string
  /** Step content — rendered as children when this step is active */
  content: React.ReactNode
  /** Whether this step can be skipped (default: false) */
  optional?: boolean
}

export interface FieldWizardProps {
  /** Ordered list of wizard steps */
  steps: WizardStep[]
  /** Called when all steps are completed */
  onComplete?: () => void
  /** Called when user clicks Back on the first step (e.g. cancel flow) */
  onCancel?: () => void
  /** Label for the Back button (default: '이전') */
  backLabel?: string
  /** Label for the Next button (default: '다음') */
  nextLabel?: string
  /** Label for the final step's submit button (default: '완료') */
  submitLabel?: string
  /** Label for the Cancel button on first step (default: '취소') */
  cancelLabel?: string
  /** Current step index (0-based). Uncontrolled when omitted. */
  currentStep?: number
  /** Called when step changes (controlled mode) */
  onStepChange?: (step: number) => void
  /** Additional className for the wizard container */
  className?: string
  /**
   * Validate the current step before advancing.
   * Return true to advance; false to block; string to show error message.
   */
  onValidateStep?: (stepIndex: number) => boolean | string | Promise<boolean | string>
}

// ─── FieldWizard ──────────────────────────────────────────────────────────────

/**
 * FieldWizard — multi-step form wizard with step indicator and navigation.
 *
 * Manages the step state machine internally (uncontrolled) or accepts
 * `currentStep` + `onStepChange` for controlled usage with external state.
 *
 * Integrates with React Hook Form: wrap the wizard in a `<FormProvider>` and
 * call `trigger()` inside `onValidateStep` to validate the current step's
 * fields before advancing:
 *
 * ```tsx
 * const form = useForm()
 * <FormProvider {...form}>
 *   <FieldWizard
 *     steps={steps}
 *     onValidateStep={async (i) => form.trigger(stepFields[i])}
 *     onComplete={form.handleSubmit(onSubmit)}
 *   />
 * </FormProvider>
 * ```
 *
 * **Observability:** fires `form.wizard.step_advanced_count` via `window.__axMetrics`.
 */
export default function FieldWizard({
  steps,
  onComplete,
  onCancel,
  backLabel = '이전',
  nextLabel = '다음',
  submitLabel = '완료',
  cancelLabel = '취소',
  currentStep: controlledStep,
  onStepChange,
  className,
  onValidateStep,
}: FieldWizardProps) {
  const [internalStep, setInternalStep] = React.useState(0)
  const [validationError, setValidationError] = React.useState<string>('')
  const [isValidating, setIsValidating] = React.useState(false)

  const isControlled = controlledStep !== undefined
  const activeStep = isControlled ? controlledStep : internalStep
  const totalSteps = steps.length
  const isFirstStep = activeStep === 0
  const isLastStep = activeStep === totalSteps - 1
  const currentStepData = steps[activeStep]

  const setStep = React.useCallback(
    (next: number) => {
      if (isControlled) {
        onStepChange?.(next)
      } else {
        setInternalStep(next)
      }
    },
    [isControlled, onStepChange]
  )

  const handleNext = React.useCallback(async () => {
    setValidationError('')

    if (onValidateStep) {
      setIsValidating(true)
      const result = await onValidateStep(activeStep)
      setIsValidating(false)

      if (result === false) return
      if (typeof result === 'string') {
        setValidationError(result)
        return
      }
    }

    if (isLastStep) {
      onComplete?.()
      return
    }

    const nextStep = activeStep + 1
    setStep(nextStep)

    // Observability shim
    if (typeof window !== 'undefined') {
      (window as Window & { __axMetrics?: { increment: (k: string) => void } })
        .__axMetrics?.increment('form.wizard.step_advanced_count')
    }
  }, [activeStep, isLastStep, onComplete, onValidateStep, setStep])

  const handleBack = React.useCallback(() => {
    setValidationError('')
    if (isFirstStep) {
      onCancel?.()
      return
    }
    setStep(activeStep - 1)
  }, [activeStep, isFirstStep, onCancel, setStep])

  if (!currentStepData) return null

  return (
    <div className={['space-y-6', className].filter(Boolean).join(' ')}>
      {/* Step indicator */}
      <nav aria-label="폼 진행 단계" className="w-full">
        <ol className="flex items-center gap-0">
          {steps.map((step, index) => {
            const isCompleted = index < activeStep
            const isCurrent = index === activeStep
            const isUpcoming = index > activeStep

            return (
              <React.Fragment key={step.id}>
                <li className="flex flex-col items-center">
                  <div className="flex items-center">
                    <div
                      aria-current={isCurrent ? 'step' : undefined}
                      className={[
                        'flex h-8 w-8 items-center justify-center rounded-full',
                        'text-sm font-medium transition-colors',
                        isCompleted
                          ? 'bg-primary text-primary-foreground'
                          : isCurrent
                            ? 'border-2 border-primary bg-background text-primary'
                            : 'border-2 border-muted bg-background text-muted-foreground',
                      ].join(' ')}
                    >
                      {isCompleted ? (
                        <svg
                          className="h-4 w-4"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth={3}
                          aria-hidden="true"
                        >
                          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                      ) : (
                        <span aria-hidden="true">{index + 1}</span>
                      )}
                    </div>
                  </div>
                  <span
                    className={[
                      'mt-1 text-xs font-medium',
                      isCurrent ? 'text-primary' : isUpcoming ? 'text-muted-foreground' : 'text-foreground',
                    ].join(' ')}
                  >
                    {step.title}
                    {step.optional && (
                      <span className="ml-1 text-muted-foreground">(선택)</span>
                    )}
                  </span>
                </li>

                {/* Connector line */}
                {index < totalSteps - 1 && (
                  <div
                    aria-hidden="true"
                    className={[
                      'mx-2 mb-5 h-px flex-1',
                      index < activeStep ? 'bg-primary' : 'bg-muted',
                    ].join(' ')}
                  />
                )}
              </React.Fragment>
            )
          })}
        </ol>

        {/* Progress bar for screen readers */}
        <div
          role="progressbar"
          aria-valuenow={activeStep + 1}
          aria-valuemin={1}
          aria-valuemax={totalSteps}
          aria-label={`${totalSteps}단계 중 ${activeStep + 1}단계`}
          className="sr-only"
        />
      </nav>

      {/* Step content */}
      <section aria-label={`${currentStepData.title} 입력`}>
        {currentStepData.description && (
          <p className="mb-4 text-sm text-muted-foreground">
            {currentStepData.description}
          </p>
        )}
        {currentStepData.content}
      </section>

      {/* Validation error */}
      {validationError && (
        <p role="alert" className="text-sm text-destructive">
          {validationError}
        </p>
      )}

      {/* Navigation */}
      <div className="flex justify-between gap-3">
        <button
          type="button"
          onClick={handleBack}
          disabled={isValidating}
          className="inline-flex items-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          {isFirstStep ? cancelLabel : backLabel}
        </button>

        <button
          type="button"
          onClick={handleNext}
          disabled={isValidating}
          className="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          {isValidating
            ? '확인 중…'
            : isLastStep
              ? submitLabel
              : nextLabel}
        </button>
      </div>
    </div>
  )
}

export type { WizardStep as FieldWizardStep }
