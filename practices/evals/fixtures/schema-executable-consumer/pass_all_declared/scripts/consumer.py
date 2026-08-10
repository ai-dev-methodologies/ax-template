#!/usr/bin/env python3
"""Fixture consumer for schema_executable_consumer_guard.sh — pass_all_declared.

Loads schemas/sample-b.schema.json at runtime, mirroring the real
verify/scripts/run_verify_triplet.py consumption of verify/manifest.schema.json.
"""
import json
from pathlib import Path

SCHEMA_PATH = Path(__file__).resolve().parents[1] / "schemas" / "sample-b.schema.json"


def load_required_keys():
    schema = json.loads(SCHEMA_PATH.read_text())
    return set(schema.get("required", []))
