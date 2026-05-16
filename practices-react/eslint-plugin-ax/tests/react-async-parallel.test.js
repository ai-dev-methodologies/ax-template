import { RuleTester } from "eslint";
import test from "node:test";
import rule from "../rules/react-async-parallel.js";

const tester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: "module",
  },
});

test("react-async-parallel — RuleTester suite", () => {
  tester.run("ax/react-async-parallel", rule, {
    valid: [
      // Dependent awaits — second uses identifier bound by first.
      {
        code: `
          async function loadProfile(id) {
            const user = await fetchUser(id);
            const posts = await fetchPostsForUser(user.id);
            return { user, posts };
          }
        `,
      },
      // Single await — no waterfall possible.
      {
        code: `
          async function loadOne() {
            const user = await fetchUser();
            return user;
          }
        `,
      },
      // Awaits separated by non-await statement — out of scope by design.
      {
        code: `
          async function withGap() {
            const user = await fetchUser();
            log("loaded");
            const posts = await fetchPosts();
            return [user, posts];
          }
        `,
      },
      // Already using Promise.all — correct.
      {
        code: `
          async function loadParallel() {
            const [user, posts] = await Promise.all([fetchUser(), fetchPosts()]);
            return { user, posts };
          }
        `,
      },
      // Non-async function — body never executes await.
      {
        code: `
          function notAsync() {
            return 1;
          }
        `,
      },
      // Awaits inside conditional branches — scope intentionally narrow.
      {
        code: `
          async function conditional(flag) {
            if (flag) {
              const a = await fetchA();
              return a;
            } else {
              const b = await fetchB();
              return b;
            }
          }
        `,
      },
      // Destructured dependent await — defines names, second uses them.
      {
        code: `
          async function destructured() {
            const { id } = await fetchSession();
            const profile = await fetchProfile(id);
            return profile;
          }
        `,
      },
      // Playwright-style page actions — shared receiver = dependent.
      // (Each `page.foo()` mutates the page; order matters by design.)
      {
        code: `
          async function loginFlow(page) {
            await page.goto('/login');
            await page.fill('#email', 'test@test.com');
            await page.fill('#password', 'secret');
            await page.click('button[type=submit]');
          }
        `,
      },
      // Builder / fluent pattern — same receiver in a chain.
      {
        code: `
          async function buildAndSend(client) {
            await client.connect();
            await client.authenticate('token');
            await client.send({ kind: 'event' });
            await client.disconnect();
          }
        `,
      },
      // 'this'-method sequence in a class — order-dependent.
      {
        code: `
          class Service {
            async run() {
              await this.startTransaction();
              await this.applyChanges();
              await this.commitTransaction();
            }
          }
        `,
      },
      // Chained member receiver — `db.users.find(...)` then `db.posts.find(...)`
      // share root receiver `db`; treat as dependent (could be in a tx).
      {
        code: `
          async function loadAll(db) {
            await db.users.find();
            await db.posts.find();
          }
        `,
      },
      // Receiver walk through CallExpression — `page.locator('x').fill('y')`
      // has callee `page.locator('x').fill`, whose object is a CallExpression
      // `page.locator('x')`. Descend into that call to find root `page`.
      {
        code: `
          async function fillForm(page) {
            await page.locator('input[type=email]').fill('a@b.com');
            await page.locator('input[type=password]').fill('secret');
            await page.getByRole('button').click();
          }
        `,
      },
      // Assertion wrapper — \`expect(page.x).toFoo()\` and \`expect(page.y).toBar()\`
      // share root receiver \`expect\`. Playwright/jest matcher chains belong here.
      {
        code: `
          async function asserts(page) {
            await expect(page.locator('h1')).toContainText('Login');
            await expect(page.getByText('Submit')).toBeVisible();
            await expect(page.getByRole('button')).toBeEnabled();
          }
        `,
      },
      // Receiver of one await is referenced by the next — cross-receiver dep.
      // `await page.goto(...)` mutates page; following `expect(page.x)` reads it.
      {
        code: `
          async function loginPage(page) {
            await page.goto('/login');
            await expect(page.locator('h1')).toContainText('Login');
          }
        `,
      },
      // Symmetric direction — assertion first, then state-mutating call.
      {
        code: `
          async function check(page) {
            await expect(page.locator('h1')).toBeVisible();
            await page.click('button');
          }
        `,
      },
    ],
    invalid: [
      // Classic 3-call waterfall from Vercel's "Incorrect" example.
      {
        code: `
          async function loadDashboard() {
            const user = await fetchUser();
            const posts = await fetchPosts();
            const comments = await fetchComments();
            return { user, posts, comments };
          }
        `,
        errors: [{ messageId: "independentAwaits" }, { messageId: "independentAwaits" }],
      },
      // 2-call waterfall, arrow function form.
      {
        code: `
          const load = async () => {
            const a = await fetchA();
            const b = await fetchB();
            return [a, b];
          };
        `,
        errors: [{ messageId: "independentAwaits" }],
      },
      // Awaited expression-statement form (no var binding).
      {
        code: `
          async function side() {
            await sendMetric();
            await sendOtherMetric();
          }
        `,
        errors: [{ messageId: "independentAwaits" }],
      },
      // Mixed: first dependent, then independent — should flag only the
      // independent pair.
      {
        code: `
          async function mixed(uid) {
            const user = await fetchUser(uid);
            const posts = await fetchPostsForUser(user.id);
            const config = await fetchAppConfig();
            return { user, posts, config };
          }
        `,
        errors: [{ messageId: "independentAwaits" }],
      },
    ],
  });
});
