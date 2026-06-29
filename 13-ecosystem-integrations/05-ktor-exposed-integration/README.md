# Explicit Ktor Exposed Integration

English | [한국어](README.ko.md)

This example shows how a Ktor application can use the explicit
`bluetape4k-exposed-ktor` integration instead of hand-owning every Exposed
transaction and readiness boundary. The older chapter 12 Ktor examples still
show production service structure; this module focuses on the helper surface
added for the Exposed 1.11 line.

![Explicit Ktor Exposed integration boundary](../../docs/images/readme-diagrams/05-ktor-exposed-integration-architecture-01.png)

The diagram separates three concerns:

- Ktor routes and tests that exercise the application through HTTP.
- `bluetape4k-exposed-ktor` helpers that install exposed health routes, map
  database failures, and run blocking JDBC work on a caller-supplied dispatcher.
- caller-owned JDBC and R2DBC resources that remain explicit and local-first.

## Purpose

Ktor applications often need the same database integration rules:

- a blocking dispatcher for JDBC transactions.
- readiness checks that prove both JDBC and R2DBC resources are usable.
- database error responses that do not leak SQL, JDBC URLs, or credentials.
- explicit application ownership for connection pools and lifecycle cleanup.

This module keeps those rules visible while delegating the repetitive Ktor glue
to `bluetape4k-exposed-ktor`.

## Helper-Based Ktor Setup

The application installs the base Ktor JSON support, composes `StatusPages`, and
then installs the Exposed-specific Ktor helper:

```kotlin
installBluetape4kKtorCore(
    Bluetape4kKtorCoreConfig(
        installStatusPages = false,
        installHealthRoutes = false,
    )
)
install(StatusPages) {
    bluetape4kErrorResponses()
    bluetape4kExposedErrors()
}
installBluetape4kExposedKtor(
    Bluetape4kExposedKtorConfig(
        jdbcDatabase = resources.jdbcDatabase,
        jdbcBlockingDispatcher = resources.jdbcDispatcher,
        r2dbcDatabase = resources.r2dbcDatabase,
        installHealthRoutes = true,
    )
)
```

The caller still creates and owns the HikariCP, H2 JDBC, R2DBC pool, and
dispatcher resources. The helper does not hide lifecycle ownership.

## CRUD Route

The note routes run JDBC work through `call.exposedJdbcTransaction`:

```kotlin
val note = call.exposedJdbcTransaction(
    db = resources.jdbcDatabase,
    blockingDispatcher = resources.jdbcDispatcher,
) {
    val id = WorkshopNotes.insertAndGetId {
        it[title] = request.title.trim()
        it[body] = request.body.trim()
    }
    WorkshopNotes.selectAll()
        .where { WorkshopNotes.id eq id }
        .single()
        .toNoteResponse()
}
```

That keeps the route concise while preserving the important boundary: blocking
JDBC work runs on the dispatcher chosen by the application.

## Health And Error Mapping

The helper contributes these routes:

| Route | Behavior |
|---|---|
| `/healthz/exposed` | Static Exposed liveness response. |
| `/readyz/exposed` | Probes configured JDBC and R2DBC resources with `SELECT 1`. |

The tests also call `/api/failures/sql`, which throws a `SQLException`. The
composed `StatusPages` block maps it to `EXPOSED_DATABASE_UNAVAILABLE` without
returning raw SQL, JDBC URLs, or passwords.

## Run

```bash
./gradlew :05-ktor-exposed-integration:test
```

Expected result: the tests use Ktor `testApplication`, in-memory H2 JDBC, and
in-memory H2 R2DBC. They do not start Docker and do not contact external
services.

## Tested Behavior

The tests verify that:

- `POST /api/notes` and `GET /api/notes` use helper-backed JDBC transactions.
- `/healthz/exposed` returns the helper liveness response.
- `/readyz/exposed` reports JDBC and R2DBC readiness when both resources are
  available.
- `/readyz/exposed` returns `503 Service Unavailable` when the caller-owned JDBC
  datasource is closed.
- exposed database errors are structured and sanitized.

## Relationship To Chapter 12

Chapter 12 Ktor modules remain the place to study full production-service
patterns such as layered routing, domain services, repositories, outbox flows,
auth sessions, and observability. This module is intentionally smaller: it
isolates the new Ktor Exposed helper so learners can decide when the helper is
better than hand-owned Ktor database wiring.
