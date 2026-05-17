#!/usr/bin/env python3
"""
templates/backend/_check-anchors.py
Validates @ax-template-meta blocks in all Java templates under templates/backend/.

For each template:
  1. Extracts the @ax-template-meta Javadoc block
  2. Verifies anchors_rule references existing rule files in practices/rules/
  3. Verifies evidence entries have valid citation+url (external) or
     registered upstream_id (_MANIFEST.yaml)

Exit 0: all templates pass
Exit 1: one or more violations
"""
import argparse
import json
import pathlib
import re
import sys
import yaml


def parse_meta_block(java_text: str) -> str | None:
    """Extract the @ax-template-meta Javadoc block, stripping leading ' * ' prefixes."""
    m = re.search(r'/\*\*.*?@ax-template-meta(.*?)\*/', java_text, re.DOTALL)
    if not m:
        return None
    lines = []
    for line in m.group(1).splitlines():
        lines.append(re.sub(r'^\s*\*\s?', '', line))
    return '\n'.join(lines)


def load_manifest_ids(manifest_path: pathlib.Path) -> set:
    if not manifest_path.exists():
        return set()
    data = yaml.safe_load(manifest_path.read_text()) or {}
    return {s.get('id', '') for s in data.get('snapshots', []) if s.get('id')}


def check_template(java_file: pathlib.Path,
                   rules_dir: pathlib.Path,
                   manifest_ids: set,
                   verbose: bool) -> list[str]:
    """Returns list of violation strings (empty = pass)."""
    violations = []
    relpath = str(java_file)

    if verbose:
        print(f"checking: {relpath}")

    text = java_file.read_text()
    meta_block = parse_meta_block(text)

    if not meta_block:
        return [f"VIOLATION [{relpath}]: no @ax-template-meta block found"]

    # --- anchors_rule check ---
    anchors_match = re.search(r'anchors_rule:\s*(.+)', meta_block)
    if not anchors_match:
        violations.append(f"VIOLATION [{relpath}]: missing anchors_rule field")
    else:
        rule_refs = re.findall(r'[a-z][a-z0-9-]+\.md', anchors_match.group(0))
        for rule_file in rule_refs:
            if not (rules_dir / rule_file).exists():
                violations.append(
                    f"VIOLATION [{relpath}]: anchors_rule references non-existent rule: {rule_file}"
                )

    # --- evidence check ---
    ev_match = re.search(r'(evidence:.*)', meta_block, re.DOTALL)
    if not ev_match:
        violations.append(f"VIOLATION [{relpath}]: missing evidence block")
        return violations

    try:
        parsed = yaml.safe_load(ev_match.group(1)) or {}
        evidence = parsed.get('evidence', []) if isinstance(parsed, dict) else []
    except yaml.YAMLError as exc:
        violations.append(f"VIOLATION [{relpath}]: evidence YAML parse error: {exc}")
        return violations

    if not evidence:
        violations.append(f"VIOLATION [{relpath}]: evidence block is empty")
        return violations

    for i, entry in enumerate(evidence):
        if not isinstance(entry, dict):
            violations.append(f"VIOLATION [{relpath}]: evidence entry {i} is not a mapping")
            continue
        if 'upstream_id' in entry:
            uid = entry['upstream_id']
            if uid not in manifest_ids:
                violations.append(
                    f"VIOLATION [{relpath}]: evidence entry {i}: "
                    f"upstream_id={uid!r} not registered in _MANIFEST.yaml"
                )
        elif entry.get('source_type') == 'external':
            if not str(entry.get('citation', '')).strip():
                violations.append(f"VIOLATION [{relpath}]: evidence entry {i}: missing citation")
            if not str(entry.get('url', '')).strip():
                violations.append(f"VIOLATION [{relpath}]: evidence entry {i}: missing url")
        else:
            violations.append(
                f"VIOLATION [{relpath}]: evidence entry {i}: "
                "must have upstream_id or source_type: external"
            )

    return violations


def main() -> int:
    parser = argparse.ArgumentParser(description='Check @ax-template-meta anchors in Java templates')
    parser.add_argument('--templates-dir', required=True, type=pathlib.Path)
    parser.add_argument('--rules-dir', required=True, type=pathlib.Path)
    parser.add_argument('--manifest', required=True, type=pathlib.Path)
    parser.add_argument('--verbose', action='store_true')
    args = parser.parse_args()

    manifest_ids = load_manifest_ids(args.manifest)
    java_files = sorted(args.templates_dir.rglob('*.java'))

    all_violations = []
    for java_file in java_files:
        violations = check_template(java_file, args.rules_dir, manifest_ids, args.verbose)
        all_violations.extend(violations)

    for v in all_violations:
        print(v)

    if all_violations:
        print(f"\n_check-anchors: {len(all_violations)} violation(s) found — fix before shipping",
              file=sys.stderr)
        return 1

    print("_check-anchors: all template anchors valid")
    return 0


if __name__ == '__main__':
    sys.exit(main())
