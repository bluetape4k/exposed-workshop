# Issue #142 계획 - 명시적 Ktor Exposed integration

## 단계 1 - Source 및 requirement grounding

- 작업: issue #142, chapter 13 README, existing Ktor example,
  `bluetape4k-exposed-ktor` source/demo code를 검사한다.
- DoD: 선택한 module path, helper API, acceptance evidence를 design artifact에 기록한다.

## 단계 2 - TDD Contract

- 작업: Production code보다 먼저 새 module skeleton과 test를 추가한다.
- DoD: `./gradlew :05-ktor-exposed-integration:test --no-daemon --no-configuration-cache`
  는 production API가 의도적으로 없기 때문에 compile time에 실패한다.

## 단계 3 - 구현

- 작업: Local H2 Ktor example, caller-owned resource, CRUD route, readiness route, sanitized
  error route를 구현한다.
- DoD: Targeted module test가 local에서 통과한다.

## 단계 4 - 문서와 diagram

- 작업: Bilingual module README file과 `$bluetape4k-diagram` compliant SVG/PNG architecture
  diagram을 추가한다.
- DoD: SVG geometry/endpoint audit가 통과하고 PNG가 성공적으로 render되며 README link가 rendered
  asset을 가리킨다.

## 단계 5 - 등록과 자동화

- 작업: Chapter/root README link, version catalog alias, `.github/workflows/examples.yml`을
  갱신한다.
- DoD: Gradle project discovery가 module을 포함하고 Examples workflow가
  `:05-ktor-exposed-integration:build`를 포함한다.

## 단계 6 - 검증과 PR

- 작업: Targeted test, `git diff --check`를 실행하고 local diff를 review하며 review note와
  lesson을 작성한 뒤 issue #142에 연결된 PR을 연다.
- DoD: PR metadata는 issue #142 assignee, label, milestone을 반영하고 PR body는
  `## DoD Status`로 끝난다.
