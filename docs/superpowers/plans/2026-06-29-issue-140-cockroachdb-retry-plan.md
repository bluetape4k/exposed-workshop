# Issue #140 CockroachDB serializable retry workshop 계획

일자: 2026-06-29
이슈: https://github.com/bluetape4k/exposed-workshop/issues/140

## 단계 1 - RED test

작업: 원하는 workshop API를 참조하는 module skeleton과 test를 추가한다.

DoD: `./gradlew :03-cockroachdb-retry:test`는 workshop implementation 또는 catalog wiring이
없어서만 실패한다.

## 단계 2 - 구현

작업: Public `bluetape4k-exposed-cockroachdb` API를 사용해 inventory reservation helper를
구현한다.

DoD: Test는 successful commit, retryable failure replay, non-retryable failure boundary, schema
bootstrap을 증명한다.

## 단계 3 - 문서와 diagram

작업: `README.md`, `README.ko.md`, chapter/root README link, 검증된 SVG+PNG sequence diagram을
추가한다.

DoD: README locale pair는 같은 PNG를 embed하고 raw Mermaid 없이 local Testcontainers path를
설명한다.

## 단계 4 - Workflow와 검증

작업: Module을 Examples workflow에 추가하고 targeted local check를 실행하며 diff를 review하고
commit, PR open, CI monitor를 수행한 뒤 green 이후 merge하고 local develop을 sync한다.

DoD: Local verification이 통과하고 PR이 issue metadata를 반영하며 CI가 통과한다. PR은 develop에
반영되고 issue #140은 닫힌다.
