#!/usr/bin/env bash
set -euo pipefail

catalog="${1:-gradle/libs.versions.toml}"
authority_expected_version="2.0.0-SNAPSHOT"

if [[ ! -f "$catalog" ]]; then
  printf 'catalog expected=%s actual=<missing> action=provide-a-valid-catalog-path\n' "$catalog"
  exit 2
fi

expected_keys='
ktor|3.5.2
hikaricp|7.1.0
postgresql-driver|42.7.13
redisson|4.7.0
netty|4.2.17.Final
jackson|2.22.1
jackson3|3.2.1
caffeine|3.2.4
fory-kotlin|1.5.0
agroal|3.2.1
vertx|5.1.6
hibernate|7.4.5.Final
hibernate-reactive|4.5.2.Final
spring-modulith|2.1.0
mockk|1.14.11
logback|1.5.34
zstd-jni|1.5.7-12
datafaker|2.7.0
kotlinx-benchmark|0.4.17
lz4-java|1.11.1
micrometer|1.17.0
springdoc-openapi|3.1.0
kover|0.9.9
mysql-connector-j|9.7.0
guava|33.6.0-jre'

actual_for() {
  local key="$1"
  awk -v key="$key" '$0 ~ "^" key "[[:space:]]*=" {
    line = $0
    sub(/^[^=]*=[[:space:]]*"/, "", line)
    sub(/".*$/, "", line)
    print line
    exit
  }' "$catalog"
}

failed=0
authority_actual_version="$(actual_for bluetape4k-dependencies)"
if [[ "$authority_actual_version" != "$authority_expected_version" ]]; then
  printf 'bluetape4k-dependencies expected=%s actual=%s action=review-release-authority-before-updating-governed-keys\n' \
    "$authority_expected_version" "${authority_actual_version:-<missing>}"
  failed=1
else
  printf 'bluetape4k-dependencies expected=%s actual=%s status=ok\n' \
    "$authority_expected_version" "$authority_actual_version"
fi

while IFS='|' read -r key expected_value; do
  [[ -z "$key" ]] && continue
  actual="$(actual_for "$key")"
  if [[ "$actual" != "$expected_value" ]]; then
    printf '%s expected=%s actual=%s action=align-with-2.0.0-SNAPSHOT-catalog\n' \
      "$key" "$expected_value" "${actual:-<missing>}"
    failed=1
  else
    printf '%s expected=%s actual=%s status=ok\n' "$key" "$expected_value" "$actual"
  fi
done <<EOF
$expected_keys
EOF

exit "$failed"
