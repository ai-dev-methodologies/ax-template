#!/usr/bin/env python3
"""Internal helper for verify-completion.sh.

Reads the tab-separated PLAN_FILE produced by verify-completion.sh and, given a
step id, prints to stdout the collapsed plan for that step in this format:

    <working_dir>\\t<collapsed_command>\\t<advisory_cmd_count>\\n
    <working_dir>\\t<advisory_command>\\tadvisory\\n   (one per advisory)

The FIRST line is the collapsed non-advisory gradle invocation. Subsequent
lines (if any) are the advisory commands that must run sequentially after.

If the step cannot be safely collapsed (mixed working dirs, non-gradle
commands, or shell metacharacters), nothing is printed (caller falls back to
sequential per-command execution).

R26 rationale: collapsing turns 15-17 cold `./gradlew testXxx` daemon starts
into ONE warm invocation so per-domain-tests finishes in ~1-2 min instead of
~10 min. Advisory items (knowingly-RED external fixtures) run after the
collapsed PASS — their RED state is expected and does not invalidate the
collapsed batch.
"""
from __future__ import annotations
import pathlib
import sys

_UNSAFE_TOKENS = ("|", "&", ";", ">", "<", "`")
_UNSAFE_SUBSHELL_OPEN = "$" + "("  # avoid literal "$(" so this file is safe to
                                    # transit through any shell heredoc.


def _is_unsafe(rest: str) -> bool:
    for tok in _UNSAFE_TOKENS:
        if tok in rest:
            return True
    if _UNSAFE_SUBSHELL_OPEN in rest:
        return True
    return False


def main() -> int:
    plan_path, sid = sys.argv[1], sys.argv[2]
    non_advisory: list[str] = []
    advisory: list[str] = []
    working_dir: str | None = None
    prefix = "./gradlew "

    for raw in pathlib.Path(plan_path).read_text().splitlines():
        if not raw.strip():
            continue
        parts = raw.split("\t")
        if len(parts) < 6:
            continue
        # Accept 6- or 7-column plans (R28 added timeout_seconds column).
        s = parts[0]
        cmd = parts[2]
        wd = parts[3]
        adv = parts[5]
        if s != sid:
            continue
        if working_dir is None:
            working_dir = wd
        elif wd != working_dir:
            # Different working dirs → cannot collapse safely.
            return 0
        if not cmd.startswith(prefix):
            return 0
        rest = cmd[len(prefix):].strip()
        if _is_unsafe(rest):
            return 0
        if adv.lower() == "true":
            advisory.append(rest)
        else:
            non_advisory.append(rest)

    if not non_advisory and not advisory:
        return 0

    # Only collapse when there's a real win — multiple non-advisory commands.
    # Single-command steps gain nothing from a collapse path (the daemon would
    # only fire once anyway) and avoid the noise of `--continue` injection.
    # Also skip when ALL commands are advisory (no batch to collapse).
    if len(non_advisory) < 2:
        return 0

    # Print collapsed non-advisory line first. Empty if all commands were
    # advisory (rare but handle it).
    if non_advisory:
        print(f"{working_dir}\t./gradlew {' '.join(non_advisory)}\t{len(advisory)}")
    else:
        # No non-advisory commands; signal with empty collapsed cmd.
        print(f"{working_dir}\t\t{len(advisory)}")
    for adv_rest in advisory:
        print(f"{working_dir}\t./gradlew {adv_rest}\tadvisory")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
