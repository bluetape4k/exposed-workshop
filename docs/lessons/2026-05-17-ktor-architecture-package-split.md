# Ktor 아키텍처 패키지 분리

## 배경

첫 Ktor 아키텍처 예제는 의도적으로 작게 만들었지만, routes, services,
repositories, persistence, models를 한 파일에 두면 production-integration
학습 흐름을 훑어보기 어려웠다.

## 결정

customer model은 하나의 model package에 유지하고, 나머지 구현은 application
wiring, Ktor config, routes, service, repository, persistence 계층으로 나눈다.
테스트도 하나의 넓은 HTTP integration test에 기대지 않고 같은 구조를 반영한다.
짧은 H2 database suffix에는 UUID 문자열 대신 `Base58.randomString(8)`을 사용한다.

## 결과

이제 모듈은 package layout으로 아키텍처 경계를 보여 준다. 테스트는 application
wiring, routes, service behavior, repository persistence/concurrency 단위로
분리됐다.

## 검증

- `./gradlew --offline :01-ktor-application-architecture:compileKotlin :01-ktor-application-architecture:compileTestKotlin :01-ktor-application-architecture:test` - 13 passing
- `./gradlew --offline detekt` - `NO-SOURCE`
- `git diff --check`
- IntelliJ optimize imports and batch diagnostics - zero problems, not fresh editor highlights
- Claude Code refactor review - PASS, P0 = 0, P1 = 0

## 향후 지침

아키텍처 중심 Ktor 예제에서 routing을 넘어서는 내용이 들어가면 single-file
구현을 피한다. domain이 작을 때 model DTO는 함께 두되, route, service,
repository, persistence 코드는 나눠 독자가 의도한 production shape를 바로 볼 수
있게 한다.
