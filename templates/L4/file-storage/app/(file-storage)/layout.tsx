/*
---
template_id: L4/file-storage/app/(file-storage)/layout
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 file-storage vertical — route group layout with AppShell (sidebar + header)."
  - source_type: external
    citation: "Next.js 15 App Router — route groups and layouts"
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
provenance_class: internal_design
imports_from: [L2]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
import React from 'react'
import AppShell from 'templates/L2/blocks/app-shell'
import AppHeader from 'templates/L2/blocks/app-header'
import Sidebar from 'templates/L2/blocks/sidebar'

interface FileStorageLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/upload', label: 'Upload', icon: 'upload-cloud' as const },
  { href: '/files', label: 'My Files', icon: 'folder' as const },
]

/**
 * FileStorageLayout — route group layout for all file-storage pages.
 *
 * Fork instructions:
 *   1. Update NAV_ITEMS to match your file management navigation.
 *   2. Add storage quota progress bar to Sidebar footer.
 *   3. Add user profile / logout action to AppHeader.
 */
export default function FileStorageLayout({ children }: FileStorageLayoutProps) {
  return (
    <AppShell
      sidebarSlot={
        <Sidebar
          navItems={NAV_ITEMS}
          title="Files"
        />
      }
      headerSlot={
        <AppHeader title="File Storage" />
      }
    >
      {children}
    </AppShell>
  )
}
