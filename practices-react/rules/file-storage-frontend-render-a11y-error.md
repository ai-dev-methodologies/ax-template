---
title: "File-storage UI must render the documented file surfaces with human-readable sizes, accessible dropzone + status, mapped error messages, and virtualized large lists"
rule_id: file-storage-frontend-render-a11y-error
impact: HIGH
impactDescription: "A file UI that renders raw byte counts is unreadable; a dropzone reachable only by mouse excludes keyboard users; status conveyed by color alone is invisible to color-blind and screen-reader users; an upload error shown as a generic 'failed' gives the user no recovery; an un-virtualized list of thousands of files freezes the tab. Each defect is a usability or accessibility failure of the file surface."
tags:
  - file-storage
  - frontend
  - accessibility
  - a11y
  - error-handling
  - rendering
applicable_to:
  - react
  - nextjs
spec_ref: "specs/file-storage-frontend-l0.yaml#FILE-FE-A11Y-002"
verification:
  type: review
  notes: |
    Reviewer confirms the file-storage UI against specs/file-storage-frontend-l0.yaml:
    RENDER — file list is a DataTable (name/type/size/status/uploaded-date); upload page is a
    FileDropzone (L1) with accepted MIME + size limit as props and a progress indicator; detail page
    shows metadata + a Download button; file size is rendered human-readable (formatBytes → '1.5 MB'),
    never a raw byte count. A11Y — the dropzone is keyboard operable (Tab focus, Space/Enter opens the
    picker); status badges pair a text label WITH color (never color alone); upload/quota errors are
    announced via an aria-live region. ERROR — upload failures map backend error types to specific
    messages (quota-exceeded / unsupported-type / too-large), not a generic 'failed'; a 202 scan-pending
    download shows a non-blocking message and polls. PERF — the list virtualizes beyond 50 rows
    (off-screen rows are not in the DOM).
evidence:
  - source_type: external
    citation: "WCAG 2.2 Success Criterion 1.4.1 Use of Color (Level A) — status badge needs a text label, not color alone (FILE-FE-A11Y-002)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html"
    quote: "Color is not used as the only visual means of conveying information, indicating an action, prompting a response, or distinguishing a visual element."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "WCAG 2.2 Success Criterion 4.1.3 Status Messages (Level AA) — upload/quota errors announced via an aria-live region (FILE-FE-A11Y-003)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quote: "In content implemented using markup languages, status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "RFC 9457 Problem Details — backend error types mapped to specific user-facing upload messages (FILE-FE-ERROR-001)"
    url: "https://www.rfc-editor.org/rfc/rfc9457"
    quote: "This document defines a 'problem detail' to carry machine-readable details of errors in HTTP response content to avoid the need to define new error response formats for HTTP APIs."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## File-storage UI must render documented surfaces with human-readable sizes, accessible dropzone + status, mapped errors, and virtualized lists

**Impact: HIGH — The file surface is where accessibility and error-handling defects hit users hardest. A size column showing `1572864` instead of `1.5 MB` is unreadable. A drop zone that only accepts a mouse drag excludes every keyboard and screen-reader user. A status pill that is only colored — green for READY, red for QUARANTINED — is invisible to a color-blind user and silent to a screen reader; WCAG 1.4.1 is explicit that *color is not used as the only visual means of conveying information ... or distinguishing a visual element*. An upload error shown as a generic "failed" tells the user nothing actionable, when the backend already said `quota-exceeded`. And a non-virtualized list of thousands of files locks the tab. This rule binds the file-storage-frontend contract across RENDER, A11Y, ERROR, and PERF.**

There are ten load-bearing requirements — the items of `specs/file-storage-frontend-l0.yaml`, all governed by this rule.

**RENDER.** The file list is a DataTable with the documented columns (name, type, size, status, uploaded date) (FILE-FE-RENDER-001). The upload page renders a FileDropzone (L1) with the accepted MIME types and size limit passed as props plus a progress indicator (FILE-FE-RENDER-002). The detail page renders the file metadata and a Download button (FILE-FE-RENDER-003). File size is rendered human-readable (`formatBytes(n)` → `'1.5 MB'`, `'320 KB'`), never a raw byte count (FILE-FE-RENDER-004).

**A11Y.** The FileDropzone is keyboard operable — Tab focuses it, Space/Enter opens the native file picker (WCAG 2.1.1 Keyboard) (FILE-FE-A11Y-001). Status badges (PENDING/READY/QUARANTINED) pair a text label WITH color — never color alone, per WCAG 1.4.1 (FILE-FE-A11Y-002). Upload and quota-exceeded errors are announced via an `aria-live` region so a screen-reader user is notified without a focus change, per WCAG 4.1.3 (FILE-FE-A11Y-003).

**ERROR.** Upload failures map backend error types to SPECIFIC user-facing messages — `quota-exceeded`, `unsupported-type`, `too-large` — read from the RFC 9457 problem `type`, not a generic "failed" (FILE-FE-ERROR-001). A download that returns `202` (scan pending) shows a non-blocking message and automatically polls until the scan completes (FILE-FE-ERROR-002).

**PERF.** The file list virtualizes beyond 50 rows — off-screen rows are not rendered into the DOM (FILE-FE-PERF-001).

**Incorrect — raw bytes, color-only status, generic error, mouse-only dropzone:**

```tsx
<td>{file.sizeBytes}</td>                                   {/* VIOLATION: raw bytes (FILE-FE-RENDER-004) */}
<span className={file.status === 'READY' ? 'text-green' : 'text-red'} />  {/* VIOLATION: color alone (FILE-FE-A11Y-002) */}
<div onDrop={handleDrop}>Drop files</div>                   {/* VIOLATION: no keyboard/picker (FILE-FE-A11Y-001) */}
catch (e) { setError('Upload failed'); }                    {/* VIOLATION: generic, unmapped (FILE-FE-ERROR-001) */}
```

**Correct — human-readable size, label+color status, aria-live errors, keyboard dropzone, mapped messages:**

```tsx
<td>{formatBytes(file.sizeBytes)}</td>                      {/* '1.5 MB' (FILE-FE-RENDER-004) */}
<StatusBadge status={file.status} />                        {/* renders icon + TEXT label + color (FILE-FE-A11Y-002) */}
<FileDropzone accept={ACCEPTED_MIME} maxSize={MAX}          {/* MIME/size as props (FILE-FE-RENDER-002) */}
  onSelect={upload} />                                      {/* Tab/Space/Enter opens picker (FILE-FE-A11Y-001) */}
<div role="alert" aria-live="assertive">{errorMessage}</div>{/* announced (FILE-FE-A11Y-003) */}
catch (e) { setError(messageForProblemType(e));            {/* quota-exceeded / unsupported-type / too-large (FILE-FE-ERROR-001) */}
}
// list: <VirtualizedTable rows={files} />  (virtualize > 50, FILE-FE-PERF-001)
// download 202 → non-blocking notice + poll (FILE-FE-ERROR-002)
```

Verification: review-tier. These are UI-contract + accessibility properties with no compile signal — a color-only badge and a raw-byte size render fine and fail real users. Verify by review against `specs/file-storage-frontend-l0.yaml`: documented surfaces render; sizes are human-readable; the dropzone is keyboard operable; status pairs label+color; errors announce via aria-live and map backend problem types to specific messages; a 202 download polls; the list virtualizes past 50 rows. When a fork-receiver wires a real component/a11y test (axe on the file list; keyboard-opens-picker; color-blind status has a label), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [WCAG 2.2 — Use of Color (1.4.1)](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html)

Reference: [WCAG 2.2 — Status Messages (4.1.3)](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)
