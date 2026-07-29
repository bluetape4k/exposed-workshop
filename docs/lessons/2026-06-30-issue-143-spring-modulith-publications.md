# Issue 143 Spring Modulith publication

## 배경

Issue #143은 `bluetape4k-exposed-spring-modulith`를 기반으로 하는 local Spring Modulith
publication-store 예제 `13-ecosystem-integrations/06-spring-modulith-publications`를
추가했다.

## 결정

- Exposed Modulith auto-configuration이 publication repository를 만들 수 있도록 in-memory H2와
  명시적 `springTransactionManager` bean을 사용한다.
- 명시적인 Jackson 3 `EventSerializer`를 제공한다. 이것이 없으면 workshop reader가 repository
  condition chain을 진단하기 어렵다.
- 예제는 completed publication, failed publication resubmission, unloadable event row라는 세
  operational path에 집중한다.

## Diagram 보호 장치

두 route가 corridor를 공유하면 connector script가 same-line overlap을 놓칠 수 있다. Automated
audit이 통과한 뒤 full-size PNG를 검사하고 same-color 또는 cross-color route sharing을 눈으로
확인한다. 이 예제에서는 retry route를 lower recovery corridor로 옮겨 더 이상 migration-guard
path를 공유하지 않게 했다.

## 검증

- `./gradlew :06-spring-modulith-publications:test --no-daemon --no-configuration-cache`
- `./gradlew :06-spring-modulith-publications:build --no-daemon --no-configuration-cache`
- `./gradlew projects --no-daemon --no-configuration-cache`
- `actionlint .github/workflows/examples.yml`
- `git diff --check`
- `xmllint`, CairoSVG render, endpoint, geometry, mixed-corner, connector audits,
  and full-size PNG inspection for the README diagram.
