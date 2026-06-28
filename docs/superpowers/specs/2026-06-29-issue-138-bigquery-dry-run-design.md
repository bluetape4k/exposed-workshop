# Issue #138 BigQuery Dry-Run Workshop Design

## Context

Issue #138 is the first runnable child example under chapter 13,
`13-ecosystem-integrations`. The parent epic #137 created the chapter boundary
for Exposed 1.11 ecosystem examples and reserved
`13-ecosystem-integrations/01-bigquery-dry-run` for this module.

`bluetape4k-exposed` issue #228 already added the product feature:
`BigQueryContext`, `BigQueryQueryOptions`, dry-run validation for raw/generated
SQL, and query-job options such as billed-byte limits, labels, priority,
location, destination table, timeout, and query cache flags. That repository
also has a compact `examples-exposed-bigquery-dry-run` example. This workshop
module should not simply copy that example. It should make the learning path
clearer for `exposed-workshop` readers by showing how an Exposed analytical
read model becomes generated SQL and then a credential-free BigQuery dry-run
request.

## Goals

- Add the first runnable chapter 13 module:
  `13-ecosystem-integrations/01-bigquery-dry-run`.
- Demonstrate generated SQL validation through
  `BigQueryContext.validateQuery`, not only raw SQL validation.
- Keep the default path credential-free, deterministic, network-free, and
  cost-free by using a mocked BigQuery REST client.
- Verify generated SQL, query-job option mapping, success handling, and failure
  handling in tests.
- Add English and Korean module README files with a clear distinction between
  dry-run validation and query execution.
- Add a README diagram as both editable SVG and rendered PNG.
- Promote issue #138 from `Planned` to `Ready` in the chapter README pair and
  add the module task to the weekly Examples workflow.

## Non-Goals

- Do not contact real Google Cloud BigQuery in default tests.
- Do not use Application Default Credentials, local credential files, service
  accounts, project secrets, or user-provided cloud endpoints by default.
- Do not add a BigQuery emulator or Testcontainers dependency for this example.
  The acceptance criteria are about dry-run request construction and error
  mapping, which can be tested deterministically with a mocked REST client.
- Do not implement #139-#145.
- Do not close parent epic #137.

## Current Evidence

- `settings.gradle.kts` already scans `13-ecosystem-integrations` with
  `includeModules("13-ecosystem-integrations", false, false)`, so adding a
  child directory with `build.gradle.kts` creates Gradle project
  `:01-bigquery-dry-run`.
- `.github/workflows/examples.yml` already has chapter 13 path filters but no
  chapter 13 Gradle task. The child module PR must add
  `:01-bigquery-dry-run:build` to the selected Examples build list.
- `.github/workflows/ci.yml` and `.github/workflows/nightly.yml` already run
  broad `test` or matrix coverage tasks and do not need child-module-specific
  path additions for this mock-only module. The implementation must still
  record explicit N/A evidence for CI, Nightly, summary `needs`, and coverage
  artifact changes during the registration audit.
- `gradle/libs.versions.toml` imports `bluetape4k-dependencies` 1.3.1 and
  already has aliases for common bluetape4k and Exposed modules, but it does
  not yet expose `bluetape4k-exposed-bigquery`.
- `bluetape4k-dependencies/gradle/libs.versions.toml` defines
  `bluetape4k-exposed-bigquery`, so this repo can add the same catalog alias
  without pinning an extra local version.
- `bluetape4k-exposed` has the source APIs:
  `BigQueryContext`, `BigQueryQueryOptions`, `BigQueryQueryPriority`,
  `BigQueryQueryException`, and `BigQueryResultRow`.
- The sibling example uses `MockK` to capture a `QueryRequest`, which is the
  right credential-free testing strategy for this workshop module.

## Design

Create a small module focused on one scenario: validating a dashboard read model
before a potentially billable warehouse query is executed.

The module will contain:

- `build.gradle.kts`
  - depends on `libs.exposed.bigquery`, JetBrains Exposed core/JDBC, H2,
    MockK, and `bluetape4k-junit5`
  - keeps the dependency version governed by the imported bluetape4k BOM
- `README.md` and `README.ko.md`
  - explain dry run vs execution
  - document the credential-free default command
  - describe what is verified by the tests
  - warn that real-service execution is intentionally out of this issue's scope
- one small production source package:
  `exposed.examples.bigquery.dryrun`
  - `BigQueryDryRunWorkshop.kt`
  - local `Events` table object for generated SQL
  - helper functions that build the read-model query, create default dry-run
    options, and call `BigQueryContext.validateQuery`
- one test source package:
  `exposed.examples.bigquery.dryrun`
  - `BigQueryDryRunWorkshopTest.kt`
  - MockK stubs for the real Google API service chain:
    `Bigquery`, `Bigquery.Jobs`, and `Bigquery.Jobs.Query`
  - capture of the real `QueryRequest` passed to `Jobs.query(projectId, request)`
  - tests for success, option mapping, generated SQL shape, and BigQuery error
    conversion
- test resources:
  `junit-platform.properties` and `logback-test.xml`
- diagram assets:
  `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg`
  `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.png`

The main test should generate a query like:

