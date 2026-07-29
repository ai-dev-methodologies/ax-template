/*
---
template_id: L1/components/address-search
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: external
    citation: "Kakao Postcode Widget API v2.0"
    url: "https://postcode.map.kakao.com/guide"
  - source_type: upstream_id
    upstream_id: kakao-postcode-2026-05
    section: oncomplete-callback
    quote: "The callback receives a data object with address information. Key properties: zonecode, roadAddress, jibunAddress"
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 — address fields labelled individually (우편번호, 도로명, 지번, 상세)"
  - "WCAG 2.2 SC 4.1.2 — search button role='button' with descriptive aria-label"
  - "WCAG 2.2 SC 2.4.3 — focus returns to trigger button after modal closes"
risks:
  - "Runtime script injection creates external network dependency"
  - "Mitigation: injector prop for DI; Playwright story uses mocked injector in CI"
dependencies: []
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#address-search"
---
*/
import * as React from 'react'
import { Search } from 'lucide-react'
import { cn } from '../lib/utils'
import { Button } from './button'
import { Input } from './input'
import { Label } from './label'

// ─── Kakao Postcode types ──────────────────────────────────────────────────

export interface KakaoPostcodeData {
  zonecode: string       // 5-digit postal code
  roadAddress: string    // 도로명주소
  jibunAddress: string   // 지번주소
  addressEnglish: string
  userSelectedType: 'R' | 'J'  // R=road, J=jibun
  buildingName: string
  bname: string          // 법정동명
}

export interface AddressValue {
  zonecode: string
  roadAddress: string
  jibunAddress: string
  detailAddress: string
}

// ─── Script injector (dependency-injected for testability) ─────────────────

export interface KakaoScriptInjector {
  /** Load the Kakao postcode script and resolve when ready */
  load: () => Promise<void>
  /** Open the Kakao postcode widget */
  open: (config: { oncomplete: (data: KakaoPostcodeData) => void }) => void
}

/** Default injector — loads the real Kakao CDN script */
export const defaultKakaoInjector: KakaoScriptInjector = {
  load(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof window === 'undefined') {
        resolve()
        return
      }
      const kakaoWindow = window as unknown as {
        kakao?: { Postcode: new (config: unknown) => { open: () => void } }
      }
      if (kakaoWindow.kakao?.Postcode) {
        resolve()
        return
      }
      const existing = document.querySelector(
        'script[src*="postcode.v2.js"]'
      )
      if (existing) {
        existing.addEventListener('load', () => resolve())
        existing.addEventListener('error', reject)
        return
      }
      const script = document.createElement('script')
      script.src =
        '//t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
      script.async = true
      script.onload = () => resolve()
      script.onerror = reject
      document.head.appendChild(script)
    })
  },

  open(config: { oncomplete: (data: KakaoPostcodeData) => void }): void {
    const kakaoWindow = window as unknown as {
      kakao: { Postcode: new (config: unknown) => { open: () => void } }
    }
    new kakaoWindow.kakao.Postcode(config).open()
  },
}

// ─── Component ─────────────────────────────────────────────────────────────

export interface AddressSearchProps {
  /** Current address value (controlled) */
  value?: AddressValue
  /** Called when address changes */
  onChange?: (value: AddressValue) => void
  /** Disables the component */
  disabled?: boolean
  /** Custom Kakao script injector (default: loads from CDN; override for tests) */
  injector?: KakaoScriptInjector
  /** Additional className for the root container */
  className?: string
  /** Labels configuration */
  labels?: {
    zonecode?: string
    roadAddress?: string
    jibunAddress?: string
    detailAddress?: string
    searchButton?: string
  }
  /** data-testid for the search button */
  searchButtonTestId?: string
}

const DEFAULT_VALUE: AddressValue = {
  zonecode: '',
  roadAddress: '',
  jibunAddress: '',
  detailAddress: '',
}

