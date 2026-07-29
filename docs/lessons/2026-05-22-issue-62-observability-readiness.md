# Issue 62 Observability readiness

## 배경

Issue #62는 database-backed service의 operational diagnostics를 보여 주는 paired Spring
Boot 4/Ktor 예제를 요구했다.

## 결정

Readiness를 기존 Ktor architecture baseline에 합치지 않고 focused 12장 모듈 두 개를 추가한다.

- `09-spring-observability-readiness`
- `10-ktor-observability-readiness`

Spring은 Actuator readiness group과 custom database health contributor를 사용한다. Ktor는
`CallId`, `CallLogging`, `StatusPages`를 갖춘 명시적 `/readyz` route를 사용한다.

## 결과

두 모듈은 Exposed JDBC를 통해 slow-operation diagnostics를 저장하고, `X-Request-ID`를
sanitize/propagate하며, structured error를 반환하고, in-process state로 degraded database
readiness를 테스트한다.

README architecture section은 `docs/images/readme-diagrams/` 아래 generated PNG diagram을
사용하고 SVG source를 옆에 보관한다.

Repository guidance는 이제 모든 예제 README에 Architecture Diagram PNG/SVG pair를 요구한다.
Architecture diagram만으로 관계나 flow가 명확하지 않으면 class, sequence, ERD 또는 다른
UML-style diagram을 추가한다.

## 검증

```bash
./gradlew :09-spring-observability-readiness:test :10-ktor-observability-readiness:test --stacktrace --continue
```

Claude review 수정 후 결과: Spring 6 test passing, Ktor 6 test passing with
`--rerun-tasks`.

Image link 추가 후 README diagram link에서 missing local PNG target과 Mermaid residue를
확인했다.

현재 12장 위로 rebase한 뒤 모듈 번호를 09/10으로 다시 매기고, chapter English/Korean
index README file을 갱신했으며, regenerated PNG diagram을 직접 열어 title과 label이 09/10에
맞는지 확인했다. 최종 chapter README scan은 `readmes=20`, `missingPng=0`,
`missingArchitecture=0`, `mermaidResidue=0`, `missingFiles=0`을 보고했다.
`./gradlew projects --quiet`는 두 새 모듈을 모두 표시했고, `git diff --check`도 통과했다.

Claude 6-Tier review는 PR 생성 전에 P1 한 건과 여러 P2/P3를 찾았다. Spring
method-level parallel test가 `DiagnosticsState`를 공유했고, request ID가 validation 없이
echo됐으며, Spring readiness가 repository exception을 escape하게 했고, Ktor schema
initialization은 DDL success 전에 complete로 표시될 수 있었다. Rerun은 Spring framework
4xx error와 catch-all handler의 unexpected exception도 지적했다. 이제 이들은 structured
400 response를 반환하거나 sanitized 500 반환 전에 server-side log를 남긴다. 모두 수정했고
최종 targeted test run으로 검증했다.

최종 Claude 6-Tier rerun artifact:
`.omx/artifacts/claude-issue-62-code-review-6tier-stdin-6min-20260522184325.md`.
It reported `P0=0`, `P1=0`, `P2=0`, `P3=2`, verdict `PASS`.

## 향후 agent 지침

Spring Boot 4 health class는 Spring Boot 3의 `org.springframework.boot.actuate.health.*`
package가 아니라 `org.springframework.boot.health.*` 아래에 있다.

예제에서 caller-supplied correlation header를 그대로 echo하지 않는다. Response header, log,
persisted row에 넣기 전에 cap과 sanitize를 적용한다.