```kotlin
Events
    .select(Events.region, Events.eventId.count())
    .where { Events.revenue greaterEq BigDecimal("10.00") }
    .groupBy(Events.region)
    .orderBy(Events.region)
```

Then call:

```kotlin
context.validateQuery(
    query,
    BigQueryQueryOptions(
        maximumBytesBilled = 1_000_000L,
        labels = mapOf("workshop" to "bigquery-dry-run"),
        priority = BigQueryQueryPriority.BATCH,
        location = "US",
        timeoutMs = 5_000L,
    )
)
```

Assertions should verify:

- `QueryRequest.dryRun == true`
- `useLegacySql == false`
- `defaultDataset` contains the explicit project and dataset IDs
- generated SQL includes `SELECT`, `FROM EVENTS`, `GROUP BY`, and `ORDER BY`
- `maximumBytesBilled`, labels, priority, location, and timeout are mapped
- a successful dry run returns a complete response
- a BigQuery error response is surfaced as `BigQueryQueryException` with a
  clear message

## Approach Comparison

### A. Mocked REST dry-run with generated SQL

Recommended. It proves the Exposed-to-BigQuery boundary without credentials or
network access. It also keeps the module fast enough for weekly Examples CI.

### B. BigQuery emulator

Rejected for this issue. It is useful for broader execution semantics but adds
container lifecycle complexity and does not improve confidence in query-job
option mapping for this dry-run example.

### C. Real BigQuery opt-in test

Rejected for the default module. It would require credentials, project
configuration, network access, and cost guardrails. A future manual opt-in
example can be opened separately if needed.

For issue #138, do not add any executable opt-in path for real BigQuery. The
module must not read `GOOGLE_APPLICATION_CREDENTIALS`, Application Default
Credentials, service-account files, `GOOGLE_CLOUD_PROJECT`, endpoint overrides,
tokens, API keys, system properties, or environment variables, and must not
construct a real Google Cloud BigQuery client. Only mocked REST client wiring is
allowed.

## Risks And Mitigations

- Risk: generated SQL assertions become brittle across Exposed formatting
  changes. Mitigation: assert durable SQL fragments instead of one full
  whitespace-exact SQL string.
- Risk: README accidentally implies a real BigQuery query is executed.
  Mitigation: use explicit dry-run vs execution wording in both locales.
- Risk: credentials leak into tests through ADC or local environment. Mitigation:
  construct only a mocked BigQuery REST client in tests, keep placeholder
  project and dataset IDs as test constants only, and scan executable code for
  real-client or environment/property access.
- Risk: Exposed/H2 global state leaks across tests. Mitigation: create a
  deterministic H2 SQL-generation database in test setup, keep each validation
  inside `BigQueryContext.validateQuery` transaction boundaries, and avoid
  shared mutable mock state by recreating the mocked client per test.
- Risk: workflow turns green without building the new module. Mitigation: add
  `:01-bigquery-dry-run:build` to `.github/workflows/examples.yml` in the same
  PR that creates the runnable module.
- Risk: dependency drift from local catalog edits. Mitigation: add only the
  centrally governed `exposed-bigquery` alias and rely on the imported
  `bluetape4k-dependencies` BOM for the version.

## Acceptance Criteria

- `13-ecosystem-integrations/01-bigquery-dry-run/build.gradle.kts` exists and
  Gradle discovers project `:01-bigquery-dry-run`.
- Default tests run without Google Cloud credentials, ADC, network access, or
  billable BigQuery execution.
- No production, test, README command, or example path reads Google credential
  environment variables, system properties, service-account files, project
  secrets, endpoint overrides, tokens, API keys, or constructs a real BigQuery
  service/client.
- Tests construct `BigQueryContext` with deterministic placeholder constants for
  project ID, dataset ID, and location, then MockK-capture the actual
  `QueryRequest` sent through `Bigquery.Jobs.query`.
- Tests verify generated SQL fragments, dry-run flag, default dataset,
  billed-byte cap, labels, priority, location, timeout, success response, and
  failure response handling.
- New tests use JUnit 5, MockK, and `bluetape4k-assertions`.
- `README.md` and `README.ko.md` exist for the module and include a language
  switch.
- README files explain dry-run validation vs query execution and document the
  credential-free test command.
- Diagram SVG and PNG are committed under `docs/images/readme-diagrams/` and
  referenced from both README locales.
- Chapter README pair marks #138 as runnable and links the module README pair.
- Root README pair lists the first runnable chapter 13 module.
- `.github/workflows/examples.yml` runs `:01-bigquery-dry-run:build`.
- New-module registration audit records `settings.gradle.kts`,
  repo-local module lists, CI, Nightly, Examples workflow path filters/jobs,
  summary `needs`, coverage artifacts, and explicit N/A rationale where no
  change is required.
- `actionlint .github/workflows/examples.yml` passes.
- `./gradlew :01-bigquery-dry-run:test` and
  `./gradlew :01-bigquery-dry-run:build` pass.
- `./gradlew projects --quiet` includes `:01-bigquery-dry-run`.
- `git diff --check` passes.
- PR metadata is derived from live issue #138 metadata, closes #138, references
  parent #137, and verifies #137 remains open.
