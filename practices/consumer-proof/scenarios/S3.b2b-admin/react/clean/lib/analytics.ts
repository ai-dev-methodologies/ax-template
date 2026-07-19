// HAND-ROLLED — capability-gap signal. No L0/L2 catalog asset provides a
// client-side analytics/telemetry wrapper (checked: `grep -rli analytics
// templates/L0 templates/L2` → only bulk-export.tsx, an unrelated block).
// This is the minimal stub the scenario pages call into; the invariant this
// scenario proves lives in the CALLER's payload shape, not in this stub.

export const analytics = {
  track(event: string, payload: Record<string, unknown>) {
    // In a real fork-receiver this forwards to the configured analytics
    // vendor (PostHog / Amplitude / GA). The scenario only exercises the
    // call-site shape, so this stub is a no-op.
    void event
    void payload
  },
}
