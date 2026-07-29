# Issue #142 lesson - 명시적 Ktor Exposed integration

## 배경

Issue #142에는 기존 12장 Ktor service example을 대체하지 않으면서 새로운
`bluetape4k-exposed-ktor` helper surface를 보여 주는 13장 예제가 필요했다.

## 결정

모듈은 작고 local-first로 유지한다.

- `call.exposedJdbcTransaction`을 통한 CRUD에는 H2 JDBC를 사용한다.
- H2 R2DBC는 readiness probe backend로만 사용한다.
- 하나의 `StatusPages` block에서 `bluetape4kErrorResponses()`와 `bluetape4kExposedErrors()`를
  compose한다.
- HikariCP, R2DBC pool, dispatcher lifecycle은 caller-owned로 유지한다.

## 결과

모듈은 helper boundary를 직접 보여 주고 12장은 production-service structure에 집중하게 둔다.
README diagram은 추가 rendered-PNG inspection pass가 필요했다. Resource-lane connector
corridor가 시각적으로 깨끗해지기 전에 SVG audit은 이미 통과했기 때문이다.

## 향후 지침

향후 Ktor/Exposed workshop 예제에서는 lesson이 readiness, sanitized error,
dispatcher-aware transaction wiring이면 helper를 사용한다. Lesson이 layered application
architecture이면 hand-owned transaction 예제는 12장에 유지한다.
