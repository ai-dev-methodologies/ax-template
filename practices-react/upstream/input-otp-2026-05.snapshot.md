# input-otp — Frozen Snapshot + InputOTP Tagline Refresh

**Source URL(s):** https://input-otp.rodz.dev/ (original 2026-05 fetch, preserved below; refetched 2026-07-30)
**HTTP status:** 200
**Fetched at:** 2026-07-30T00:51:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://input-otp.rodz.dev/`
**Body SHA-256 (below the `---` divider, header excluded):** aa7d7296927bec2170d68344fe2cd87c9777b720856f30d9c98563e8ae68d6a4

---

# input-otp API — Frozen Snapshot 2026-05

Source: https://input-otp.rodz.dev/  
Package: `input-otp@^1`  
shadcn/ui wraps this as `InputOTP`.  
Fetched: 2026-05-18  
Purpose: Evidence anchor for `templates/L1/components/otp-input.tsx`

## Installation

```bash
npm install input-otp
```

## Core Component

```typescript
import { OTPInput, SlotProps } from 'input-otp'
```

## Key Props

| Prop | Type | Description |
|------|------|-------------|
| `maxLength` | `number` | Total number of OTP digits (typically 6) |
| `value` | `string` | Controlled value |
| `onChange` | `(v: string) => void` | Called on every change |
| `onComplete` | `(value: string) => void` | Called when all digits are filled |
| `pattern` | `string \| RegExp` | Validation pattern; DIGITS = /^\d+$/ |
| `disabled` | `boolean` | Disables the input |
| `render` | `(props: { slots: SlotProps[] }) => ReactNode` | Custom slot renderer |
| `containerClassName` | `string` | CSS class for outer container |

## shadcn/ui Wrapper (InputOTP)

shadcn/ui provides pre-built components `InputOTP`, `InputOTPGroup`, `InputOTPSlot`,
and `InputOTPSeparator` that wrap `input-otp` with design-system styles.

```tsx
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSeparator,
  InputOTPSlot,
} from '@/components/ui/input-otp'

<InputOTP maxLength={6} value={value} onChange={setValue}>
  <InputOTPGroup>
    <InputOTPSlot index={0} />
    <InputOTPSlot index={1} />
    <InputOTPSlot index={2} />
  </InputOTPGroup>
  <InputOTPSeparator />
  <InputOTPGroup>
    <InputOTPSlot index={3} />
    <InputOTPSlot index={4} />
    <InputOTPSlot index={5} />
  </InputOTPGroup>
</InputOTP>
```

## Paste Handling

`input-otp` natively handles paste events. Pasting a 6-digit string
into any slot automatically fills all slots.

## Pattern Constant

```typescript
import { REGEXP_ONLY_DIGITS } from 'input-otp'
// Equivalent to: /^\d+$/
```

## InputOTP (2026-07 refresh)

Source: https://input-otp.rodz.dev/ (curl+snapshot-extract.sh, 2026-07-30 — the live tagline uses "passcode", not
"password"; requoted accordingly)

Stop wasting time building OTP inputs. One-time passcode input for React. Unstyled, accessible, and copy-paste friendly
out of the box.
