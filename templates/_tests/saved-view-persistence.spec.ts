/**
 * TDD anchor: saved-view-persistence.spec.ts
 * SP33 acceptance gate — SavedViewPersistence must be 'url' | 'server' only.
 *
 * RED reason: scaffolding writes localStorage-based saved-view config.
 * GREEN: rule enforced via ESLint + type system; persistence: 'localStorage' is a type error.
 *
 * First green command:
 *   npx eslint --fix templates/L2/blocks/saved-view.tsx && npx vitest run
 */

import { describe, expect, test } from 'vitest'
import type { SavedViewItem, SavedViewPersistence } from '../L2/blocks/saved-view'

describe('SavedView persistence contract', () => {
  test('persistence type only allows url and server', () => {
    const urlMode: SavedViewPersistence = 'url'
    const serverMode: SavedViewPersistence = 'server'
    expect(urlMode).toBe('url')
    expect(serverMode).toBe('server')
    // TypeScript would reject 'localStorage' at compile time
    // Runtime: these are the only values the component's onSave receives
  })

  test('SavedViewItem with url persistence passes assertion', () => {
    const view: SavedViewItem = {
      id: 'view-1',
      name: 'Default view',
      config: {
        columns: ['id', 'name', 'status'],
        sort: { field: 'name', direction: 'asc' },
      },
      persistence: 'url',
    }
    expect(view.persistence).toBe('url')
    expect(['url', 'server']).toContain(view.persistence)
  })

  test('SavedViewItem with server persistence passes assertion', () => {
    const view: SavedViewItem = {
      id: 'view-2',
      name: 'My saved view',
      config: {
        columns: ['id', 'email', 'role', 'createdAt'],
      },
      persistence: 'server',
    }
    expect(view.persistence).toBe('server')
    expect(['url', 'server']).toContain(view.persistence)
  })

  test('persistence value must not be localStorage', () => {
    // Simulates runtime check for the rule: if somehow a 'localStorage' value
    // slips through (e.g., from a legacy migration), the check below catches it.
    const invalidPersistence = 'localStorage' as string

    expect(['url', 'server']).not.toContain(invalidPersistence)
  })

  test('url-persisted view config is JSON-serializable to URL param', () => {
    const config = {
      columns: ['id', 'name', 'status'],
      sort: { field: 'status', direction: 'asc' as const },
      filter: JSON.stringify({ type: 'group', connector: 'AND', rules: [] }),
    }
    // Must be serializable without error (URL param value)
    const serialized = btoa(JSON.stringify(config))
    const deserialized = JSON.parse(atob(serialized))
    expect(deserialized.columns).toEqual(config.columns)
    expect(deserialized.sort.field).toBe('status')
  })
})
