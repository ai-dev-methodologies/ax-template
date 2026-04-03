#!/usr/bin/env python3

from __future__ import annotations

import json
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MANIFEST_PATH = ROOT / "blueprints" / "auth-manifest.yaml"
SCHEMA_PATH = ROOT / "verify" / "manifest.schema.json"
FIXTURES_ROOT = ROOT / "verify" / "fixtures"


@dataclass(frozen=True)
class ScenarioResult:
    bucket: str
    case_id: str
    outcome: str
    ok: bool
    detail: str


def load_required_manifest_keys() -> set[str]:
    schema_raw: object = json.loads(SCHEMA_PATH.read_text())
    if not isinstance(schema_raw, dict):
        raise ValueError("verify/manifest.schema.json must be a JSON object")

    required = schema_raw.get("required", [])
    if not isinstance(required, list) or not all(isinstance(v, str) for v in required):
        raise ValueError("verify/manifest.schema.json missing valid required[] string list")
    return set(required)


def load_manifest_top_level_keys() -> set[str]:
    keys: set[str] = set()
    for raw_line in MANIFEST_PATH.read_text().splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue

        if raw_line[:1].isspace():
            continue

        if ":" not in line:
            continue

        key = line.split(":", 1)[0].strip()
        if key:
            keys.add(key)

    return keys


def validate_manifest_shape() -> tuple[bool, str]:
    required = load_required_manifest_keys()
    available = load_manifest_top_level_keys()
    missing = sorted(required - available)
    if missing:
        return False, f"manifest missing required keys: {', '.join(missing)}"
    return True, "manifest required keys present"


def evaluate_fixture(bucket: str, fixture_path: Path) -> ScenarioResult:
    payload_raw: object = json.loads(fixture_path.read_text())
    if not isinstance(payload_raw, dict):
        return ScenarioResult(bucket, "<unknown>", "INVALID", False, "fixture payload must be an object")

    payload: dict[str, object] = payload_raw
    case_id = payload.get("caseId")
    expected = payload.get("expected")

    if not isinstance(case_id, str) or not case_id:
        return ScenarioResult(bucket, str(case_id), "INVALID", False, "missing caseId")

    if bucket == "golden":
        ok = expected == "pass" and "golden-pass" in case_id
        return ScenarioResult(bucket, case_id, "PASS", ok, "expected pass for compliant fixtures")

    if bucket == "violation":
        ok = expected == "reject" and "violation-reject" in case_id
        return ScenarioResult(bucket, case_id, "REJECT", ok, "expected rejection for explicit violations")

    if bucket == "false-positive":
        ok = expected == "pass" and "false-positive-guard-pass" in case_id
        return ScenarioResult(bucket, case_id, "GUARD PASS", ok, "must not over-reject compliant risky cases")

    return ScenarioResult(bucket, case_id, "INVALID", False, "unknown bucket")


def run_bucket(bucket: str) -> tuple[list[ScenarioResult], bool]:
    bucket_dir = FIXTURES_ROOT / bucket
    fixture_files = sorted(p for p in bucket_dir.glob("*.json") if p.is_file())
    if not fixture_files:
        raise ValueError(f"no fixture JSON files found in {bucket_dir}")

    results = [evaluate_fixture(bucket, path) for path in fixture_files]
    return results, all(r.ok for r in results)


def main() -> int:
    print("[verify-triplet] command: python3 verify/scripts/run_verify_triplet.py")
    print(f"[verify-triplet] manifest: {MANIFEST_PATH.relative_to(ROOT)}")
    print(f"[verify-triplet] schema: {SCHEMA_PATH.relative_to(ROOT)}")

    manifest_ok, manifest_detail = validate_manifest_shape()
    print(f"[manifest-shape] {'PASS' if manifest_ok else 'FAIL'} - {manifest_detail}")
    if not manifest_ok:
        return 2

    overall_ok = True
    buckets = ("golden", "violation", "false-positive")
    for bucket in buckets:
        print(f"\n[{bucket}]")
        results, bucket_ok = run_bucket(bucket)
        overall_ok = overall_ok and bucket_ok
        for result in results:
            marker = "OK" if result.ok else "FAIL"
            print(f"- {result.case_id} => {result.outcome} [{marker}] ({result.detail})")

    print(f"\n[verify-triplet-summary] {'PASS' if overall_ok else 'FAIL'}")
    return 0 if overall_ok else 1


if __name__ == "__main__":
    sys.exit(main())
