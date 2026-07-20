/// <reference types="@testing-library/jest-dom/vitest" />
import { readFileSync } from 'fs'
import { join } from 'path'
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import AuditLogDetailView, {
  type AuditLogDetail,
} from '../../templates/L4/audit-log/app/(audit-log)/[id]/audit-log-detail-view'

// S2.AUDIT-PII.XB — FE render leg of the redacted-actorIp closure. Reads the
// SAME golden fixture as
// backend/src/test/java/.../auditlog/AuditLogResponseRedactionGoldenTest.java
// (frontend/tests/_fixtures/audit-log-response.golden.json), which that BE
// test builds by running a raw IP through the REAL AuditLogPiiRedactor and
// serializing the resulting AuditLogResponse — one committed golden, two
// independent consumers. A regression in either the redactor's masking
// format or this render path trips exactly one of the two, never silently.
//
// Renders AuditLogDetailView DIRECTLY (the pure props->JSX component
// extracted from (audit-log)/[id]/page.tsx for exactly this reason — see
// that file's frontmatter). The page itself (useRouter + useQuery) is NOT
// unit-renderable from this vitest project without a shared-config
// resolve.alias for the `@tanstack/react-query` bare specifier when imported
// from a templates/L4/... file living outside frontend/ (reproduced
// empirically: Vite's import-analysis fails to resolve it — the SAME class
// of gap frontend/tests/L2/search-palette-hydration.spec.ts already
// documents for `cmdk`). Editing vitest.config.ts is out of scope for this
// change (shared config) — see the audit report for the L4-render-
// testability census this reproduction fed.

const RAW_IP = '203.0.113.42'
const MASKED_IP = '203.0.113.xxx'

function readGolden(): Record<string, unknown> {
  const raw = readFileSync(join(process.cwd(), 'tests/_fixtures/audit-log-response.golden.json'), 'utf8')
  return JSON.parse(raw)
}

/**
 * Adapts the BE `AuditLogResponse` golden shape to the FE `AuditLogDetail`
 * props shape. The golden has no `metadata` key (AuditLogResponse never
 * serializes one — a separate, pre-existing FE/BE field mismatch this test
 * does not attempt to fix), so it is filled in as `null` here, matching the
 * "no metadata" branch AuditLogDetailView already renders correctly.
 */
function toDetail(golden: Record<string, unknown>): AuditLogDetail {
  return {
    id: golden.id as string,
    actorId: golden.actorId as string,
    actorIp: golden.actorIp as string | null,
    action: golden.action as string,
    resourceType: golden.resourceType as string,
    resourceId: golden.resourceId as string | null,
    outcome: golden.outcome as 'SUCCESS' | 'FAILURE',
    timestamp: golden.timestamp as string,
    metadata: null,
    correlationId: golden.correlationId as string | null,
    userAgent: golden.userAgent as string | null,
  }
}

describe('AuditLogDetailView — redacted actorIp survives into the rendered DOM (S2.AUDIT-PII.XB)', () => {
  it('golden fixture itself carries the masked IP, never the raw one (sanity)', () => {
    const golden = readGolden()
    expect(golden.actorIp).toBe(MASKED_IP)
    expect(golden.actorIp).not.toBe(RAW_IP)
  })

  it('renders the MASKED IP from the real BE-shaped golden fixture; the RAW IP is absent from the DOM', () => {
    const golden = readGolden()
    render(<AuditLogDetailView data={toDetail(golden)} />)

    expect(screen.getByText(MASKED_IP)).toBeInTheDocument()
    expect(screen.queryByText(RAW_IP)).not.toBeInTheDocument()
    expect(document.body.textContent).not.toContain(RAW_IP)
  })

  it('NON-VACUITY: a planted raw IP DOES surface in the DOM — proves the raw-absent assertion above is capable of going RED, not vacuously passing', () => {
    // Simulates a redaction-bypass regression: the API returned the raw IP
    // (e.g. AuditLogPiiRedactor.redactIp stopped masking, or a write path
    // forgot to call it — the exact class of bug AUDIT-PII-001 guards).
    const golden = readGolden()
    const withRawIpPlanted: AuditLogDetail = {
      ...toDetail(golden),
      actorIp: RAW_IP,
    }

    render(<AuditLogDetailView data={withRawIpPlanted} />)

    // If this failed, the "raw absent" assertion above would be meaningless
    // vacuity (e.g. the actorIp field silently never reaching the DOM at
    // all, for any value) — this proves the field DOES reach the DOM when
    // present, so its absence in the masked-fixture test is a real signal.
    expect(screen.getByText(RAW_IP)).toBeInTheDocument()
  })

  it('the mutated (raw-planted) render does NOT also show the masked form — the two states are mutually exclusive, not both-present noise', () => {
    const golden = readGolden()
    const withRawIpPlanted: AuditLogDetail = {
      ...toDetail(golden),
      actorIp: RAW_IP,
    }
    render(<AuditLogDetailView data={withRawIpPlanted} />)
    expect(screen.queryByText(MASKED_IP)).not.toBeInTheDocument()
  })
})
