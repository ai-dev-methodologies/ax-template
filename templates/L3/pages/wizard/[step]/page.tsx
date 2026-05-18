/*
---
template_id: L3/pages/wizard
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — dynamic segments"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — Step Indicator / Wizard Pattern"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/wizard/"
  - source_type: internal_design
    rationale: "Generic multi-step wizard skeleton. Manages currentStep internally via useState. Exposes onStepChange and onComplete callbacks for L4 integration (URL sync, server actions). Steps passed as an array of { title, content } objects."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
'use client'

import * as React from 'react'

/**
 * WizardPage — generic multi-step wizard skeleton.
 *
 * Slot props:
 *   - steps         (required) array of { title, content } step objects
 *   - initialStep   (optional) starting step index (0-based, default: 0)
 *   - onComplete    (optional) called when user clicks Finish on the last step
 *   - onStepChange  (optional) called with new step index whenever step changes
 *   - nextLabel     (optional) Next button label (default: "Next")
 *   - backLabel     (optional) Back button label (default: "Back")
 *   - completeLabel (optional) Finish button label (default: "Finish")
 *
 * L4 usage:
 *   import WizardPage from 'templates/L3/pages/wizard/[step]/page'
 *   export default async function OnboardingRoute({ params }) {
 *     const currentStep = Number(params.step) - 1  // URL is 1-based
 *     return (
 *       <WizardPage
 *         steps={[
 *           { title: 'Profile', content: <ProfileStep /> },
 *           { title: 'Preferences', content: <PreferencesStep /> },
 *           { title: 'Review', content: <ReviewStep /> },
 *         ]}
 *         initialStep={currentStep}
 *         onStepChange={(s) => router.push(`/onboarding/${s + 1}`)}
 *         onComplete={() => router.push('/dashboard')}
 *       />
 *     )
 *   }
 */
export interface WizardStep {
  /** Step title shown in the progress indicator */
  title: string
  /** Step content rendered in the main body */
  content: React.ReactNode
}

export interface WizardPageProps {
  /** Ordered array of wizard steps */
  steps: WizardStep[]
  /** Starting step index, 0-based (default: 0) */
  initialStep?: number
  /** Called with the new step index when navigation occurs */
  onStepChange?: (step: number) => void
  /** Called when the user clicks Finish on the last step */
  onComplete?: () => void | Promise<void>
  /** Next button label (default: "Next") */
  nextLabel?: string
  /** Back button label (default: "Back") */
  backLabel?: string
  /** Finish button label (default: "Finish") */
  completeLabel?: string
}

export default function WizardPage({
  steps,
  initialStep = 0,
  onStepChange,
  onComplete,
  nextLabel = 'Next',
  backLabel = 'Back',
  completeLabel = 'Finish',
}: WizardPageProps) {
  const [currentStep, setCurrentStep] = React.useState(
    Math.max(0, Math.min(initialStep, steps.length - 1))
  )
  const [isPending, setIsPending] = React.useState(false)

  const isFirst = currentStep === 0
  const isLast = currentStep === steps.length - 1
  const progressPct = steps.length > 1
    ? Math.round((currentStep / (steps.length - 1)) * 100)
    : 100

  function goTo(next: number) {
    const clamped = Math.max(0, Math.min(next, steps.length - 1))
    setCurrentStep(clamped)
    onStepChange?.(clamped)
  }

  async function handleNext() {
    if (isLast) {
      setIsPending(true)
      try {
        await onComplete?.()
      } finally {
        setIsPending(false)
      }
    } else {
      goTo(currentStep + 1)
    }
  }

  const step = steps[currentStep]

  return (
    <main className="container mx-auto px-4 py-8 max-w-2xl space-y-8">
      {/* Progress bar */}
      <div className="space-y-3">
        {/* Step labels */}
        <nav aria-label="Wizard steps">
          <ol className="flex items-center gap-0">
            {steps.map((s, idx) => {
              const state =
                idx < currentStep
                  ? 'done'
                  : idx === currentStep
                  ? 'active'
                  : 'upcoming'
              return (
                <li key={idx} className="flex flex-1 items-center">
                  <div className="flex flex-col items-center gap-1">
                    <span
                      aria-current={state === 'active' ? 'step' : undefined}
                      className={[
                        'flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold',
                        state === 'done'
                          ? 'bg-primary text-primary-foreground'
                          : state === 'active'
                          ? 'border-2 border-primary text-primary'
                          : 'border-2 border-muted-foreground/30 text-muted-foreground',
                      ].join(' ')}
                    >
                      {state === 'done' ? '✓' : idx + 1}
                    </span>
                    <span
                      className={[
                        'hidden sm:block text-xs max-w-[4rem] text-center truncate',
                        state === 'active'
                          ? 'font-medium text-foreground'
                          : 'text-muted-foreground',
                      ].join(' ')}
                    >
                      {s.title}
                    </span>
                  </div>
                  {idx < steps.length - 1 && (
                    <div
                      className={[
                        'h-0.5 flex-1 mx-2 mt-[-1rem]',
                        idx < currentStep ? 'bg-primary' : 'bg-muted-foreground/20',
                      ].join(' ')}
                      aria-hidden="true"
                    />
                  )}
                </li>
              )
            })}
          </ol>
        </nav>

        {/* Linear progress */}
        <div
          className="h-1 w-full rounded-full bg-muted overflow-hidden"
          role="progressbar"
          aria-valuenow={progressPct}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`Step ${currentStep + 1} of ${steps.length}`}
        >
          <div
            className="h-full bg-primary transition-all duration-300"
            style={{ width: `${progressPct}%` }}
          />
        </div>
      </div>

      {/* Step heading */}
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">
          {step?.title}
        </h1>
        <p className="text-sm text-muted-foreground">
          Step {currentStep + 1} of {steps.length}
        </p>
      </div>

      {/* Step content slot */}
      <div className="min-h-[12rem]">{step?.content}</div>

      {/* Navigation */}
      <div className="flex items-center justify-between gap-4 border-t pt-4">
        <button
          type="button"
          onClick={() => goTo(currentStep - 1)}
          disabled={isFirst}
          className="inline-flex items-center rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted transition-colors disabled:pointer-events-none disabled:opacity-40"
        >
          {backLabel}
        </button>

        <button
          type="button"
          onClick={handleNext}
          disabled={isPending}
          className="inline-flex items-center rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity disabled:pointer-events-none disabled:opacity-50"
        >
          {isPending ? '…' : isLast ? completeLabel : nextLabel}
        </button>
      </div>
    </main>
  )
}
