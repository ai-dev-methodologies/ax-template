---
snapshot_id: input-otp-2026-05
source: "https://input-otp.rodz.dev/"
fetched_at: "2026-05-18T00:00:00Z"
via: WebFetch
bytes: 1390
sha: "c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4"
tier: 3
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
