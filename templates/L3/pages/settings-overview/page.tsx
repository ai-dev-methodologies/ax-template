/*
---
template_id: L3/pages/settings-overview
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: internal_design
    rationale: "Generic settings overview skeleton with named section slots (account / security / notifications / billing). Each section renders a card with a title and a ReactNode slot. L4 fills in the actual section content. Extra sections can be added via the sections prop."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
import * as React from 'react'

/**
 * SettingsOverviewPage — generic settings overview skeleton.
 *
 * Named section slots:
 *   - accountSlot       (optional) account / profile settings content
 *   - securitySlot      (optional) security settings (password, MFA, sessions)
 *   - notificationsSlot (optional) notification preferences
 *   - billingSlot       (optional) billing / subscription content
 *
 * Additional props:
 *   - title       (optional) page heading (default: "Settings")
 *   - description (optional) page subtitle
 *   - sections    (optional) extra sections in addition to the four named slots
 *
 * L4 usage:
 *   import SettingsOverviewPage from 'templates/L3/pages/settings-overview/page'
 *   export default function SettingsRoute() {
 *     return (
 *       <SettingsOverviewPage
 *         accountSlot={<AccountSection />}
 *         securitySlot={<SecuritySection />}
 *         notificationsSlot={<NotificationsSection />}
 *         billingSlot={<BillingSection />}
 *       />
 *     )
 *   }
 */
export interface SettingsSection {
  id: string
  title: string
  description?: string
  content: React.ReactNode
}

export interface SettingsOverviewPageProps {
  /** Page heading (default: "Settings") */
  title?: string
  /** Optional subtitle */
  description?: string
  /** Account / profile settings section */
  accountSlot?: React.ReactNode
  /** Security settings section (password, MFA, active sessions) */
  securitySlot?: React.ReactNode
  /** Notification preference section */
  notificationsSlot?: React.ReactNode
  /** Billing / subscription section */
  billingSlot?: React.ReactNode
  /** Extra sections beyond the four named ones */
  sections?: SettingsSection[]
}

interface SectionCardProps {
  title: string
  description?: string
  children: React.ReactNode
}

function SectionCard({ title, description, children }: SectionCardProps) {
  return (
    <section
      aria-labelledby={`settings-section-${title.toLowerCase().replace(/\s+/g, '-')}`}
      className="rounded-lg border bg-card shadow-sm"
    >
      <div className="px-6 py-4 border-b">
        <h2
          id={`settings-section-${title.toLowerCase().replace(/\s+/g, '-')}`}
          className="text-base font-semibold"
        >
          {title}
        </h2>
        {description && (
          <p className="text-sm text-muted-foreground mt-0.5">{description}</p>
        )}
      </div>
      <div className="px-6 py-4">{children}</div>
    </section>
  )
}

const NAMED_SECTIONS = [
  { key: 'accountSlot' as const, title: 'Account', description: 'Manage your profile and account details' },
  { key: 'securitySlot' as const, title: 'Security', description: 'Password, two-factor authentication, and active sessions' },
  { key: 'notificationsSlot' as const, title: 'Notifications', description: 'Email and push notification preferences' },
  { key: 'billingSlot' as const, title: 'Billing', description: 'Subscription plan and payment methods' },
] as const

export default function SettingsOverviewPage({
  title = 'Settings',
  description,
  accountSlot,
  securitySlot,
  notificationsSlot,
  billingSlot,
  sections = [],
}: SettingsOverviewPageProps) {
  const slotMap: Record<typeof NAMED_SECTIONS[number]['key'], React.ReactNode> = {
    accountSlot,
    securitySlot,
    notificationsSlot,
    billingSlot,
  }

  return (
    <main className="container mx-auto px-4 py-8 max-w-3xl space-y-6">
      {/* Page header */}
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </div>

      {/* Named section slots */}
      <div className="space-y-4">
        {NAMED_SECTIONS.map(({ key, title: sectionTitle, description: sectionDesc }) => {
          const content = slotMap[key]
          if (!content) return null
          return (
            <SectionCard key={key} title={sectionTitle} description={sectionDesc}>
              {content}
            </SectionCard>
          )
        })}

        {/* Extra sections */}
        {sections.map((section) => (
          <SectionCard
            key={section.id}
            title={section.title}
            description={section.description}
          >
            {section.content}
          </SectionCard>
        ))}
      </div>
    </main>
  )
}
