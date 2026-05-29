/**
 * L3 render-contract.spec.ts — TDD anchor (SP6 + SP20)
 *
 * RED phase:  fails before templates/L3/pages/ templates are written.
 * GREEN phase: passes after all L3 page-template families are written.
 *
 * Strategy: file-existence + export-shape checks (no DOM renderer required)
 * so the test works without Next.js server context. Each family must:
 *   1. Exist at the expected path
 *   2. Export a React component as `default`
 *   3. Have a sibling README.md with a `## Slot contract` section
 *
 * SP20 additions:
 *   - 8 new L3 families (auth extras + wizard + settings + import/export)
 *   - Behavioral tests using @testing-library/react for interactive pages
 */
import { describe, it, expect, vi } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'
import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const L3_PAGES = path.join(REPO_ROOT, 'templates/L3/pages')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

function hasDefaultExport(src: string): boolean {
  return /export\s+default\s+/.test(src)
}

function hasSlotContractSection(readme: string): boolean {
  return /^#{1,3}\s+(Slots|Slot contract)/im.test(readme)
}

// ─── family registry ────────────────────────────────────────────────────────

interface Family {
  dir: string
  mainFiles: string[]
  description: string
}

const FAMILIES: Family[] = [
  {
    dir: 'list-page',
    mainFiles: ['page.tsx'],
    description: 'generic list view with filter + pagination slots',
  },
  {
    dir: 'detail-page',
    mainFiles: ['[id]/page.tsx'],
    description: 'generic detail view with section + action slots',
  },
  {
    dir: 'create-page',
    mainFiles: ['page.tsx'],
    description: 'generic create form with form slot',
  },
  {
    dir: 'edit-page',
    mainFiles: ['[id]/page.tsx'],
    description: 'generic edit form with form + delete slots',
  },
  {
    dir: 'dashboard-page',
    mainFiles: ['page.tsx'],
    description: 'generic dashboard with widget slots',
  },
  {
    dir: 'auth-callback-page',
    mainFiles: ['page.tsx'],
    description: 'OAuth / email-verify callback skeleton',
  },
  {
    dir: 'error-page',
    mainFiles: ['loading.tsx', 'not-found.tsx', 'error.tsx'],
    description: 'Next.js error-state bundle (loading, not-found, error)',
  },
  // SP20 — auth extras
  {
    dir: 'forgot-password',
    mainFiles: ['page.tsx'],
    description: 'forgot-password email-input → check-email confirmation (uses email-outbox backend)',
  },
  {
    dir: 'reset-password',
    mainFiles: ['[token]/page.tsx'],
    description: 'reset-password token-verify → password-input → submit',
  },
  {
    dir: 'mfa-setup',
    mainFiles: ['page.tsx'],
    description: 'MFA setup — QR code display + OTP confirm (uses L1 otp-input)',
  },
  {
    dir: 'account-locked',
    mainFiles: ['page.tsx'],
    description: 'account-locked — display lock reason + unlock instructions',
  },
  // SP20 — onboarding / settings
  {
    dir: 'wizard',
    mainFiles: ['[step]/page.tsx'],
    description: 'multi-step wizard with progress indicator and step-slot content',
  },
  {
    dir: 'settings-overview',
    mainFiles: ['page.tsx'],
    description: 'settings overview — section grid (account / security / notifications / billing)',
  },
  // SP20 — import / export
  {
    dir: 'import-csv',
    mainFiles: ['page.tsx'],
    description: 'file-dropzone + preview-mapping-validate-submit flow',
  },
  {
    dir: 'export-job-status',
    mainFiles: ['page.tsx'],
    description: 'long-running job polling + download link',
  },
]

// ─── suite ──────────────────────────────────────────────────────────────────

describe('L3 page template render-contract', () => {
  it('templates/L3/pages/ directory exists', () => {
    expect(
      fileExists(L3_PAGES),
      `templates/L3/pages/ must exist — create it in SP6`
    ).toBe(true)
  })

  it('templates/L3/pages/README.md exists with Slot contract section', () => {
    const readmePath = path.join(L3_PAGES, 'README.md')
    expect(fileExists(readmePath), `Missing: ${readmePath}`).toBe(true)
    const content = readFile(readmePath)
    expect(
      hasSlotContractSection(content),
      `${readmePath} must have a ## Slot contract section`
    ).toBe(true)
  })

  for (const family of FAMILIES) {
    const familyDir = path.join(L3_PAGES, family.dir)
    const readmePath = path.join(familyDir, 'README.md')

    describe(`family: ${family.dir}`, () => {
      it(`directory exists (${family.description})`, () => {
        expect(
          fileExists(familyDir),
          `Missing family dir: templates/L3/pages/${family.dir}/`
        ).toBe(true)
      })

      it(`README.md exists with Slot contract section`, () => {
        expect(fileExists(readmePath), `Missing: ${readmePath}`).toBe(true)
        const content = readFile(readmePath)
        expect(
          hasSlotContractSection(content),
          `${readmePath} must have a ## Slot contract section`
        ).toBe(true)
      })

      for (const mainFile of family.mainFiles) {
        const filePath = path.join(familyDir, mainFile)

        it(`${mainFile} exists`, () => {
          expect(
            fileExists(filePath),
            `Missing: templates/L3/pages/${family.dir}/${mainFile}`
          ).toBe(true)
        })

        it(`${mainFile} has a default export`, () => {
          const src = readFile(filePath)
          expect(
            hasDefaultExport(src),
            `${mainFile} must export a default React component`
          ).toBe(true)
        })

        it(`${mainFile} has evidence frontmatter comment`, () => {
          const src = readFile(filePath)
          expect(
            src.includes('template_id:') && src.includes('evidence:'),
            `${mainFile} must have template_id and evidence in frontmatter comment`
          ).toBe(true)
        })
      }
    })
  }
})

