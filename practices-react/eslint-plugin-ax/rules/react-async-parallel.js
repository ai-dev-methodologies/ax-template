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
 *   - Skips entire file when it imports an interactive-CLI prompt library
 *     (`readline`, `inquirer`, `prompts`, `enquirer`, `@inquirer/*`,
 *     `@clack/prompts`). Sequential awaits inside such files are usually
 *     stdin-blocking prompts where parallelization would corrupt UX.
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
     * File-level skip flag. Set by the Program visitor when an interactive-CLI
     * prompt library is imported (readline / inquirer / prompts / enquirer /
     * @inquirer/* / @clack/prompts). Such files have sequential awaits by
     * design — each `await rl.question(...)` blocks on stdin and must run in
     * order. Parallelizing would corrupt UX (prompts overlap) or write to
     * stdout in nondeterministic order.
     *
     * Discovered as a false-positive cluster in nextjs/saas-starter's
     * `lib/db/setup.ts` during the Round 2 empirical validation.
     */
    let skipFile = false;

    /**
     * Return true if a module source is one of the known interactive-CLI
     * prompt libraries. Conservative list — only modules whose primary use
     * case is stdin-blocking prompts. CLI argument parsers (yargs, commander)
     * are deliberately NOT on this list because they don't cause sequential
     * await patterns.
     */
    function isInteractiveCliImport(source) {
      if (typeof source !== "string") return false;
      return (
        source === "readline" ||
        source === "node:readline" ||
        source === "readline/promises" ||
        source === "node:readline/promises" ||
        source === "inquirer" ||
        source === "@inquirer/prompts" ||
        source === "@inquirer/core" ||
        source === "prompts" ||
        source === "enquirer" ||
        source === "@clack/prompts"
      );
    }

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

    /**
     * If the awaited call is a method call — return the *root receiver*
     * identifier name. Used to detect builder / Playwright-style sequences
     * where consecutive awaits target the same receiver and are therefore
     * order-dependent even though no `const` binding is shared.
     *
     * Walks through chained MemberExpressions AND through intermediate
     * CallExpressions:
     *
     *   page.foo()                        → "page"
     *   page.x.foo()                      → "page"   (walk member chain)
     *   page.locator('x').fill('y')       → "page"   (descend call → member → page)
     *   expect(page.x).toContainText(...) → "expect" (descend call → expect identifier)
     *   this.foo()                        → "<this>"
     */
    function methodReceiverName(callExpr) {
      if (!callExpr || callExpr.type !== "CallExpression") return null;
      const callee = callExpr.callee;
      if (!callee || callee.type !== "MemberExpression") return null;
      let obj = callee.object;
      // Cap loop depth as a defensive measure against pathological trees.
      for (let depth = 0; obj && depth < 50; depth++) {
        if (obj.type === "MemberExpression") {
          obj = obj.object;
          continue;
        }
        if (obj.type === "CallExpression") {
          // `foo(...).bar()` — receiver is the root of `foo`'s callee.
          obj = obj.callee;
          continue;
        }
        break;
      }
      if (obj && obj.type === "Identifier") return obj.name;
      if (obj && obj.type === "ThisExpression") return "<this>";
      return null;
    }

    function checkBlock(blockNode) {
      const stmts = blockNode.body;
      // Collect index of statements that contribute a top-level await with a Call arg.
      const awaitInfo = []; // {idx, defines, referenced, receiver, reportNode}
      for (let i = 0; i < stmts.length; i++) {
        const stmt = stmts[i];
        const awaitExpr = topLevelAwait(stmt);
        if (!awaitExpr) continue;
        // Only flag when the awaited expression is a call — pure work, not
        // awaiting a passed-in promise value or an already-resolved literal.
        if (awaitExpr.argument.type !== "CallExpression") continue;
        const defines = namesDefined(stmt);
        const referenced = collectReferencedIdentifiers(awaitExpr.argument);
        const receiver = methodReceiverName(awaitExpr.argument);
        awaitInfo.push({ idx: i, defines, referenced, receiver, reportNode: stmt });
      }
      // Compare each consecutive pair (in statement order).
      for (let k = 1; k < awaitInfo.length; k++) {
        const prev = awaitInfo[k - 1];
        const curr = awaitInfo[k];
        // If indices are not consecutive in the source (something non-await
        // sits between them, e.g. logging, validation), skip — that something
        // may consume the previous await's value or have side effects.
        if (curr.idx !== prev.idx + 1) continue;

        // (a) Either await binds a name the next await reads → dependent.
        if (prev.defines.some((n) => curr.referenced.has(n))) continue;

        // (b) Both awaits are method calls on the same receiver identifier
        // (`page.foo()` then `page.bar()`, or `this.x()` then `this.y()`).
        // Such sequences are order-dependent by convention (builder / fluent
        // API / Playwright page actions / database client transactions etc.).
        // Flagging them is high false-positive risk; treat as dependent.
        if (prev.receiver && curr.receiver && prev.receiver === curr.receiver) {
          continue;
        }

        // (c) One await's receiver is referenced by the other await's
        // expression — e.g. `await page.goto('/x')` followed by
        // `await expect(page.locator('h1')).toBeVisible()`. The second
        // await consumes `page` (which the first await mutated). Treat as
        // dependent.
        if (prev.receiver && curr.referenced.has(prev.receiver)) continue;
        if (curr.receiver && prev.referenced.has(curr.receiver)) continue;

        context.report({
          node: curr.reportNode,
          messageId: "independentAwaits",
        });
      }
    }

    function visitFunction(node) {
      if (skipFile) return;
      if (!node.async) return;
      if (!node.body || node.body.type !== "BlockStatement") return;
      checkBlock(node.body);
    }

    return {
      Program(node) {
        for (const stmt of node.body) {
          if (
            stmt.type === "ImportDeclaration" &&
            isInteractiveCliImport(stmt.source.value)
          ) {
            skipFile = true;
            return;
          }
        }
      },
      FunctionDeclaration: visitFunction,
      FunctionExpression: visitFunction,
      ArrowFunctionExpression: visitFunction,
    };
  },
};

export default rule;
