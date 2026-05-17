# L4 / file-storage

**ax-template SP18** — File Storage domain reference workload.

## Overview

A full-stack file management vertical: drag-and-drop upload, virus scan lifecycle,
presigned URL download, per-user quota enforcement, and delete with IDOR protection.

Spec Trio anchors: `specs/file-storage-l0.yaml` (12 backend items) ·
`specs/file-storage-frontend-l0.yaml` (10 frontend items) ·
`contracts/file-storage-openapi.yaml` · `blueprints/file-storage-manifest.yaml`

## Pages

| Route | Op ID | Description |
|---|---|---|
| `/(file-storage)/upload` | `uploadFile` | Drag-and-drop upload via L1 FileDropzone |
| `/(file-storage)/files` | `listFiles` | Paginated file list with DataTable |
| `/(file-storage)/files/[id]` | `getFile` + `downloadFile` | Detail, download, poll PENDING |

## How to fork this template

1. **copy** `templates/L4/file-storage/` into your project as `app/`:
   ```bash
   cp -r templates/L4/file-storage/app ./app
   cp templates/L4/file-storage/next.config.ts ./next.config.ts
   ```

2. **Install dependencies**:
   ```bash
   npm install @tanstack/react-query react-dropzone lucide-react
   ```

3. **Wire backend**: Start `ax-template` backend on port 8080 or update the `rewrites()`
   destination in `next.config.ts`.

4. **Copy L1 FileDropzone**:
   ```bash
   cp templates/L1/components/file-dropzone.tsx src/components/
   cp templates/L1/lib/utils.ts src/lib/
   ```

5. **Copy L2 blocks** used by these pages:
   - `data-table` — file list table
   - `empty-state` — zero-files placeholder
   - `confirm-dialog` — delete confirmation modal
   - `error-boundary` — upload error wrapper
   - `toast-queue` — success/error toasts
   - `app-shell` + `app-header` + `sidebar` — navigation shell

6. **Run**:
   ```bash
   npm run dev
   ```

## Domain-specific spec requirements

| Spec ID | Requirement | Implementation |
|---|---|---|
| FILE-UPLOAD-001 | MIME allowlist | `FileValidationService.java` |
| FILE-UPLOAD-002 | 100 MB max size | `MultipartConfig.java` |
| FILE-UPLOAD-003 | Filename sanitization | `FilenameSanitizer.java` |
| FILE-SCAN-001 | Virus scan lifecycle | `VirusScanService.java` (async) |
| FILE-SCAN-002 | PENDING → 202 + Retry-After | `FileStorageController.java` |
| FILE-QUOTA-001 | 1 GB per-user quota | `FileStorageService.java` |
| FILE-SEC-001 | Presigned URLs only (no storage key) | `PresignedUrlService.java` |
| FILE-AUTHZ-002 | IDOR → 404 | `FileStorageRepository.findByIdAndOwnerUserId` |
| FILE-FE-RENDER-002 | FileDropzone (L1) in upload page | `upload/page.tsx` |
| FILE-FE-ERROR-002 | Poll PENDING status every 3s | `files/[id]/page.tsx` |

## Backend templates

`templates/backend/file-storage/` contains:
- `StoredFile.java` — JPA entity
- `FileStatus.java` — lifecycle enum
- `FileStorageRepository.java` — JPA repository
- `FileStorageDto.java` — request/response records (storageKey excluded)
- `FileStorageService.java` — business logic
- `FileStorageController.java` — REST endpoints
- `FileValidationService.java` — MIME allowlist
- `MultipartConfig.java` — 100 MB limit at Tomcat layer
- `PresignedUrlService.java` — HMAC-SHA256 signed tokens
