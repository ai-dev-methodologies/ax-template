import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import {
  RateLimitBannerProvider,
  useRateLimitBanner,
  extractRetryAfterFrom429,
  parseRetryAfter,
} from '../../templates/L2/blocks/rate-limit-banner'

// S2.OBSERVABILITY-LIMITS.FE closure (practices-react/rules/rate-limit-must-surface-to-user.md):
// rate-limit-banner.tsx (R56) shipped as a real L2 primitive, but before this file nothing
// exercised it at runtime — a refactor that broke `notify429` propagation, the countdown, or
// Retry-After parsing could regress silently. This renders the REAL block (not a mock) against
// simulated 429 responses, mirroring the fetch-wrapper usage the new rule mandates.
//
// Import convention follows locale-format.vitest.tsx / fmw2-primitives.vitest.ts /
// parse-error-denylist.vitest.ts: templates/L2/blocks/rate-limit-banner.tsx is not (yet)
// re-exported through the @ax/blocks package, so it is imported by its repo-root path.

// Mirrors the "Correct" fetch-wrapper shape from the rule body: only a real
// 429 triggers notify429. A non-429 response must be a no-op here, exactly
// like extractRetryAfterFrom429's own status !== 429 short-circuit — this
// harness intentionally re-checks res.status itself (not just relying on the
// helper's null return) so the "non-429 never triggers" test proves the
// mandated call-site GATE, not merely that null defers to the fallback.
function Harness({ res, source }: { res: Response; source?: string }) {
  const { notify429 } = useRateLimitBanner()
  return (
    <button
      data-testid="trigger-429"
      onClick={() => {
        if (res.status === 429) {
          notify429(extractRetryAfterFrom429(res), source)
        }
      }}
    >
      Fire
    </button>
  )
}

function make429(headers: Record<string, string> = {}): Response {
  return new Response(null, { status: 429, headers })
}

afterEach(() => {
  vi.useRealTimers()
})

describe('rate-limit-banner — a 429 must reach the user (RFC 6585 §4 / RFC 9110 §10.2.3)', () => {
  it('a 429 with Retry-After renders a visible role=status banner naming the wait, not a generic error', () => {
    render(
      <RateLimitBannerProvider>
        <Harness res={make429({ 'Retry-After': '3' })} source="order submission" />
      </RateLimitBannerProvider>,
    )

    fireEvent.click(screen.getByTestId('trigger-429'))

    const banner = screen.getByRole('status')
    expect(banner).toHaveTextContent('Too many requests on order submission — retry in 3s')
  })

  it('a 429 without Retry-After still surfaces the banner via the unknownDurationSec fallback', () => {
    render(
      <RateLimitBannerProvider unknownDurationSec={45}>
        <Harness res={make429()} />
      </RateLimitBannerProvider>,
    )

    fireEvent.click(screen.getByTestId('trigger-429'))

    expect(screen.getByRole('status')).toHaveTextContent('Too many requests — retry in 45s')
  })

  it('a non-429 response never triggers the banner (the harness\'s own 429-only gate blocks notify429 before extractRetryAfterFrom429 would even run)', () => {
    render(
      <RateLimitBannerProvider>
        <Harness res={new Response(null, { status: 200 })} />
      </RateLimitBannerProvider>,
    )

    fireEvent.click(screen.getByTestId('trigger-429'))

    expect(screen.queryByRole('status')).toBeNull()
  })

  it('the countdown decrements each second, shows the retry-now state, then auto-dismisses', () => {
    vi.useFakeTimers()
    render(
      <RateLimitBannerProvider>
        <Harness res={make429({ 'Retry-After': '2' })} />
      </RateLimitBannerProvider>,
    )

    fireEvent.click(screen.getByTestId('trigger-429'))
    expect(screen.getByRole('status')).toHaveTextContent('retry in 2s')

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByRole('status')).toHaveTextContent('retry in 1s')

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByRole('status')).toHaveTextContent('you can retry now')

    act(() => {
      vi.advanceTimersByTime(1500)
    })
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('Dismiss removes the banner immediately, without waiting for the countdown', () => {
    render(
      <RateLimitBannerProvider>
        <Harness res={make429({ 'Retry-After': '30' })} />
      </RateLimitBannerProvider>,
    )

    fireEvent.click(screen.getByTestId('trigger-429'))
    expect(screen.getByRole('status')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }))

    expect(screen.queryByRole('status')).toBeNull()
  })
})

// P3-62 closure: parseRetryAfter's HTTP-date branch and extractRetryAfterFrom429's
// own status !== 429 short-circuit were previously untested (only the delta-seconds
// branch and the component-level tests above, which never invoke
// extractRetryAfterFrom429 for a non-429 response — see the harness's own gate).
describe('parseRetryAfter / extractRetryAfterFrom429 — direct unit coverage (RFC 9110 §10.2.3)', () => {
  it('parses the HTTP-date form of Retry-After relative to a fixed clock', () => {
    const fixedNow = new Date('Wed, 21 Oct 2026 07:27:00 GMT')
    const seconds = parseRetryAfter('Wed, 21 Oct 2026 07:28:00 GMT', fixedNow)
    expect(seconds).toBe(60)
  })

  it('extractRetryAfterFrom429 returns null for a non-429 response via its own status check, independent of any caller gate', () => {
    // Retry-After is deliberately set on this 200 response: if the `res.status !== 429`
    // branch were ever deleted, extractRetryAfterFrom429 would fall through to
    // parseRetryAfter('120') and return 120 instead of null, so this assertion would
    // catch the regression. A 200 with no Retry-After header would pass either way.
    const res = new Response(null, { status: 200, headers: { 'Retry-After': '120' } })
    expect(extractRetryAfterFrom429(res)).toBeNull()
  })
})
