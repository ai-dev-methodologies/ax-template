#!/usr/bin/env bash
# practices/evals/liveness_probe_no_downstream_guard.sh
# Mechanises specs/health-check-l0.yaml#HEALTH-LIVENESS-001 — the clause the
# existing actuator-kubernetes-probes rule (PRACTICES-ACTUATOR-001) does NOT cover.
#
# THE ANTI-PATTERN: a Kubernetes liveness probe failing → kill + restart the pod.
# If the liveness health group includes a DOWNSTREAM dependency indicator (db,
# redis, mongo, kafka, …), then a transient dependency outage flips liveness to
# DOWN, the orchestrator restarts every pod, the restarts hammer the recovering
# dependency, and a recoverable blip becomes a self-amplifying outage. Liveness
# MUST answer only "is THIS process wedged" — never "is my database up". Readiness
# (HEALTH-READINESS-001) is where critical-dependency checks belong (a DOWN
# readiness drains traffic without a restart), so this guard scopes STRICTLY to the
# `liveness` group and never inspects `readiness`.
#
# THE CHECK: scan backend application*.yml. Spring Boot's DEFAULT liveness group is
# `livenessState` only (safe) — so a config with no custom liveness group PASSES.
# The guard fires only when a fork-receiver has explicitly added a
# `management.endpoint.health.group.liveness.include` (nested or flattened) that
# lists a downstream indicator token.
#
# Evidence (external):
#   Spring Boot Reference — "Kubernetes Probes" + "Health Groups":
#     https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes
#   Kubernetes — "Configure Liveness, Readiness and Startup Probes" (liveness != dependency health):
#     https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
#
# Exit: 0 PASS · 1 liveness group includes a downstream indicator · 2 usage error.
#
# Usage:
#   bash practices/evals/liveness_probe_no_downstream_guard.sh
#   bash practices/evals/liveness_probe_no_downstream_guard.sh --config FILE   # check one file (fixture/test)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CONFIG_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --config) CONFIG_OVERRIDE="$2"; shift 2 ;;
        --config=*) CONFIG_OVERRIDE="${1#--config=}"; shift ;;
        *) echo "liveness_probe_no_downstream_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Downstream HealthIndicator ids that must NOT gate liveness (Spring Boot auto-config
# indicator names, matched case-insensitively as whole tokens).
DOWNSTREAM='db|datasource|r2dbc|redis|mongo|mongodb|cassandra|couchbase|elasticsearch|neo4j|ldap|mail|jms|rabbit|kafka|solr|influxdb|hazelcast'

# Emit every token listed under a `liveness:` group's `include:` (inline CSV value
# and/or subsequent YAML list items), plus the flattened-key form. Indent-scoped so
# the block ends at the first same-or-shallower key — readiness is never read.
extract_liveness_includes() {
    local file="$1"
    awk '
        function indent(s,   i) { i = match(s, /[^ ]/); return (i ? i - 1 : length(s)) }
        # Flattened form anywhere: ...group.liveness.include: a, b, c
        /group\.liveness\.include[[:space:]]*:/ {
            v = $0; sub(/.*include[[:space:]]*:[[:space:]]*/, "", v)
            print v; next
        }
        {
            ind = indent($0)
            if (in_inc) {
                if ($0 ~ /^[[:space:]]*-[[:space:]]*/) { it=$0; sub(/^[[:space:]]*-[[:space:]]*/,"",it); print it; next }
                if ($0 ~ /^[[:space:]]*[A-Za-z0-9_.-]+[[:space:]]*:/ && ind <= inc_ind) in_inc = 0
            }
            if (in_liveness && $0 ~ /^[[:space:]]*[A-Za-z0-9_.-]+[[:space:]]*:/ && ind <= liveness_ind && $0 !~ /^[[:space:]]*liveness[[:space:]]*:/) {
                in_liveness = 0
            }
            if (in_liveness && $0 ~ /^[[:space:]]*include[[:space:]]*:/) {
                v=$0; sub(/.*include[[:space:]]*:[[:space:]]*/,"",v)
                if (v ~ /[^[:space:]]/) print v   # inline CSV value
                in_inc = 1; inc_ind = ind; next
            }
            if ($0 ~ /^[[:space:]]*liveness[[:space:]]*:/) { in_liveness = 1; liveness_ind = ind; in_inc = 0 }
        }
    ' "$file"
}

check_file() {
    local file="$1" bad=0
    [ -f "$file" ] || return 0
    local raw line t clean
    raw="$(extract_liveness_includes "$file")"
    [ -z "$raw" ] && return 0
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        # Normalize separators (commas/brackets/quotes/colons) to spaces, lowercase.
        clean="$(printf '%s' "$line" | tr 'A-Z' 'a-z' | sed 's/[^a-z0-9_-]/ /g')"
        for t in $clean; do
            if printf '%s\n' "$t" | grep -qiE "^(${DOWNSTREAM})$"; then
                echo "VIOLATION: $file — liveness health group includes downstream indicator '$t'" >&2
                echo "  liveness MUST NOT gate on a dependency (HEALTH-LIVENESS-001): a '$t' outage would restart-loop every pod. Move '$t' to the readiness group." >&2
                bad=1
            fi
        done
    done <<< "$raw"
    return $bad
}

violations=0
if [ -n "$CONFIG_OVERRIDE" ]; then
    check_file "$CONFIG_OVERRIDE" || violations=1
else
    shopt -s nullglob
    files=("$REPO_ROOT"/backend/src/main/resources/application*.yml "$REPO_ROOT"/backend/src/main/resources/application*.yaml)
    shopt -u nullglob
    if [ ${#files[@]} -eq 0 ]; then
        echo "liveness_probe_no_downstream_guard: no backend application*.yml found — nothing to check"
        exit 0
    fi
    for f in "${files[@]}"; do
        check_file "$f" || violations=1
    done
fi

if [ "$violations" -ne 0 ]; then
    echo "" >&2
    echo "liveness_probe_no_downstream_guard: liveness probe gated on a downstream dependency — BLOCKED" >&2
    exit 1
fi

echo "liveness_probe_no_downstream_guard: PASS — no liveness health group gates on a downstream dependency (HEALTH-LIVENESS-001)"
exit 0
