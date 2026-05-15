#!/usr/bin/env bash
# practices/upstream/fetch.sh — 3-tier upstream documentation fetcher.
#
# tier-1: chub get (Java/Kotlin unsupported in current registry → almost always skips)
# tier-2: Context7 query-docs (no CLI in this environment → emits WebFetch-fallback TODO)
# tier-3: curl from public docs URL (default actual fetch path for Java/Spring)
#
# 2026-05-15 URL revision: switched from `htmlsingle/index.html` navigation pages to
# topic-level deep references so the snapshot body actually contains the prose each rule
# claims. After this revision, `upstream_id` evidence in rule frontmatter becomes usable.
set -euo pipefail

cd "$(dirname "$0")/../.."

DRY_RUN=false
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=true

# Each entry: id|url. The id is what rule.md frontmatter references via `upstream_id`.
# Keep ids short and topic-scoped — one id per cited section.
SNAPSHOTS=(
  "spring-jpa-fetching|https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
  "spring-tx-declarative|https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
  "spring-aop-proxying|https://docs.spring.io/spring-framework/reference/core/aop/proxying.html"
  "spring-mvc-modelattribute|https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
  "spring-beans-scopes|https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html"
  "spring-beans-constructor-injection|https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html"
  "spring-boot-testing|https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html"
  "owasp-mass-assignment|https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html"
  "rest-assured-usage|https://github.com/rest-assured/rest-assured/wiki/Usage"
  "cwe-915|https://cwe.mitre.org/data/definitions/915.html"
  "rfc-7807|https://datatracker.ietf.org/doc/html/rfc7807"
  "spring-mvc-exception-handler|https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"
  "spring-mvc-controlleradvice|https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html"
  "owasp-api-error-handling|https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/"
  "slf4j-mdc|https://www.slf4j.org/manual.html"
  "logback-layouts|https://logback.qos.ch/manual/layouts.html"
  "owasp-logging-cheatsheet|https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html"
  "spring-mvc-validation|https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
  "hibernate-validator|https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/"
  "jep-444-virtual-threads|https://openjdk.org/jeps/444"
  "spring-task-execution|https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html"
  "spring-scheduling|https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
  "spring-data-paging|https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
  "google-aip-versioning|https://google.aip.dev/180"
  "gradle-toolchains|https://docs.gradle.org/current/userguide/toolchains.html"
  "spring-dependency-management|https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/"
  "spring-rest-clients|https://docs.spring.io/spring-framework/reference/integration/rest-clients.html"
  "archunit-userguide|https://www.archunit.org/userguide/html/000_Index.html"
  "jep-395-records|https://openjdk.org/jeps/395"
  "jep-409-sealed-classes|https://openjdk.org/jeps/409"
  "spring-boot-external-config|https://docs.spring.io/spring-boot/reference/features/external-config.html"
  "spring-boot-profiles|https://docs.spring.io/spring-boot/reference/features/profiles.html"
  "spring-boot-actuator-endpoints|https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
  "spring-security-csrf|https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html"
  "spring-security-headers|https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html"
  "spring-security-stateless|https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html"
  "spring-boot-sql-migration|https://docs.spring.io/spring-boot/reference/data/sql.html"
  "spring-cache-abstraction|https://docs.spring.io/spring-framework/reference/integration/cache.html"
  "spring-boot-cache|https://docs.spring.io/spring-boot/reference/io/caching.html"
  "spring-application-events|https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
)

MANIFEST="practices/upstream/_MANIFEST.yaml"
TODAY="$(date -u +%FT%TZ)"

try_chub() {
  local id="$1"
  command -v chub >/dev/null 2>&1 || return 1
  chub get "$id" --lang java >/dev/null 2>&1
}

context7_stub() {
  echo "# TODO[Context7]: WebFetch fallback for id $1 — no CLI available in this env" >&2
  return 1
}

curl_fetch() {
  local url="$1" out="$2"
  curl -sSL --max-time 30 \
    -H "User-Agent: ax-template-practices-fetch/0.2" \
    -o "$out" "$url" 2>/dev/null
}

emit_manifest() {
  {
    echo "version: \"1.1\""
    echo "generated_at: \"$TODAY\""
    echo "snapshots:"
    for entry in "${SNAPSHOTS[@]}"; do
      local id="${entry%%|*}"
      local src="${entry##*|}"
      local path="practices/upstream/${id}.snapshot.md"
      [[ -f "$path" ]] || continue
      local sha
      sha="$(shasum -a 256 "$path" | awk '{print $1}')"
      local size
      size="$(wc -c < "$path" | tr -d ' ')"
      echo "  - id: \"$id\""
      echo "    tier: 3"
      echo "    source: \"$src\""
      echo "    via: \"curl\""
      echo "    fetched_at: \"$TODAY\""
      echo "    bytes: $size"
      echo "    sha: \"$sha\""
    done
  } > "$MANIFEST"
}

main() {
  for entry in "${SNAPSHOTS[@]}"; do
    local id="${entry%%|*}"
    local src="${entry##*|}"
    local path="practices/upstream/${id}.snapshot.md"
    echo "[$id] tier-1 chub (java unsupported, skipping)"
    try_chub "$id" || true
    echo "[$id] tier-2 Context7 stub"
    context7_stub "$id" || true
    echo "[$id] tier-3 curl <- $src"
    if $DRY_RUN; then
      echo "  [dry-run] would write $path"
      continue
    fi
    if curl_fetch "$src" "$path" && [[ -s "$path" ]]; then
      echo "  fetched: $path ($(wc -c < "$path" | tr -d ' ') bytes)"
    else
      cat > "$path" <<EOF
# $id snapshot — placeholder (offline / fetch failed)

Source: $src
Fetched at: $TODAY
Note: replace with real content when network access is available; sha will change on next refresh.
EOF
      echo "  placeholder written: $path"
    fi
  done
  if ! $DRY_RUN; then
    emit_manifest
    echo "manifest: $MANIFEST"
  else
    if [[ ! -f "$MANIFEST" ]]; then
      printf 'version: "1.1"\nsnapshots: []\n' > "$MANIFEST"
    fi
  fi
}

main "$@"
