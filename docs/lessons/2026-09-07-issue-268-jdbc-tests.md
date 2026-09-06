# Issue #268 — 공개 JDBC 테스트 fixture 이전 교훈

## 결정

두 consumer 모듈의 private `project(":exposed-shared-tests")` 의존성을
Maven Central의 `io.github.bluetape4k.exposed:bluetape4k-exposed-jdbc-tests:2.0.0`
테스트 스코프 의존성으로 교체했다. 버전 없는 catalog alias를 추가해
Bluetape4k 버전 권한은 기존 안정 BOM `2.0.0`에 남겼다.

## 범위 조사

기준 브랜치에서 private fixture를 참조한 Gradle consumer는 44개였다. 이 중
테스트 소스에서 fixture API를 실제로 사용하는 모듈은 30개, 의존성만 선언한
모듈은 14개였다. 이번 이슈는 의존성만 선언한 Spring MVC 두 모듈을 우선
이전하고, 두 모듈에 공개 provider의 schema lifecycle smoke test를 추가했다.
이전 후 private fixture consumer는 42개다. 남은 consumer는 각자의 fixture 사용
범위가 있어 이번 PR에 섞지 않는다.

공개 `TestDB` matrix에는 H2, MariaDB, MySQL, PostgreSQL 계열이 있고 private
matrix에만 있던 CockroachDB는 포함되지 않는다. 이 차이는 공개 provider의
계약으로 기록하고, consumer가 CockroachDB를 사용해야 하는 후속 이슈로 남긴다.

## RED → GREEN

- 기준선: `:spring-mvc-exposed:test` 49개가 통과했다.
- 첫 공개 의존성 적용: public artifact가 전이시키는
  `org.jetbrains.exposed:exposed-spring-boot4-starter:1.4.0`와 consumer의
  `exposed-spring-boot-starter:1.4.0`가 함께 test runtime에 들어와
  `springTransactionManager` bean override로 `processTestAot`가 실패했다.
- 최소 수정: 두 build 파일에서 공개 fixture를
  `testImplementation(...){ isTransitive = false }`로 선언했다. consumer가
  이미 명시한 Exposed/JDBC/Testcontainers 의존성만 사용하게 해 AOT 충돌을
  제거했다.
- 공개 provider smoke test의 `ProviderTable`은 Spring migration scanner가
  접근할 수 있도록 public object로 두었다. `withDb` cleanup 검증에는
  `configure = {}`를 명시해 fixture가 이전 consumer의 DB 설정을 남기지 않게
  했다.
- 최종: `:spring-mvc-exposed:test` 50개, `:04-exposed-repository:test` 39개
  (1 skipped)가 모두 통과했고, 두 smoke test도 별도로 통과했다.

## 재현 명령

```bash
./gradlew :spring-mvc-exposed:cleanTest :spring-mvc-exposed:test \
  -PuseFastDB=true --no-daemon --no-configuration-cache --no-build-cache --console=plain
./gradlew :04-exposed-repository:cleanTest :04-exposed-repository:test \
  -PuseFastDB=true --no-daemon --no-configuration-cache --no-build-cache --console=plain
```

공개 artifact POM과 sources JAR는 Maven Central에서 HTTP 200으로 확인했다.
private fixture의 공용 API 자체는 provider 저장소가 소유하므로 consumer PR에서
중복 계약 테스트를 확장하지 않는다.

## 후속 조치

다음 공개 provider 릴리스에서 Spring Boot 자동설정 전이가 분리되거나 artifact가
consumer-safe variant를 제공하면 `isTransitive = false` 우회 선언의 필요성을
재검토한다. 그 전까지는 이 우회를 제거하지 않는다.
