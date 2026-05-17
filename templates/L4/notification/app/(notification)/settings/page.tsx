/*
---
template_id: L4/notification/app/(notification)/settings/page
layer: L4
domain: notification
domain_mode: full_trio
backend_operation_id: getNotificationPreferences
evidence:
  - source_type: internal
    rationale: "L4 notification vertical — SETTINGS page for per-user channel preferences. Loads current prefs via GET getNotificationPreferences; submits PATCH updateNotificationPreferences."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for server mutations with optimistic updates"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

// ─── types ───────────────────────────────────────────────────────────────────

interface NotificationPreferences {
  userId: string
  inAppEnabled: boolean
  emailEnabled: boolean
  updatedAt: string | null
}

// ─── API helpers ─────────────────────────────────────────────────────────────

async function fetchPreferences(): Promise<NotificationPreferences> {
  const res = await fetch('/api/notifications/preferences', {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) throw new Error('Failed to load preferences')
  return res.json() as Promise<NotificationPreferences>
}

async function updatePreferences(
  patch: Partial<Pick<NotificationPreferences, 'inAppEnabled' | 'emailEnabled'>>
): Promise<NotificationPreferences> {
  const res = await fetch('/api/notifications/preferences', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(patch),
  })
  if (!res.ok) throw new Error('Failed to update preferences')
  return res.json() as Promise<NotificationPreferences>
}

// ─── toggle row ───────────────────────────────────────────────────────────────

interface ToggleRowProps {
  id: string
  label: string
  description: string
  checked: boolean
  disabled?: boolean
  onChange: (checked: boolean) => void
}

function ToggleRow({ id, label, description, checked, disabled, onChange }: ToggleRowProps) {
  return (
    <div className="flex items-start justify-between gap-4 py-4 border-b last:border-0">
      <div className="flex-1">
        <label htmlFor={id} className="text-sm font-medium leading-none cursor-pointer">
          {label}
        </label>
        <p className="mt-1 text-xs text-muted-foreground">{description}</p>
      </div>
      <button
        id={id}
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={[
          'relative inline-flex h-6 w-11 shrink-0 rounded-full border-2 border-transparent',
          'transition-colors duration-200 ease-in-out focus-visible:outline-none',
          'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-50',
          checked ? 'bg-primary' : 'bg-muted',
        ].join(' ')}
      >
        <span
          aria-hidden="true"
          className={[
            'pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow-lg',
            'ring-0 transition duration-200 ease-in-out',
            checked ? 'translate-x-5' : 'translate-x-0',
          ].join(' ')}
        />
      </button>
    </div>
  )
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * NotificationSettingsPage — per-user notification channel preferences.
 *
 * Backend bindings:
 *   GET  /api/notifications/preferences → getNotificationPreferences
 *   PATCH /api/notifications/preferences → updateNotificationPreferences
 *
 * UX policy (notification-ui-manifest.yaml#settings):
 *   - Optimistic updates: toggle state changes immediately; reverts on error.
 *   - Partial update: only the changed field is sent in the PATCH body.
 *   - Defaults: if no preferences row exists yet, all channels shown as enabled.
 *
 * Fork instructions:
 *   1. Add more preference fields (PROMOTION, REMINDER channels) by extending
 *      the ToggleRow list and the PATCH payload shape.
 *   2. Wire to ToastQueue (L2 toast-queue block) for success/error feedback.
 *   3. Replace fetch() with your API client / tRPC call.
 */
export default function NotificationSettingsPage() {
  const queryClient = useQueryClient()

  const { data: prefs, isLoading } = useQuery<NotificationPreferences>({
    queryKey: ['notification-preferences'],
    queryFn: fetchPreferences,
  })

  const mutation = useMutation({
    mutationFn: updatePreferences,
    onMutate: async (patch) => {
      // Optimistic update
      await queryClient.cancelQueries({ queryKey: ['notification-preferences'] })
      const prev = queryClient.getQueryData<NotificationPreferences>(['notification-preferences'])
      queryClient.setQueryData(['notification-preferences'], (old: NotificationPreferences | undefined) =>
        old ? { ...old, ...patch } : old
      )
      return { prev }
    },
    onError: (_err, _patch, context) => {
      // Revert optimistic update on error
      if (context?.prev) {
        queryClient.setQueryData(['notification-preferences'], context.prev)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-preferences'] })
    },
  })

  if (isLoading) {
    return (
      <div className="mx-auto max-w-lg py-6 px-4" aria-busy="true">
        <div className="h-8 w-48 animate-pulse rounded bg-muted mb-6" />
        <div className="space-y-4">
          {[1, 2].map((i) => (
            <div key={i} className="h-16 animate-pulse rounded bg-muted" />
          ))}
        </div>
      </div>
    )
  }

  const inAppEnabled = prefs?.inAppEnabled ?? true
  const emailEnabled = prefs?.emailEnabled ?? true

  return (
    <div className="mx-auto max-w-lg py-6 px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">Notification Settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Choose which channels deliver your notifications.
        </p>
      </div>

      <div className="rounded-lg border bg-card p-4">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-2">
          Delivery channels
        </h2>

        <ToggleRow
          id="pref-inapp"
          label="In-app notifications"
          description="Show notifications in your notification inbox."
          checked={inAppEnabled}
          disabled={mutation.isPending}
          onChange={(checked) => mutation.mutate({ inAppEnabled: checked })}
        />

        <ToggleRow
          id="pref-email"
          label="Email notifications"
          description="Receive notifications by email when important events occur."
          checked={emailEnabled}
          disabled={mutation.isPending}
          onChange={(checked) => mutation.mutate({ emailEnabled: checked })}
        />
      </div>

      {mutation.isError && (
        <p role="alert" className="mt-4 text-sm text-destructive">
          Failed to save preferences. Please try again.
        </p>
      )}
    </div>
  )
}
