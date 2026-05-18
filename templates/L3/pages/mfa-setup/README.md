# mfa-setup — L3 MFA Setup Page Template

Generic TOTP MFA setup skeleton. Accepts QR code and OTP input as ReactNode slots
from L4. No TOTP generation or validation logic — L4 generates the QR URI and
validates the OTP code via the backend. Uses L1 `OtpInput` component (SP14) passed
as a slot.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `qrCodeSlot` | `ReactNode` | ✅ | QR code image/SVG rendered by L4 |
| `otpSlot` | `ReactNode` | ✅ | OTP input — L4 passes L1 `<OtpInput />` with its own state |
| `onConfirm` | `() => void \| Promise<void>` | — | Called when user clicks "Verify & Enable" |
| `secretKey` | `string` | — | Manual-entry fallback key (displayed below QR code) |
| `backHref` | `string` | — | Cancel/back link href |

## Behaviour

1. L4 generates TOTP secret + QR URI and renders `<img>` into `qrCodeSlot`
2. L4 controls OTP state via L1 `OtpInput` and passes it as `otpSlot`
3. User scans QR code, enters 6-digit code
4. User clicks "Verify & Enable" → `onConfirm()` is awaited
5. L4 handles validation and success/error feedback externally

## Usage (L4 example)

```tsx
import MfaSetupPage from 'templates/L3/pages/mfa-setup/page'
import { OtpInput } from 'templates/L1/components/otp-input'

export default function MfaSetupRoute() {
  const [otp, setOtp] = React.useState('')
  const { qrUri, secret } = useTotpSetup() // L4 hook

  async function handleConfirm() {
    await api.post('/auth/mfa/verify', { otp })
    router.push('/dashboard')
  }

  return (
    <MfaSetupPage
      qrCodeSlot={<img src={qrUri} alt="MFA QR code" width={180} height={180} />}
      otpSlot={<OtpInput length={6} value={otp} onChange={setOtp} />}
      secretKey={secret}
      onConfirm={handleConfirm}
      backHref="/settings/security"
    />
  )
}
```

## Layer dependencies

- **L1**: Accepts L1 `OtpInput` via `otpSlot` (SP14)
- **L2**: No L2 blocks imported directly
- **L4**: Generates TOTP secret/QR URI; owns OTP state; validates via backend
