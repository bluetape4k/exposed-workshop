#!/usr/bin/env bash

set -euo pipefail

# Keep this list aligned with the fixed weekly Examples gate. The path mapping
# below is intentionally complete for this list; unknown paths fall back to all
# modules instead of silently dropping coverage.
ALL_TASKS=(
  :03-routing-datasource:build
  :04-schema-per-tenant-spring-web:build
  :05-database-per-tenant-spring-web:build
  :06-spring-security-tenant-authorization-spring-web:build
  :01-ktor-application-architecture:build
  :02-spring-application-architecture:build
  :03-spring-http-outbox-idempotency:build
  :04-ktor-http-outbox-idempotency:build
  :05-spring-auth-session:build
  :06-ktor-auth-session:build
  :07-spring-outbox-realtime:build
  :08-ktor-outbox-realtime:build
  :09-spring-observability-readiness:build
  :10-ktor-observability-readiness:build
  :01-bigquery-dry-run:build
  :02-trino-session-options:build
  :03-cockroachdb-retry:build
  :04-starrocks-olap-local:build
  :05-ktor-exposed-integration:build
  :06-spring-modulith-publications:build
  :07-ddd-aggregate-repository:build
  :08-ddd-modulith-boundaries:build
  :09-duckdb-embedded-analytics:build
  :10-druid-query-only:build
)

all=false
declare -a selected=()

add_task() {
  local task="$1"
  local candidate
  for candidate in "${selected[@]-}"; do
    [[ "$candidate" == "$task" ]] && return
  done
  selected+=("$task")
}

if [[ "${FORCE_ALL:-false}" == "true" ]]; then
  all=true
else
  diff_range="${1:-}"
  if [[ -z "$diff_range" ]]; then
    echo "A git diff range is required unless FORCE_ALL=true." >&2
    exit 2
  fi

  while IFS= read -r path; do
    case "$path" in
      10-multi-tenant/04-schema-per-tenant-spring-web/**)
        add_task :04-schema-per-tenant-spring-web:build
        ;;
      10-multi-tenant/05-database-per-tenant-spring-web/**)
        add_task :05-database-per-tenant-spring-web:build
        ;;
      10-multi-tenant/06-spring-security-tenant-authorization-spring-web/**)
        add_task :06-spring-security-tenant-authorization-spring-web:build
        ;;
      11-high-performance/03-routing-datasource/**)
        add_task :03-routing-datasource:build
        ;;
      12-production-integration/*/**)
        module="${path#12-production-integration/}"
        module="${module%%/*}"
        add_task ":$module:build"
        ;;
      13-ecosystem-integrations/*/**)
        module="${path#13-ecosystem-integrations/}"
        module="${module%%/*}"
        add_task ":$module:build"
        ;;
      .github/workflows/examples.yml|.github/scripts/select-changed-examples.sh|buildSrc/**|gradle/**|settings.gradle.kts|*.gradle.kts)
        all=true
        ;;
      *.md|docs/**)
        # README/diagram-only companions do not add a Gradle module. If they
        # are the only changed paths, the empty selection below conservatively
        # restores the historical all-module gate.
        ;;
      *)
        # The workflow path filters should prevent this branch for normal
        # runs. Fail open if a new selected path is added without a mapping.
        all=true
        ;;
    esac
  done < <(git diff --name-only "$diff_range")
fi

if [[ "$all" == "true" || ${#selected[@]} -eq 0 ]]; then
  selected=("${ALL_TASKS[@]}")
  all=true
fi

printf 'all=%s\n' "$all"
printf 'module_tasks=%s\n' "${selected[*]}"
