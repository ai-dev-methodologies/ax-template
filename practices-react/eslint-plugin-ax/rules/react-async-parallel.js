/**
 * ax/react-async-parallel
 *
 * Flags consecutive top-level `await` statements inside an async function body
 * whose awaited calls are independent (the second does not reference any
 * identifier bound by the first). These are waterfalls — should be initiated
 * eagerly and aggregated via Promise.all (or Promise.allSettled).
 *
 * Pairs 1:1 with: practices-react/rules/async-parallel.md
 *
 * Scope intentionally narrow to keep false positives low:
 *   - Only looks at statements that are DIRECT children of an async function body
 *     (BlockStatement.body). Awaits inside if/for/try/etc. are NOT analyzed.
 *   - Only flags when the awaited expression is a CallExpression (i.e. work).
 *   - Only flags when the second await references zero identifiers bound by
 *     the first.
 *
 * Future expansion (not in pilot):
 *   - For-loop awaits (`for-of` with await) — separate rule.
 *   - Top-level await in modules (no enclosing function).
 *   - Partial-dependency graphs (better-all / async-dependencies sibling rule).
 */

/** @type {import("eslint").Rule.RuleModule} */
const rule = {
  meta: {
    type: "problem",
    docs: {
      description:
        "Disallow consecutive independent top-level awaits inside an async function body (use Promise.all instead).",
      recommended: true,
      url: "https://github.com/ax-template/practices-react/blob/main/rules/async-parallel.md",
    },
    schema: [],
    messages: {
      independentAwaits:
        "Independent awaits create a waterfall. Initiate both promises first and aggregate with Promise.all (or Promise.allSettled for partial failure). See practices-react/rules/async-parallel.md.",
    },
  },

  create(context) {
    /**
     * Extract identifier names defined by a VariableDeclaration's declarators.
     * Handles plain `const x = ...` and `const { a, b } = ...` / `const [a, b] = ...`.
     */
    function namesDefined(node) {
      if (node.type !== "VariableDeclaration") return [];
      const names = [];
      for (const decl of node.declarations) {
        collectPattern(decl.id, names);
      }
      return names;
    }

    function collectPattern(pat, out) {
      if (!pat) return;
      switch (pat.type) {
        case "Identifier":
          out.push(pat.name);
          return;
        case "ObjectPattern":
          for (const prop of pat.properties) {
            if (prop.type === "Property") collectPattern(prop.value, out);
            else if (prop.type === "RestElement") collectPattern(prop.argument, out);
          }
          return;
        case "ArrayPattern":
          for (const el of pat.elements) collectPattern(el, out);
          return;
        case "AssignmentPattern":
          collectPattern(pat.left, out);
          return;
        case "RestElement":
          collectPattern(pat.argument, out);
          return;
        default:
          return;
      }
    }

    /**
     * Extract the AwaitExpression at the top of a statement, if any.
     * Handles:
     *   - `await call()`               (ExpressionStatement)
     *   - `const x = await call()`     (VariableDeclaration → Declarator.init)
     */
    function topLevelAwait(stmt) {
      if (
        stmt.type === "ExpressionStatement" &&
        stmt.expression.type === "AwaitExpression"
      ) {
        return stmt.expression;
      }
      if (stmt.type === "VariableDeclaration") {
        // Allow only single-declarator forms for clarity; multi-declarator with
        // mixed awaits is unusual and not in the pilot's scope.
        if (
          stmt.declarations.length === 1 &&
          stmt.declarations[0].init &&
          stmt.declarations[0].init.type === "AwaitExpression"
        ) {
          return stmt.declarations[0].init;
        }
      }
      return null;
    }

    /** Collect identifier names referenced inside an arbitrary expression subtree. */
    function collectReferencedIdentifiers(node, out = new Set()) {
      if (!node || typeof node !== "object") return out;
      if (Array.isArray(node)) {
        for (const n of node) collectReferencedIdentifiers(n, out);
        return out;
      }
      if (node.type === "Identifier") {
        out.add(node.name);
        return out;
      }
      // Skip property keys of MemberExpressions and ObjectExpressions (they
      // shadow real references — only the object/value sides matter).
      for (const key of Object.keys(node)) {
        if (key === "parent" || key === "loc" || key === "range" || key === "type")
          continue;
        if (
          node.type === "MemberExpression" &&
          key === "property" &&
          !node.computed
        )
          continue;
        if (node.type === "Property" && key === "key" && !node.computed) continue;
        collectReferencedIdentifiers(node[key], out);
      }
      return out;
    }

    function checkBlock(blockNode) {
      const stmts = blockNode.body;
      // Collect index of statements that contribute a top-level await with a Call arg.
      const awaitInfo = []; // {idx, defines: string[], referenced: Set<string>, reportNode}
      for (let i = 0; i < stmts.length; i++) {
        const stmt = stmts[i];
        const awaitExpr = topLevelAwait(stmt);
        if (!awaitExpr) continue;
        // Only flag when the awaited expression is a call — pure work, not
        // awaiting a passed-in promise value or an already-resolved literal.
        if (awaitExpr.argument.type !== "CallExpression") continue;
        const defines = namesDefined(stmt);
        const referenced = collectReferencedIdentifiers(awaitExpr.argument);
        awaitInfo.push({ idx: i, defines, referenced, reportNode: stmt });
      }
      // Compare each consecutive pair (in statement order).
      for (let k = 1; k < awaitInfo.length; k++) {
        const prev = awaitInfo[k - 1];
        const curr = awaitInfo[k];
        // If indices are not consecutive in the source (something non-await
        // sits between them, e.g. logging, validation), skip — that something
        // may consume the previous await's value or have side effects.
        if (curr.idx !== prev.idx + 1) continue;
        const sharesIdentifier = prev.defines.some((n) =>
          curr.referenced.has(n),
        );
        if (sharesIdentifier) continue; // dependent — correct to await sequentially
        context.report({
          node: curr.reportNode,
          messageId: "independentAwaits",
        });
      }
    }

    function visitFunction(node) {
      if (!node.async) return;
      if (!node.body || node.body.type !== "BlockStatement") return;
      checkBlock(node.body);
    }

    return {
      FunctionDeclaration: visitFunction,
      FunctionExpression: visitFunction,
      ArrowFunctionExpression: visitFunction,
    };
  },
};

export default rule;
