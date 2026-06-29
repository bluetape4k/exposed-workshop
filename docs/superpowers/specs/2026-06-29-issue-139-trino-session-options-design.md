# Issue #139 Trino Session Options Workshop Design

## Context

Issue #139 is the second child of epic #137 for chapter 13 ecosystem
integrations. The previous child, #138, added a credential-free BigQuery
dry-run example as `13-ecosystem-integrations/01-bigquery-dry-run`.

The next planned module is `13-ecosystem-integrations/02-trino-session-options`.
It should demonstrate the `bluetape4k-exposed` Trino option surface without
requiring a live Trino cluster in default CI.

## Current Evidence

- `13-ecosystem-integrations/README.md` already lists #139 as
  `02-trino-session-options` and planned Gradle task `:02-trino-session-options:build`.
- `bluetape4k-exposed` issue #229 delivered `TrinoConnectionOptions` and
  documented connector-specific pushdown verification.
- `TrinoConnectionOptions` supports `explicitPrepare`, `encoding`,
  `validateConnection`, `source`, `clientTags`, `sessionProperties`,
  `extraCredentials`, and `extraHeaders`.
- JetBrains Exposed 1.3.0 documents `Query.prepareSQL(prepared = false)` for
  generating a non-parameterized SQL string.
- Trino documentation treats pushdown as connector-specific; stable checks
  should use `EXPLAIN` signals instead of brittle full-plan snapshots.

## Design Decision

Build a local-only workshop module that models a Trino analytical connection
profile and a pushdown-friendly query plan request:

1. Use `TrinoConnectionOptions` as the public typed options API.
2. Keep an application-facing `TrinoWorkshopConnectionProfile` so example code
   can validate catalog, schema, source, tags, and session properties before
   constructing `TrinoConnectionOptions`.
3. Generate analytical SQL with Exposed against an H2 SQL-generation database.
4. Build an `EXPLAIN` request string around the generated SQL so tests can
   verify the predicate/top-N/projection shape without a live Trino cluster.
5. Document that actual pushdown support must be checked against the target
   Trino catalog or connector.

## Rejected Alternatives

- Live Trino Testcontainers as the default test path: rejected because #139
  requires default tests to avoid a live cluster unless the local container path
  is stable. The workshop can add an opt-in real-service lane later.
- Snapshot full `EXPLAIN` output: rejected because Trino plan text is
  connector- and version-sensitive. The module should assert stable request
  shape only.
- Build raw JDBC property strings in user code: rejected because the feature
  being taught is typed `TrinoConnectionOptions`; raw string fragments should
  stay behind a narrow preview for README/debugging.

## Acceptance Criteria

- `:02-trino-session-options:test` passes without a live Trino cluster.
- Tests verify typed option construction, unsafe value rejection, and expected
  SQL/`EXPLAIN` request shape.
- `README.md` and `README.ko.md` explain local validation versus real Trino
  connector validation.
- The chapter README pair marks #139 Ready and links the new module.
- Root README pair links the new child module.
- A rendered PNG sequence diagram and adjacent SVG source are added under
  `docs/images/readme-diagrams/`.
- `.github/workflows/examples.yml` includes `:02-trino-session-options:build`.
- Catalog dependency wiring uses existing BOM conventions and avoids ad hoc
  version pins.

## Risks

- The workshop could overclaim real pushdown. Mitigation: all prose must state
  that local tests verify request shape only.
- `TrinoConnectionOptions.toProperties` is internal to the library module.
  Mitigation: inspect public data class fields and provide a workshop preview
  map for educational assertions.
- Generated SQL may vary by dialect. Mitigation: assert stable clauses
  (`SELECT`, `FROM`, `WHERE`, `ORDER BY`, `LIMIT`) rather than exact SQL text.
