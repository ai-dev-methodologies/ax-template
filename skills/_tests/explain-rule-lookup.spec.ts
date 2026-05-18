// @ts-nocheck
/**
 * explain-rule-lookup.spec.ts — TDD spec for explain.sh (F15).
 *
 * Verifies that explain.sh correctly:
 * - Finds rules by exact spec_id
 * - Finds rules by keyword (title / filename)
 * - Returns JSON output when --format json is specified
 * - Lists all rules in --list mode
 * - Returns exit 1 for unknown IDs
 *
 * Run with:
 *   node --experimental-strip-types skills/_tests/explain-rule-lookup.spec.ts
 *   # or: npx tsx skills/_tests/explain-rule-lookup.spec.ts
 *
 * Exit 0 = all assertions pass
 * Exit 1 = one or more assertions fail
 */
import { spawnSync } from "node:child_process";
import * as path from "node:path";

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

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..");
const scriptPath = path.join(repoRoot, "skills/ax-verify/scripts/explain.sh");

function runExplain(args: string[]): { stdout: string; stderr: string; exitCode: number } {
  const result = spawnSync("bash", [scriptPath, ...args], {
    cwd: repoRoot,
    encoding: "utf-8",
    timeout: 15_000,
  });
  return {
    stdout: result.stdout ?? "",
    stderr: result.stderr ?? "",
    exitCode: result.status ?? 1,
  };
}

// ---------------------------------------------------------------------------
// [1] Look up by exact spec_id — PRACTICES-PERS-005
// ---------------------------------------------------------------------------
console.log("\n=== explain-rule-lookup.spec.ts ===\n");
console.log("[1] Lookup by exact spec_id: PRACTICES-PERS-005");

const pers005 = runExplain(["PRACTICES-PERS-005"]);
expect("PRACTICES-PERS-005 exits 0", pers005.exitCode === 0);
expect("PRACTICES-PERS-005 output contains spec_id", pers005.stdout.includes("PRACTICES-PERS-005"));
expect("PRACTICES-PERS-005 output contains title keyword 'soft'",
  pers005.stdout.toLowerCase().includes("soft"));
expect("PRACTICES-PERS-005 output contains impact level HIGH", pers005.stdout.includes("[HIGH]"));

// ---------------------------------------------------------------------------
// [2] Look up by keyword: soft-delete
// ---------------------------------------------------------------------------
console.log("\n[2] Lookup by keyword: soft-delete");

const softDelete = runExplain(["soft-delete"]);
expect("soft-delete exits 0", softDelete.exitCode === 0);
expect("soft-delete result contains PRACTICES-PERS-005",
  softDelete.stdout.includes("PRACTICES-PERS-005"));

// ---------------------------------------------------------------------------
// [3] Look up error handling rule by spec_id
// ---------------------------------------------------------------------------
console.log("\n[3] Lookup by exact spec_id: PRACTICES-ERR-001");

const err001 = runExplain(["PRACTICES-ERR-001"]);
expect("PRACTICES-ERR-001 exits 0", err001.exitCode === 0);
expect("PRACTICES-ERR-001 output contains ControllerAdvice or RestControllerAdvice",
  err001.stdout.includes("ControllerAdvice") || err001.stdout.toLowerCase().includes("advice"));

// ---------------------------------------------------------------------------
// [4] JSON format output
// ---------------------------------------------------------------------------
console.log("\n[4] JSON format output for PRACTICES-PERS-005");

const jsonResult = runExplain(["--format", "json", "PRACTICES-PERS-005"]);
expect("JSON output exits 0", jsonResult.exitCode === 0);

let parsed: Record<string, unknown> | null = null;
try {
  parsed = JSON.parse(jsonResult.stdout) as Record<string, unknown>;
} catch {
  parsed = null;
}
expect("JSON output is valid JSON", parsed !== null);
if (parsed !== null) {
  expect("JSON has spec_id field", typeof parsed["spec_id"] === "string");
  expect("JSON has title field", typeof parsed["title"] === "string");
  expect("JSON has impact field", typeof parsed["impact"] === "string");
  expect("JSON has tags array", Array.isArray(parsed["tags"]));
  expect("JSON spec_id is PRACTICES-PERS-005", parsed["spec_id"] === "PRACTICES-PERS-005");
  expect("JSON impact is HIGH", parsed["impact"] === "HIGH");
}

// ---------------------------------------------------------------------------
// [5] List mode
// ---------------------------------------------------------------------------
console.log("\n[5] --list mode");

const listResult = runExplain(["--list"]);
expect("--list exits 0", listResult.exitCode === 0);
expect("--list output contains RULE ID header", listResult.stdout.includes("RULE ID"));
expect("--list output contains PRACTICES-PERS-005", listResult.stdout.includes("PRACTICES-PERS-005"));
expect("--list output contains PRACTICES-ERR-001", listResult.stdout.includes("PRACTICES-ERR-001"));
expect("--list output contains multiple rules (>= 10 lines with PRACTICES)",
  (listResult.stdout.match(/PRACTICES-/g) ?? []).length >= 10);

// ---------------------------------------------------------------------------
// [6] Unknown ID returns exit 1
// ---------------------------------------------------------------------------
console.log("\n[6] Unknown rule ID returns exit 1");

const unknownResult = runExplain(["PRACTICES-NONEXISTENT-9999"]);
expect("unknown ID exits 1", unknownResult.exitCode === 1);
expect("unknown ID shows 'no rule found' message",
  unknownResult.stderr.includes("no rule found") ||
  unknownResult.stdout.includes("no rule found"));

// ---------------------------------------------------------------------------
// [7] Suffix match (partial ID lookup)
// ---------------------------------------------------------------------------
console.log("\n[7] Partial suffix match: PERS-005");

const suffixResult = runExplain(["PERS-005"]);
expect("PERS-005 suffix exits 0", suffixResult.exitCode === 0);
expect("PERS-005 suffix finds PRACTICES-PERS-005",
  suffixResult.stdout.includes("PRACTICES-PERS-005"));

// ---------------------------------------------------------------------------
// [8] Keyword lookup: constructor-injection
// ---------------------------------------------------------------------------
console.log("\n[8] Keyword lookup: constructor");

const ctorResult = runExplain(["constructor"]);
expect("constructor keyword exits 0", ctorResult.exitCode === 0);
expect("constructor keyword finds PRACTICES-CORE-001",
  ctorResult.stdout.includes("PRACTICES-CORE-001") ||
  ctorResult.stdout.toLowerCase().includes("constructor"));

// ---------------------------------------------------------------------------
// Summary
// ---------------------------------------------------------------------------
console.log("\n=== Summary ===");
console.log(`  Passed: ${passCount}`);
console.log(`  Failed: ${failCount}`);

if (failCount > 0) {
  console.error("\nFailed assertions:");
  failures.forEach((f) => console.error(`  - ${f}`));
  process.exit(1);
}

console.log("\nexplain-rule-lookup: all assertions PASS");
process.exit(0);
