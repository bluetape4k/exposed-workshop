# Issue 61 HTTP outbox idempotency 계획

## 검색 근거

- GNO similar work lookup: 완료.
- GNO caution lookup: 완료.
- Current issue body: GitHub issue #61에서 검증함.
- `$bluetape4k-patterns`: 모든 Kotlin task에 적용함.

## 작업

1. Spring Boot 4 module을 만든다.
   - Gradle build를 추가한다.
   - Exposed persistence/repository를 추가한다.
   - `RestClient` outbound client를 추가한다.
   - Service와 MVC controller/advice를 추가한다.
   - Repository, service, MockMvc test를 추가한다.

2. Ktor module을 만든다.
   - Gradle build를 추가한다.
   - Exposed persistence/repository를 추가한다.
   - Ktor `HttpClient` outbound client를 추가한다.
   - Service, plugin, route를 추가한다.
   - Repository, service, route test를 추가한다.

3. Docs와 workflow를 갱신한다.
   - English/Korean module README file을 추가한다.
   - Chapter README file을 갱신한다.
   - Root README file을 갱신한다.
   - 두 module을 `.github/workflows/examples.yml`에 추가한다.

4. 검증한다.
   - `./gradlew projects --no-daemon`
   - `./gradlew :03-spring-http-outbox-idempotency:test --no-daemon`
   - `./gradlew :04-ktor-http-outbox-idempotency:test --no-daemon`
   - Examples workflow equivalent build.
   - `actionlint .github/workflows/examples.yml`
   - `git diff --check`

5. 전달한다.
   - 간결한 lesson을 기록한다.
   - Lore trailer를 포함해 commit한다.
   - `debop`에게 assign된 PR을 연다.
   - Examples success를 기다린다.
   - Merge하고 local `develop`을 sync한다.
