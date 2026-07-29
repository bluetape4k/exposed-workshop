# Issue #142 설계 - 명시적 Ktor Exposed integration

## 배경

Issue #142는 Exposed 1.11 line에 추가된 명시적 `bluetape4k-exposed-ktor` integration을
소개하는 workshop example을 요구한다. 기존 12장 Ktor 예제는 transaction boundary를 의도적으로
application repository 안에서 소유한다. 이 예제는 older module을 다시 작성하지 않고 학습자가
두 style을 비교할 수 있도록 dedicated helper API를 보여 줘야 한다.

## 대상 모듈

- Directory: `13-ecosystem-integrations/05-ktor-exposed-integration`
- Gradle task: `:05-ktor-exposed-integration:build`
- Default runtime: local in-memory H2 JDBC plus H2 R2DBC readiness probe
- Public title: `Explicit Ktor Exposed Integration`

## 학습 계약

모듈은 다음을 보여 준다.

- `installBluetape4kExposedKtor`에 전달되는 caller-owned JDBC/R2DBC resource.
- Helper-provided `/healthz/exposed`, `/readyz/exposed` route.
- Blocking JDBC boundary로서의 `ApplicationCall.exposedJdbcTransaction`.
- `bluetape4kErrorResponses()`와 `bluetape4kExposedErrors()`를 조합한 Ktor `StatusPages`.
- SQL, JDBC URL, credential을 leak하지 않는 sanitized database error response.

## 비목표

- 10장, 11장, 12장 Ktor 예제를 대체하지 않는다.
- Real external service, cloud credential, network prerequisite를 도입하지 않는다.
- Full production service template를 만들지 않는다. 예제는 작고 integration helper surface에
  집중한다.

## 수용 근거

- Ktor `testApplication`은 JDBC helper transaction을 통한 note CRUD를 검증한다.
- Ktor `testApplication`은 JDBC/R2DBC probe의 readiness success를 검증한다.
- Caller-owned JDBC resource를 사용할 수 없을 때 Ktor `testApplication`은 readiness failure가
  `503 Service Unavailable`을 반환함을 검증한다.
- Ktor `testApplication`은 exposed database error가 structured/sanitized 상태임을 검증한다.
- `README.md`와 `README.ko.md`는 helper-based approach와 older hand-owned Ktor example을
  비교한다.
- README diagram asset은 editable SVG와 rendered PNG로 다음 위치에 존재한다.
  `docs/images/readme-diagrams/`.
- Examples workflow는 `:05-ktor-exposed-integration:build`를 포함한다.