/**
 * AddressSearch — Korean enterprise address lookup via Kakao Postcode widget.
 *
 * Architecture:
 *   - Controlled component: value + onChange
 *   - Kakao script loaded lazily on first click (CDN injection)
 *   - `injector` prop for DI — mock injector used in Playwright/Vitest
 *   - Stores: zonecode, roadAddress, jibunAddress, detailAddress
 *
 * CI safety: Pass a mocked `injector` to avoid live network in tests.
 */
export function AddressSearch({
  value = DEFAULT_VALUE,
  onChange,
  disabled = false,
  injector = defaultKakaoInjector,
  className,
  labels = {},
  searchButtonTestId,
}: AddressSearchProps) {
  const {
    zonecode: zoneLabel = '우편번호',
    roadAddress: roadLabel = '도로명주소',
    jibunAddress: jibunLabel = '지번주소',
    detailAddress: detailLabel = '상세주소',
    searchButton: searchBtnLabel = '주소 검색',
  } = labels

  const triggerRef = React.useRef<HTMLButtonElement>(null)
  const [isLoading, setIsLoading] = React.useState(false)

  const handleSearch = React.useCallback(async () => {
    setIsLoading(true)
    try {
      await injector.load()
    } finally {
      setIsLoading(false)
    }

    injector.open({
      oncomplete(data: KakaoPostcodeData) {
        onChange?.({
          zonecode: data.zonecode,
          roadAddress: data.roadAddress,
          jibunAddress: data.jibunAddress,
          detailAddress: value.detailAddress, // preserve existing detail
        })
        // Return focus to the trigger button after modal closes (WCAG 2.4.3)
        requestAnimationFrame(() => triggerRef.current?.focus())
      },
    })
  }, [injector, onChange, value.detailAddress])

  const handleDetailChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange?.({ ...value, detailAddress: e.target.value })
    },
    [onChange, value]
  )

  return (
    <div className={cn('space-y-[--space-2]', className)}>
      {/* Zonecode row */}
      <div className="flex gap-[--space-2]">
        <div className="flex-1 space-y-[--space-1]">
          <Label htmlFor="address-zonecode">{zoneLabel}</Label>
          <Input
            id="address-zonecode"
            value={value.zonecode}
            readOnly
            placeholder="00000"
            className="bg-[--color-surface-subtle]"
            aria-label={zoneLabel}
          />
        </div>
        <div className="flex items-end">
          <Button
            ref={triggerRef}
            type="button"
            variant="outline"
            onClick={handleSearch}
            disabled={disabled || isLoading}
            aria-label={searchBtnLabel}
            data-testid={searchButtonTestId}
          >
            <Search className="mr-[--space-2] h-4 w-4" aria-hidden="true" />
            {isLoading ? '로딩…' : searchBtnLabel}
          </Button>
        </div>
      </div>

      {/* Road address */}
      <div className="space-y-[--space-1]">
        <Label htmlFor="address-road">{roadLabel}</Label>
        <Input
          id="address-road"
          value={value.roadAddress}
          readOnly
          placeholder="도로명주소가 여기에 표시됩니다"
          className="bg-[--color-surface-subtle]"
          aria-label={roadLabel}
        />
      </div>

      {/* Jibun address */}
      <div className="space-y-[--space-1]">
        <Label htmlFor="address-jibun">{jibunLabel}</Label>
        <Input
          id="address-jibun"
          value={value.jibunAddress}
          readOnly
          placeholder="지번주소가 여기에 표시됩니다"
          className="bg-[--color-surface-subtle]"
          aria-label={jibunLabel}
        />
      </div>

      {/* Detail address (user-editable) */}
      <div className="space-y-[--space-1]">
        <Label htmlFor="address-detail">{detailLabel}</Label>
        <Input
          id="address-detail"
          value={value.detailAddress}
          onChange={handleDetailChange}
          placeholder="건물명, 동/호수 등"
          disabled={disabled}
          aria-label={detailLabel}
        />
      </div>
    </div>
  )
}

export type { KakaoPostcodeData as KakaoAddressData }
