# Issue 54 Spring Security tenant authorization

## 배경

10장에는 선택된 tenant를 `X-Tenant-ID`만으로 신뢰하지 않고 authenticated identity에서
authorize하는 Spring Security multi-tenant 예제가 필요했다.

## 결정

JWT bearer token, API key, demo session header라는 세 가지 demo credential source를
받는 dedicated Spring Web 예제 모듈을 추가한다. 이 모듈은 mixed credential source를
거부하고, selector validation 전에 authenticated tenant를 resolve하며, tenant match 이후에만
`TenantContext`를 bind하고 `finally`에서 context를 지운다.

Architecture diagram은 README file에서 PNG-first로 유지하고, SVG source는
`docs/images/readme-diagrams/` 아래에 저장한다.

## 결과

새 모듈에는 English/Korean README file, architecture/request flow PNG diagram, selected
examples CI wiring, 그리고 auth, tenant mismatch, malformed claims/selectors,
cross-tenant isolation, context cleanup, rollback, registry lifecycle,
source-text architecture guard를 다루는 30개 test가 포함된다.

## 검증

- `./gradlew :06-spring-security-tenant-authorization-spring-web:build --stacktrace --continue --console=plain`
- `actionlint .github/workflows/examples.yml`
- `git diff --check`
- README scan으로 새 module README file에 Mermaid가 없음을 확인했다.
- Diagram asset은 1280x760 RGB PNG로 rendering하고 시각적으로 검사했다.
- Claude advisor/code-review artifact는 `P0=0, P1=0`에 도달했다. Final artifact:
  `.omx/artifacts/claude-issue-54-code-review-final-stdin-6min-20260523013657.md`.

## 향후 지침

Security 예제에서는 custom filter와 `SecurityFilterChain` authorization rule 양쪽에서
health endpoint bypass behavior를 검토한다. Demo auth fixture가 의도적으로 insecure하다면
KDoc과 README text에 production boundary를 명시한다.
