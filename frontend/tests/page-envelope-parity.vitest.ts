import { readFileSync } from 'fs'
import { join } from 'path'
import { describe, it, expect } from 'vitest'

// S2.QUERY-BOUNDS.XB — FE leg of the FE<->BE pagination-envelope contract
// parity pair. Reads the SAME golden fixture as
// backend/src/test/java/.../common/PageEnvelopeContractParityTest.java
// (frontend/tests/_fixtures/page-envelope.golden.json) — one committed
// source of truth, two independent consumers. A drift in either the
// backend record's field names or this parser trips exactly one of the
// two tests, never silently.

import {
  parsePageEnvelope,
  type PageEnvelopePagination,
} from '../../templates/L0/fork-receiver-kit/parse-page-envelope'

function readGolden(): unknown {
  const raw = readFileSync(join(process.cwd(), 'tests/_fixtures/page-envelope.golden.json'), 'utf8')
  return JSON.parse(raw)
}

describe('parsePageEnvelope — parity with the BE-emitted PageEnvelope golden (S2.QUERY-BOUNDS.XB)', () => {
  it('dereferences all five pagination.* fields from the real BE-shaped golden fixture', () => {
    const golden = readGolden()
    const parsed = parsePageEnvelope<{ id: string; title: string }>(golden)

    const pagination: PageEnvelopePagination = parsed.pagination
    expect(pagination.page).toBe(1)
    expect(pagination.pageSize).toBe(20)
    expect(pagination.totalElements).toBe(137)
    expect(pagination.totalPages).toBe(7)
    expect(pagination.hasMore).toBe(true)

    expect(parsed.data).toHaveLength(2)
    expect(parsed.data[0].id).toBe('3f9a2b8e-6b7a-4e1a-9c3d-1a2b3c4d5e6f')
    expect(parsed.data[0].title).toBe('Quarterly report draft')
  })

  it('throws a TypeError when "data" is missing or not an array', () => {
    expect(() => parsePageEnvelope({ pagination: {} })).toThrow(TypeError)
    expect(() => parsePageEnvelope({ data: 'not-an-array', pagination: {} })).toThrow(TypeError)
  })

  it('throws a TypeError when "pagination" is missing', () => {
    expect(() => parsePageEnvelope({ data: [] })).toThrow(TypeError)
  })

  it('throws a TypeError when any of page/pageSize/totalElements/totalPages is missing or the wrong type', () => {
    const base = { page: 0, pageSize: 20, totalElements: 0, totalPages: 0, hasMore: false }
    for (const field of ['page', 'pageSize', 'totalElements', 'totalPages'] as const) {
      const broken = { ...base, [field]: undefined }
      expect(() => parsePageEnvelope({ data: [], pagination: broken })).toThrow(TypeError)
      const wrongType = { ...base, [field]: String((base as Record<string, unknown>)[field]) }
      expect(() => parsePageEnvelope({ data: [], pagination: wrongType })).toThrow(TypeError)
    }
  })

  it('throws a TypeError when hasMore is missing or not a boolean', () => {
    const base = { page: 0, pageSize: 20, totalElements: 0, totalPages: 0 }
    expect(() => parsePageEnvelope({ data: [], pagination: base })).toThrow(TypeError)
    expect(() => parsePageEnvelope({ data: [], pagination: { ...base, hasMore: 'true' } })).toThrow(TypeError)
  })
})
