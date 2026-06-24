#!/usr/bin/env bash
# practices/scripts/external_url_spot_audit.sh — BACKLOG P2-1b (periodic ADVISORY tool).
#
# The evidence chain has one dimension NO blocking gate checks: `source_type: external`
# entries carry only a citation + URL (no on-disk snapshot), so evidence_guard (structure)
# and evidence_quote_spotcheck_guard (quote-vs-snapshot) both skip them — a fabricated or
# rotted external citation passes every gate. Only a live fetch can verify those. This tool
# is that live fetch: it is ADVISORY and NON-DETERMINISTIC (network), so it is a periodic
# maintenance tool — NOT an R25 guard (R25 demands same-input/same-output; a guard may never
# depend on the network).
#
# For every unique external URL it classifies into three buckets:
#   OK           reachable (HTTP 2xx); and where the URL embeds a verifiable identifier
#                (an RFC number / a CWE id), that identifier appears on the fetched page.
#   SUSPICIOUS   reachable (2xx) BUT the identifier the URL itself claims is ABSENT from the
#                page — a soft-404 / wrong-page / fabricated reference. THIS is the
#                "confirmed-fabricated" signal the done-when measures (low false-positive: it
#                only fires when the page should contain its own id and does not).
#   UNREACHABLE  network error / non-2xx (403 bot-block, 404, 5xx, timeout). ADVISORY ONLY —
#                a blocked fetch is NOT evidence of fabrication, just of un-verifiability here.
#
# Exit code is ALWAYS 0 (advisory). The done-when (P2-1b) is: one sweep with SUSPICIOUS == 0
# (no confirmed-fabricated external reference).
#
# Usage:
#   bash practices/scripts/external_url_spot_audit.sh                  # full sweep, both catalogs
#   bash practices/scripts/external_url_spot_audit.sh --host rfc-editor.org   # one host
#   bash practices/scripts/external_url_spot_audit.sh --sample 40      # first N unique URLs
#   bash practices/scripts/external_url_spot_audit.sh --timeout 8 --verifiable-only
# Options:
#   --host SUBSTR        only URLs whose host contains SUBSTR
#   --sample N           only the first N unique URLs (after host filter)
#   --timeout S          per-request timeout seconds (default 12)
#   --verifiable-only    only fetch URLs that embed a checkable identifier (rfc/cwe) — the
#                        subset where SUSPICIOUS is a strong signal; skips reachability-only hosts
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

python3 - "$REPO_ROOT" "$@" <<'PY'
import sys, os, re, glob, urllib.request, urllib.error, ssl

repo = sys.argv[1]
args = sys.argv[2:]

host_filter = None
sample = None
timeout = 12
verifiable_only = False
i = 0
while i < len(args):
    a = args[i]
    if a == "--host": host_filter = args[i+1]; i += 2
    elif a == "--sample": sample = int(args[i+1]); i += 2
    elif a == "--timeout": timeout = int(args[i+1]); i += 2
    elif a == "--verifiable-only": verifiable_only = True; i += 1
    else:
        print(f"external_url_spot_audit: unknown arg: {a}", file=sys.stderr); sys.exit(2)

try:
    import yaml
except Exception:
    print("external_url_spot_audit: PyYAML required (pip install pyyaml)", file=sys.stderr)
    sys.exit(2)

# ── collect (url, citation, rule) from every source_type: external evidence entry ──
entries = {}  # url -> (citation, rule_rel)
rule_globs = [
    os.path.join(repo, "practices", "rules", "*.md"),
    os.path.join(repo, "practices-react", "rules", "*.md"),
]
for g in rule_globs:
    for path in sorted(glob.glob(g)):
        text = open(path, encoding="utf-8").read()
        if not text.startswith("---"):
            continue
        parts = text.split("---", 2)
        if len(parts) < 3:
            continue
        try:
            fm = yaml.safe_load(parts[1]) or {}
        except yaml.YAMLError:
            continue
        ev = fm.get("evidence")
        if not isinstance(ev, list):
            continue
        for item in ev:
            if not isinstance(item, dict):
                continue
            if item.get("source_type") != "external":
                continue
            url = str(item.get("url", "")).strip()
            if not url.startswith("http"):
                continue
            cit = str(item.get("citation", "")).strip()
            entries.setdefault(url, (cit, os.path.relpath(path, repo)))

# ── identifier the URL itself claims (RFC number / CWE id) for the SUSPICIOUS check ──
def claimed_id(url):
    m = re.search(r'rfc-editor\.org/rfc/rfc(\d+)', url)
    if m: return ("rfc", m.group(1))
    m = re.search(r'cwe\.mitre\.org/.*?(\d+)\.html', url)
    if m: return ("cwe", m.group(1))
    m = re.search(r'datatracker\.ietf\.org/doc/html/rfc(\d+)', url)
    if m: return ("rfc", m.group(1))
    return None

urls = sorted(entries)
if host_filter:
    urls = [u for u in urls if host_filter in u]
if verifiable_only:
    urls = [u for u in urls if claimed_id(u)]
if sample is not None:
    urls = urls[:sample]

print(f"external_url_spot_audit: {len(entries)} unique external URL(s); auditing {len(urls)} "
      f"(host={host_filter or 'all'}, verifiable_only={verifiable_only}, timeout={timeout}s)")
print()

ctx = ssl.create_default_context()
UA = "Mozilla/5.0 (ax-template evidence audit; +https://github.com/ax-template)"

ok = []; suspicious = []; unreachable = []
for u in urls:
    cid = claimed_id(u)
    try:
        req = urllib.request.Request(u, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
            code = resp.getcode()
            body = resp.read(600_000).decode("utf-8", "replace").lower()
    except urllib.error.HTTPError as e:
        unreachable.append((u, f"HTTP {e.code}")); continue
    except Exception as e:
        unreachable.append((u, type(e).__name__)); continue
    if code < 200 or code >= 300:
        unreachable.append((u, f"HTTP {code}")); continue
    if cid:
        kind, num = cid
        # the page must contain the id it claims (e.g. "9457" for rfc9457, "89" for CWE-89)
        token = num if kind == "rfc" else num
        present = (num.lower() in body) or (f"{kind}-{num}".lower() in body)
        if present:
            ok.append(u)
        else:
            suspicious.append((u, f"{kind.upper()} id {num} absent from page", entries[u][1]))
    else:
        ok.append(u)  # reachability-only host

print(f"OK (reachable{'/' if True else ''} + id-verified where checkable): {len(ok)}")
print(f"UNREACHABLE (advisory — bot-block / 404 / timeout, NOT fabrication): {len(unreachable)}")
print(f"SUSPICIOUS (reachable but claimed id absent — confirmed-fabricated candidates): {len(suspicious)}")
print()
if suspicious:
    print("── SUSPICIOUS (review these — done-when requires 0) ──")
    for u, why, rule in suspicious:
        print(f"  ! {u}\n      {why}  [{rule}]")
    print()
if unreachable:
    # summarize unreachable by host so the noise is scannable, not per-URL spam
    from collections import Counter
    hosts = Counter(re.sub(r'https?://([^/]+).*', r'\1', u) for u, _ in unreachable)
    print("── UNREACHABLE by host (advisory) ──")
    for h, n in hosts.most_common():
        print(f"  {n:>3}  {h}")
    print()

print(f"external_url_spot_audit: SUSPICIOUS={len(suspicious)} (done-when: 0). Advisory — exit 0.")
sys.exit(0)
PY
