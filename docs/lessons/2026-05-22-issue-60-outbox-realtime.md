# Issue 60 - Realtime outbox 예제

## 배경

Issue #60은 12장에 database-backed realtime delivery를 위한 paired Spring Boot 4/Ktor
예제를 추가한다. 사용자는 모든 예제 README에 PNG Architecture Diagram도 포함해야 한다고
요구했다.

## 결정

기존 #59 worktree module number와 충돌하지 않도록 `07-spring-outbox-realtime`와
`08-ktor-outbox-realtime` 모듈을 분리해 사용한다. 예제는 작게 유지한다. 하나의 Exposed
transaction에서 notification row와 outbox row를 저장한 뒤, pending row를 in-process
SSE/WebSocket hub로 publish한다.

## 결과

Spring 예제는 one-way notification delivery에 WebFlux Server-Sent Events를 사용한다.
Ktor 예제는 reconnect/replay와 향후 bidirectional command room을 위해 WebSockets를
사용한다. 두 예제 모두 event를 버리지 않고 delivery failure를 outbox state로 기록한다.

## 검증

- `./gradlew :07-spring-outbox-realtime:test :08-ktor-outbox-realtime:test --stacktrace --continue`
- README PNG link는 script로 검증했다.
- PNG diagram은 SVG에서 rendering한 뒤 시각적으로 확인했다.
- Claude advisor review는 처음에 `P0=0 P1=4`를 찾았다. Spring SSE dedupe,
  Spring/Ktor live endpoint test, FAILED-row README semantics를 수정했다.
- Claude advisor rerun은 stdin과 6-minute timeout으로 `P0=0 P1=0`을 반환했다.

## 향후 보호 장치

여러 issue branch가 12장 모듈을 독립적으로 추가할 때 같은 numeric module prefix를 재사용하지
않는다. 최종 ordering 조정은 integration 중에만 수행한다.

Claude Code CLI advisor gate에서는 stdin-compatible invocation을 사용하고 review prompt에
최소 5분을 허용한다. 더 짧은 RPC/tool timeout은 Claude가 응답하기 전에 유효한 review를
중단시킬 수 있다.
