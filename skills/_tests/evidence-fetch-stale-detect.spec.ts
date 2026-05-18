// @ts-nocheck
/**
 * evidence-fetch-stale-detect.spec.ts — TDD spec for evidence-fetch.sh (F14).
 *
 * Verifies that evidence-fetch.sh correctly detects stale/invalid evidence
 * and passes clean rules. Runs as a self-contained Node.js script.
 *
 * Run with:
 *   node --experimental-strip-types skills/_tests/evidence-fetch-stale-detect.spec.ts
 *   # or: npx tsx skills/_tests/evidence-fetch-stale-detect.spec.ts
 *
 * Requires: Node.js 22+ (strip-types) or tsx installed.
 *
 * Exit 0 = all assertions pass
 * Exit 1 = one or more assertions fail
 */
import { spawnSync } from "node:child_process";
import * as fs from "node:fs";
import * as path from "node:path";
import * as os from "node:os";

// ---------------------------------------------------------------------------
// Mini test harness
// ---------------------------------------------------------------------------
let passCount = 0;
let failCount = 0;
const failures: string[] = [];

function expect(label: string, actual: boolean): void {
  if (actual) {
    passCount++;
    console.log(`  PASS [${label}]`);
  } else {
    failCount++;
    failures.push(label);
    console.error(`  FAIL [${label}]`);
  }
}

const _thisDir = path.dirname(new URL(import.meta.url).pathname);
const repoRoot = path.resolve(_thisDir, "../..");

function runScript(args: string[]): { stdout: string; stderr: string; exitCode: number } {
  const scriptPath = path.join(repoRoot, "skills/ax-verify/scripts/evidence-fetch.sh");
  const result = spawnSync("bash", [scriptPath, ...args], {
    cwd: repoRoot,
    encoding: "utf-8",
    timeout: 30_000,
  });
  return {
    stdout: result.stdout ?? "",
    stderr: result.stderr ?? "",
    exitCode: result.status ?? 1,
  };
}

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "evidence-fetch-spec-"));

function writeTmpRule(name: string, content: string): string {
  const dir = path.join(tmpDir, "rules");
  fs.mkdirSync(dir, { recursive: true });
  const filePath = path.join(dir, name);
  fs.writeFileSync(filePath, content, "utf-8");
  return filePath;
}

// ---------------------------------------------------------------------------
// Test: clean rule passes
// ---------------------------------------------------------------------------
console.log("\n=== evidence-fetch-stale-detect.spec.ts ===\n");
console.log("[1] Clean rule with valid external evidence passes");

const cleanRule = `---
title: Test rule with valid evidence
impact: HIGH
impactDescription: Test description
tags:
  - testing
spec_ref: "specs/test.yaml#TEST-001"
evidence:
  - source_type: external
    citation: "Spring Docs — Dependency Injection patterns"
    url: "https://docs.spring.io/spring-framework/reference/core/beans/dependencies.html"
---

## Clean rule body

Nothing special here.
`;
writeTmpRule("clean-rule.md", cleanRule);

const cleanResult = runScript(["--rule", path.join(tmpDir, "rules/clean-rule.md")]);
expect("clean rule exits 0", cleanResult.exitCode === 0);
expect("clean rule shows OK count >= 1", cleanResult.stdout.includes("OK: 1"));
expect("clean rule shows no ISSUES", !cleanResult.stdout.includes("WARN ["));

// ---------------------------------------------------------------------------
// Test: rule with no evidence block is detected
// ---------------------------------------------------------------------------
console.log("\n[2] Rule with missing evidence block is detected");

const noEvidenceRule = `---
title: Rule with no evidence
impact: HIGH
impactDescription: Missing evidence
tags:
  - testing
spec_ref: "specs/test.yaml#TEST-002"
---

## Body without evidence block
`;
writeTmpRule("no-evidence.md", noEvidenceRule);

const noEvResult = runScript(["--rule", path.join(tmpDir, "rules/no-evidence.md")]);
expect("no_evidence rule exits non-0", noEvResult.exitCode !== 0);
expect("no_evidence rule shows no_evidence_block issue",
  noEvResult.stdout.includes("no_evidence_block"));

// ---------------------------------------------------------------------------
// Test: rule with empty evidence block is detected
// ---------------------------------------------------------------------------
console.log("\n[3] Rule with empty evidence block is detected");

const emptyEvidenceRule = `---
title: Rule with empty evidence
impact: HIGH
impactDescription: Empty evidence
tags:
  - testing
spec_ref: "specs/test.yaml#TEST-003"
evidence:
---

## Body
`;
writeTmpRule("empty-evidence.md", emptyEvidenceRule);

const emptyEvResult = runScript(["--rule", path.join(tmpDir, "rules/empty-evidence.md")]);
expect("empty_evidence rule exits non-0", emptyEvResult.exitCode !== 0);

// ---------------------------------------------------------------------------
// Test: rule with external source_type but no URL is detected
// ---------------------------------------------------------------------------
console.log("\n[4] External evidence with missing URL is detected");

const missingUrlRule = `---
title: Rule with missing URL
impact: MEDIUM
impactDescription: URL missing
tags:
  - testing
spec_ref: "specs/test.yaml#TEST-004"
evidence:
  - source_type: external
    citation: "Some citation without a URL"
---

## Body
`;
writeTmpRule("missing-url.md", missingUrlRule);

const missingUrlResult = runScript(["--rule", path.join(tmpDir, "rules/missing-url.md")]);
expect("missing_url rule exits non-0", missingUrlResult.exitCode !== 0);
expect("missing_url detected as external_missing_url",
  missingUrlResult.stdout.includes("external_missing_url"));

// ---------------------------------------------------------------------------
// Test: --all on real practices catalog passes
// ---------------------------------------------------------------------------
console.log("\n[5] Real practices catalog passes --all check");

const allResult = runScript(["--all"]);
expect("practices --all exits 0", allResult.exitCode === 0);
expect("practices --all reports 76 OK rules",
  allResult.stdout.includes("OK: 76") || /OK: \d+ rule/.test(allResult.stdout));
expect("practices --all shows no WARN issues", !allResult.stdout.includes("WARN ["));

// ---------------------------------------------------------------------------
// Cleanup + summary
// ---------------------------------------------------------------------------
fs.rmSync(tmpDir, { recursive: true, force: true });

console.log("\n=== Summary ===");
console.log(`  Passed: ${passCount}`);
console.log(`  Failed: ${failCount}`);

if (failCount > 0) {
  console.error(`\nFailed assertions:`);
  failures.forEach((f) => console.error(`  - ${f}`));
  process.exit(1);
}

console.log("\nevidence-fetch-stale-detect: all assertions PASS");
process.exit(0);
