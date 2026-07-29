# Issue 58 - Ktor 아키텍처 기준선

## 배경

12장은 production integration 예제로 시작한다. 첫 번째 모듈에는 auth,
outbox, client, observability 예제를 추가하기 전에 작은 Ktor 기준선이
필요했다.

## 결정

route/service/repository 경계가 명확한 Ktor + Exposed JDBC 모듈을 사용한다.
blocking Exposed transaction은 `Dispatchers.IO`로 `transaction {}`을 감싸는
suspend repository API 안에 둔다.

## 결과

Ktor JSON, StatusPages, CallId/CallLogging, H2 persistence, route test, 영어/한국어
README 파일을 포함한 `12-production-integration/01-ktor-application-architecture`를
추가했다.

Claude 구현 리뷰는 첫 커밋 뒤 두 문제를 잡았다. fallback 500 handler가 root
cause를 로그로 남기지 않았고, body limit이 `Content-Length`만 신뢰했다. 첫
수정은 `receiveText()` 이후 크기를 확인했지만, 그 시점에는 body가 이미
buffering되어 있어 Claude가 올바르게 반려했다. 최종 버전은 예상하지 못한
실패를 로그로 남기고, JSON decoding 전에 request body chunk를 streaming하면서
크기 제한을 적용한다.

## 검증

- `./gradlew -q projects`
- `./gradlew :01-ktor-application-architecture:compileKotlin`
- `./gradlew :01-ktor-application-architecture:compileTestKotlin`
- `./gradlew :01-ktor-application-architecture:test` - 8 passing
- `./gradlew --offline :01-ktor-application-architecture:compileKotlin :01-ktor-application-architecture:compileTestKotlin :01-ktor-application-architecture:test` - 8 passing after review fixes
- `./gradlew detekt` - `NO-SOURCE`
- `git diff --check`
- Claude Code implementation final re-review - PASS, P0 = 0, P1 = 0

IntelliJ에서 프로젝트를 연 뒤 IDE batch diagnostics는 문제 0건을 보고했지만,
CLI diagnostics는 editor-fresh 상태가 아니었다. Online Gradle resolution은
`io.github.bluetape4k.aws:bluetape4k-aws-bom:0.1.0` 외부 snapshot POM 때문에
막혔고, 같은 targeted compile/test 검증은 `--offline`으로 통과했다. CI workflow
변경은 필요하지 않았다. daily CI가 repository-wide Gradle test를 실행하고, 새
H2-only 모듈은 `settings.gradle.kts`를 통해 포함되기 때문이다.

## 향후 지침

향후 Ktor + JDBC 예제에서는 route에서 Exposed `transaction {}`을 직접 호출하지
않는다. blocking 경계는 repository에 두고, 예제가 service architecture를
보여 준다면 parallel write 경로를 최소 하나 테스트한다. request limit은
`Content-Length`만 신뢰하지 말고 실제로 읽은 byte 기준으로도 적용한다.
