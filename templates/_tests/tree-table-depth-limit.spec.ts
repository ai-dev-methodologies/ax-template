/**
 * TDD anchor: tree-table-depth-limit.spec.ts
 * SP33 risk mitigation — tree-table must throw DepthExceededError when depth > 5.
 *
 * PRD Risk 3: "tree-table state explosion at depth >5."
 * Command: npx vitest run templates/_tests/tree-table-depth-limit.spec.ts
 * Threshold: depth 6 input does not throw explicit error → test FAILS.
 */

import { describe, expect, test } from 'vitest'
import {
  TREE_TABLE_MAX_DEPTH,
  DepthExceededError,
  assertDepth,
  type TreeRow,
} from '../L2/blocks/tree-table'

describe('TreeTable depth guard', () => {
  test('TREE_TABLE_MAX_DEPTH is 5', () => {
    expect(TREE_TABLE_MAX_DEPTH).toBe(5)
  })

  test('assertDepth does not throw at depth 0 through 5', () => {
    for (let d = 0; d <= TREE_TABLE_MAX_DEPTH; d++) {
      expect(() => assertDepth(d)).not.toThrow()
    }
  })

  test('assertDepth throws DepthExceededError at depth 6', () => {
    expect(() => assertDepth(6)).toThrowError(DepthExceededError)
  })

  test('DepthExceededError message contains the depth value', () => {
    expect(() => assertDepth(7)).toThrowError(/depth 7/)
  })

  test('nested tree data within 5 levels validates without error', () => {
    // Depth: root(0) → child(1) → grandchild(2) → great(3) → great-great(4) → leaf(5)
    function buildNode(depth: number, maxDepth: number): TreeRow {
      return {
        id: `node-d${depth}`,
        data: { name: `Level ${depth}` },
        children: depth < maxDepth ? [buildNode(depth + 1, maxDepth)] : undefined,
      }
    }

    const validTree = [buildNode(0, 5)]
    // Should not throw when traversing valid depth
    function traverseAndAssert(nodes: TreeRow[], depth: number) {
      for (const node of nodes) {
        expect(() => assertDepth(depth)).not.toThrow()
        if (node.children) {
          traverseAndAssert(node.children, depth + 1)
        }
      }
    }
    traverseAndAssert(validTree, 0)
  })

  test('tree data at depth 6 triggers DepthExceededError', () => {
    function buildDeepNode(depth: number, maxDepth: number): TreeRow {
      return {
        id: `node-d${depth}`,
        data: { name: `Level ${depth}` },
        children: depth < maxDepth ? [buildDeepNode(depth + 1, maxDepth)] : undefined,
      }
    }

    // Build tree with 7 levels (0-6)
    const deepTree = [buildDeepNode(0, 6)]

    function traverseAndAssert(nodes: TreeRow[], depth: number) {
      for (const node of nodes) {
        assertDepth(depth) // throws at depth 6
        if (node.children) {
          traverseAndAssert(node.children, depth + 1)
        }
      }
    }

    expect(() => traverseAndAssert(deepTree, 0)).toThrowError(DepthExceededError)
  })
})
