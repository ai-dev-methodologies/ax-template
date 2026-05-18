# wizard — L3 Multi-Step Wizard Page Template

Generic multi-step wizard skeleton. Manages `currentStep` state internally with
Next / Back navigation. Exposes `onStepChange` and `onComplete` callbacks for L4
URL-sync integration (URL search params or Next.js router). Each step is a
`{ title, content }` object passed as a prop array.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `steps` | `WizardStep[]` | ✅ | Ordered array of `{ title: string; content: ReactNode }` |
| `initialStep` | `number` | — | Starting step index, 0-based (default: `0`) |
| `onStepChange` | `(step: number) => void` | — | Called on each navigation change (for URL sync) |
| `onComplete` | `() => void \| Promise<void>` | — | Called when Finish is clicked on the last step |
| `nextLabel` | `string` | — | Next button label (default: `"Next"`) |
| `backLabel` | `string` | — | Back button label (default: `"Back"`) |
| `completeLabel` | `string` | — | Finish button label (default: `"Finish"`) |

## Behaviour

1. Renders a step-indicator nav (numbered circles + connector lines) and a linear progress bar
2. Clicking **Next** advances to the next step; calling `onStepChange(next)` for URL sync
3. On the last step, **Next** becomes **Finish** and calls `onComplete()`
4. **Back** is disabled on step 0; enabled on all other steps
5. `currentStep` is clamped to `[0, steps.length - 1]`

## Usage (L4 example)

```tsx
import WizardPage from 'templates/L3/pages/wizard/[step]/page'

export default async function OnboardingRoute({
  params,
}: {
  params: { step: string }
}) {
  const stepIdx = Math.max(0, Number(params.step) - 1)
  return (
    <WizardPage
      steps={[
        { title: 'Profile', content: <ProfileStep /> },
        { title: 'Preferences', content: <PreferencesStep /> },
        { title: 'Review', content: <ReviewStep /> },
      ]}
      initialStep={stepIdx}
      onStepChange={(s) => router.push(`/onboarding/${s + 1}`)}
      onComplete={() => router.push('/dashboard')}
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports (uses Tailwind utility classes)
- **L2**: Receives step content via `steps[].content` ReactNode slots
- **L4**: Provides step content components; handles URL sync via `onStepChange`