// ─── SP20 behavioral contract ─────────────────────────────────────────────────
// Uses dynamic imports so missing files fail per-test, not at module load time.

// ─── SP20 behavioral contract ─────────────────────────────────────────────────
// Uses /* @vite-ignore */ so Vite skips import-analysis; modules resolve at
// runtime — RED when files missing, GREEN after templates are implemented.

describe('SP20 behavioral contract', () => {
  describe('forgot-password', () => {
    it('renders email input and fires onSubmit with email value', async () => {
      // @ts-ignore dynamic path
      const { default: ForgotPasswordPage } = await import(
        /* @vite-ignore */ '../../../templates/L3/pages/forgot-password/page'
      )
      const onSubmit = vi.fn()
      render(React.createElement(ForgotPasswordPage, { onSubmit, loginHref: '/login' }))
      const emailInput = screen.getByRole('textbox')
      fireEvent.change(emailInput, { target: { value: 'user@example.com' } })
      fireEvent.submit(emailInput.closest('form')!)
      expect(onSubmit).toHaveBeenCalledWith('user@example.com')
    })
  })

  describe('reset-password', () => {
    it('renders password inputs and fires onSubmit with password value', async () => {
      // @ts-ignore dynamic path
      const { default: ResetPasswordPage } = await import(
        /* @vite-ignore */ '../../../templates/L3/pages/reset-password/[token]/page'
      )
      const onSubmit = vi.fn()
      render(React.createElement(ResetPasswordPage, { token: 'tok123', onSubmit, loginHref: '/login' }))
      const form = document.querySelector('form')!
      const pwInputs = form.querySelectorAll('input[type="password"]')
      fireEvent.change(pwInputs[0], { target: { value: 'newPass123!' } })
      fireEvent.change(pwInputs[1], { target: { value: 'newPass123!' } })
      fireEvent.submit(form)
      expect(onSubmit).toHaveBeenCalledWith('newPass123!')
    })
  })

  describe('wizard', () => {
    it('navigates from step 1 → 2 → 3 via Next button', async () => {
      // @ts-ignore dynamic path
      const { default: WizardPage } = await import(
        /* @vite-ignore */ '../../../templates/L3/pages/wizard/[step]/page'
      )
      const steps = [
        { title: 'Step 1', content: React.createElement('p', null, 'Content one') },
        { title: 'Step 2', content: React.createElement('p', null, 'Content two') },
        { title: 'Step 3', content: React.createElement('p', null, 'Content three') },
      ]
      render(React.createElement(WizardPage, { steps }))
      expect(screen.getByText('Content one')).toBeTruthy()

      fireEvent.click(screen.getByRole('button', { name: /next/i }))
      expect(screen.getByText('Content two')).toBeTruthy()

      fireEvent.click(screen.getByRole('button', { name: /next/i }))
      expect(screen.getByText('Content three')).toBeTruthy()
    })
  })

  describe('settings-overview', () => {
    it('renders all section slots', async () => {
      // @ts-ignore dynamic path
      const { default: SettingsOverviewPage } = await import(
        /* @vite-ignore */ '../../../templates/L3/pages/settings-overview/page'
      )
      render(
        React.createElement(SettingsOverviewPage, {
          accountSlot: React.createElement('div', null, 'account-section'),
          securitySlot: React.createElement('div', null, 'security-section'),
          notificationsSlot: React.createElement('div', null, 'notifications-section'),
          billingSlot: React.createElement('div', null, 'billing-section'),
        })
      )
      expect(screen.getByText('account-section')).toBeTruthy()
      expect(screen.getByText('security-section')).toBeTruthy()
      expect(screen.getByText('notifications-section')).toBeTruthy()
      expect(screen.getByText('billing-section')).toBeTruthy()
    })
  })
})
