# Issue #138 BigQuery Dry-Run Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the runnable `13-ecosystem-integrations/01-bigquery-dry-run`
workshop module for issue #138.

**Architecture:** The module is a credential-free test-driven workshop example.
It uses Exposed to build analytical SQL against an H2-backed SQL generation
database, then validates the generated SQL through `BigQueryContext.validateQuery`
against a mocked BigQuery REST client. Documentation and a flow diagram explain
the boundary between dry-run validation and billable query execution.

**Tech Stack:** Kotlin 2.3, Java 21, Gradle Kotlin DSL, JetBrains Exposed v1.x
from the catalog-backed dependency line,
`bluetape4k-exposed-bigquery`, H2, JUnit 5, MockK, `bluetape4k-assertions`,
CairoSVG, GitHub Actions.

---

## File Map

- Modify: `gradle/libs.versions.toml`
  - add `exposed-bigquery` alias using the centrally governed artifact.
- Create: `13-ecosystem-integrations/01-bigquery-dry-run/build.gradle.kts`
  - module dependencies and test classpath wiring.
- Create: `13-ecosystem-integrations/01-bigquery-dry-run/src/main/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshop.kt`
  - public workshop helper API for building the read-model query, default
    dry-run options, and validation call.
- Create: `13-ecosystem-integrations/01-bigquery-dry-run/src/test/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshopTest.kt`
  - generated SQL, dry-run options, success, and failure tests.
- Create: `13-ecosystem-integrations/01-bigquery-dry-run/src/test/resources/junit-platform.properties`
- Create: `13-ecosystem-integrations/01-bigquery-dry-run/src/test/resources/logback-test.xml`
- Create: `13-ecosystem-integrations/01-bigquery-dry-run/README.md`
- Create: `13-ecosystem-integrations/01-bigquery-dry-run/README.ko.md`
- Create: `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg`
- Create: `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.png`
- Modify: `13-ecosystem-integrations/README.md`
- Modify: `13-ecosystem-integrations/README.ko.md`
- Modify: `README.md`
- Modify: `README.ko.md`
- Modify: `.github/workflows/examples.yml`
- Create: `docs/review/2026-06-29-issue-138-bigquery-dry-run-code-review.md`
- Create: `docs/lessons/2026-06-29-issue-138-bigquery-dry-run.md`

## Task 1: Catalog And Module Skeleton

complexity: low

Apply `$bluetape4k-code-patterns` for module registration and dependency
governance.

- [ ] Add `exposed-bigquery = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-bigquery" }` next to the other `exposed-*` aliases in `gradle/libs.versions.toml`.
- [ ] Create `13-ecosystem-integrations/01-bigquery-dry-run/build.gradle.kts`:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(libs.exposed.bigquery)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.mockk)
}
```

- [ ] Create `src/test/resources/junit-platform.properties` with parallel
  execution disabled for deterministic request capture.
- [ ] Create `src/test/resources/logback-test.xml` using the repository's
  existing compact test logging pattern.
- [ ] Run:

```bash
./gradlew projects --quiet
```

Expected: exit 0 and output contains `:01-bigquery-dry-run`.

## Task 2: RED Tests For Dry-Run Query Validation

complexity: medium

Apply `$bluetape4k-code-patterns`, `ecc-kotlin-exposed`, and
`ecc-kotlin-testing`. Follow TDD: write the tests before adding
`BigQueryDryRunWorkshop.kt`.

- [ ] Create
  `13-ecosystem-integrations/01-bigquery-dry-run/src/test/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshopTest.kt`.
- [ ] Write tests that expect production API symbols from
  `BigQueryDryRunWorkshop.kt`: `Events`, `buildRegionalRevenueQuery`,
  `defaultDryRunOptions`, and `validateRegionalRevenueDryRun`.
- [ ] Add MockK stubs for the actual Google API service chain:
  `Bigquery`, `Bigquery.Jobs`, and `Bigquery.Jobs.Query`.
- [ ] Capture the real `QueryRequest` argument passed to
  `Jobs.query(projectId, request)` and return a configured `QueryResponse` from
  `Jobs.Query.execute()`.
- [ ] Use named test constants for placeholder project ID, dataset ID, and
  location, then construct `BigQueryContext` with those constants.
- [ ] Add test `generated query dry run maps query job options without credentials`:
  - builds an Exposed grouped query
  - calls `BigQueryContext.validateQuery`
  - asserts captured request fields with `bluetape4k-assertions`
- [ ] Add test `dry run surfaces BigQuery validation errors without execution`:
  - mocked response includes an error
  - `validateQuery` throws `BigQueryQueryException`
  - assertion uses `io.bluetape4k.assertions.assertFailsWith`
- [ ] Run:

```bash
./gradlew :01-bigquery-dry-run:test --tests 'exposed.examples.bigquery.dryrun.BigQueryDryRunWorkshopTest' --no-daemon
```

Expected RED: tests fail because `BigQueryDryRunWorkshop.kt` has not been
implemented yet, or because actual BigQuery API signatures require import/API
adjustments. The failure must be about missing implementation or API wiring, not
syntax typos. If tests pass immediately because existing APIs fully cover the
test without production helper gaps, record that as TDD evidence instead of
forcing an artificial failure.

## Task 3: GREEN Implementation By Wiring Existing BigQuery APIs

complexity: medium

Apply `$bluetape4k-code-patterns` and `ecc-kotlin-exposed`.

- [ ] Create
  `13-ecosystem-integrations/01-bigquery-dry-run/src/main/kotlin/exposed/examples/bigquery/dryrun/BigQueryDryRunWorkshop.kt`.
- [ ] Implement public KDoc-backed workshop helpers:
  - `Events` table with `eventId`, `region`, `eventType`, `revenue`, and
    `occurredAt` columns
  - `buildRegionalRevenueQuery`
  - `defaultDryRunOptions`
  - `validateRegionalRevenueDryRun`
- [ ] Fix imports and helper code so tests compile against the actual
  `BigQueryContext`, `BigQueryQueryOptions`, `BigQueryQueryPriority`, and
  `BigQueryQueryException` APIs.
- [ ] Create the H2 SQL-generation database deterministically in test setup.
  Use `Database.connect("jdbc:h2:mem:bigquery_dry_run;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", driver = "org.h2.Driver")`
  or the matching `BigQueryContext` factory when available. Recreate the mocked
  BigQuery REST client per test, keep mutable request capture per test, and rely
  on `BigQueryContext.validateQuery` for transaction boundaries so Exposed state
  does not leak across tests.
- [ ] Do not add any path that reads `System.getenv`, `System.getProperty`,
  ADC, service-account files, project secrets, endpoint overrides, tokens, API
  keys, or constructs a real Google Cloud BigQuery client.
- [ ] Avoid deprecated Exposed imports such as `SqlExpressionBuilder.eq`.
- [ ] Use durable SQL fragment assertions instead of exact full-SQL matching.
- [ ] Run:

```bash
./gradlew :01-bigquery-dry-run:test --no-daemon
```

Expected GREEN: all tests in `:01-bigquery-dry-run` pass.

## Task 4: Module README Pair And Diagram

complexity: medium

Apply `$bluetape4k-diagram` and README locale policy.

- [ ] Create `README.md` and `README.ko.md` under the module.
- [ ] Include language switches:
  - English file: `English | [한국어](README.ko.md)`
  - Korean file: `[English](README.md) | 한국어`
- [ ] Keep README locale parity. Both files must contain matching sections for:
  purpose, dry-run vs execution, credential-free command, no-cloud-credential
  guarantee, tested behavior, diagram reference, and real BigQuery out-of-scope
  warning.
- [ ] Explain:
  - dry run parses and validates a query without executing a billable query
  - default test path uses a mocked BigQuery REST client
  - no credentials, ADC, project secrets, or network calls are required
  - command: `./gradlew :01-bigquery-dry-run:test`
  - expected result: the command uses only H2 plus mocked BigQuery REST calls
    and passes without `GOOGLE_APPLICATION_CREDENTIALS`
- [ ] Create `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg` with
  English labels and source-backed flow: Exposed query -> SQL generation DB ->
  BigQuery dry-run request -> mocked REST response -> workshop assertions.
- [ ] Embed the diagram in both README files with alt text or a caption that
  includes `mocked BigQuery REST response`.
- [ ] Render PNG:

```bash
~/.local/bin/cairosvg docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg \
  -o docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.png -s 2
```

- [ ] Validate:

```bash
xmllint --noout docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg
```

- [ ] Inspect rendered PNG at full size before accepting the diagram.

## Task 5: Chapter, Root README, And Workflow Wiring

complexity: low

Apply `$bluetape4k-code-patterns` for module registration and workflow rules.

- [ ] Update `13-ecosystem-integrations/README.md` and `README.ko.md`:
  - change #138 status from `Planned` to `Ready`
  - link `01-bigquery-dry-run/README.md` and
    `01-bigquery-dry-run/README.ko.md`
  - keep #139-#145 as planned
- [ ] Update root `README.md` and `README.ko.md` under Chapter 13 to include the
  first runnable child module link.
- [ ] Add `:01-bigquery-dry-run:build` to the selected Examples workflow Gradle
  invocation, near the chapter 13 comment.
- [ ] Complete the new-module registration audit and record the result in the
  review document:
  - `settings.gradle.kts`: automatic chapter include covers the module
  - repo-local module list: root/chapter README pairs updated
  - CI workflow: no child-module path/job change required because broad Gradle
    test jobs cover discovered projects
  - Nightly workflow: N/A for this issue because the module is mock-only and the
    Examples workflow owns runnable example coverage
  - Examples workflow: path filters already include chapter 13 and selected
    build task is added
  - summary `needs`: unchanged unless the Examples job graph changes
  - coverage artifacts: unchanged; no Kover threshold or Codecov gate is added
- [ ] Run:

```bash
actionlint .github/workflows/examples.yml
```

Expected: exit 0.

## Task 6: Verification And Review Evidence

complexity: medium

Apply `verification-before-completion`, `$bluetape4k-code-patterns`, and
`$bluetape4k-diagram`.

- [ ] Run module tests:

```bash
./gradlew :01-bigquery-dry-run:test --no-daemon
```

- [ ] Run module build:

```bash
./gradlew :01-bigquery-dry-run:build --no-daemon
```

- [ ] Run project discovery:

```bash
./gradlew projects --quiet
```

- [ ] Run workflow lint:

```bash
actionlint .github/workflows/examples.yml
```

- [ ] Run diff whitespace check:

```bash
git diff --check
```

- [ ] Run README local-link check for touched README files:

```bash
ruby -e 'files=%w[README.md README.ko.md 13-ecosystem-integrations/README.md 13-ecosystem-integrations/README.ko.md 13-ecosystem-integrations/01-bigquery-dry-run/README.md 13-ecosystem-integrations/01-bigquery-dry-run/README.ko.md]; bad=[]; files.each{|f| text=File.read(f); text.scan(/!?\[[^\]]*\]\(([^)#][^)]*)\)/).flatten.each{|href| next if href =~ %r{\Ahttps?://}; path=href.split("#",2).first; next if path.empty?; target=File.expand_path(path, File.dirname(f)); bad << "#{f} -> #{href}" unless File.exist?(target)}}; abort(bad.join("\n")) unless bad.empty?'
```
- [ ] Run credential drift scan for touched module/docs:

```bash
rg -in "GOOGLE_APPLICATION_CREDENTIALS|application-default|Application Default Credentials|\\bADC\\b|service[-_ ]account|client_secret|password|token|api[_-]?key|project[-_ ]?ids?|projectId|endpoint secret" \
  13-ecosystem-integrations README.md README.ko.md docs/images/readme-diagrams .github/workflows/examples.yml
```

Expected: only explanatory policy/warning text and placeholder dataset/project
IDs, no real secrets.

- [ ] Run executable-path real-client scan:

```bash
rg -n "System\\.getenv|System\\.getProperty|GoogleCredentials|BigQueryOptions|ServiceAccount|GOOGLE_CLOUD_PROJECT|GOOGLE_APPLICATION_CREDENTIALS|setApplicationName|new Bigquery|Bigquery\\.Builder" \
  13-ecosystem-integrations/01-bigquery-dry-run/src .github/workflows/examples.yml
```

Expected: zero matches, except mocked `Bigquery` type usage in the test helper
when the match is not real-client construction.

- [ ] Write `docs/review/2026-06-29-issue-138-bigquery-dry-run-code-review.md`
  with severity counts and findings derived from actual review evidence. Do not
  predeclare P0/P1 results before the review is complete.
- [ ] Record rendered PNG visual QA evidence in the review document after
  inspecting the generated image.

## Task 7: Lessons, Commit, PR, And CI

complexity: low

- [ ] Create `docs/lessons/2026-06-29-issue-138-bigquery-dry-run.md` with the
  module, credential-free default, and workflow task wiring lesson.
- [ ] Commit spec and plan before implementation if not already committed.
- [ ] Commit implementation/docs/review/lesson with Lore commit trailers.
- [ ] Push branch `feat/issue-138-bigquery-dry-run`.
- [ ] Read live issue metadata before creating the PR:

```bash
gh issue view 138 --json assignees,labels,milestone,state
```

- [ ] Create PR:
  - title: `feat: add BigQuery dry-run workshop example`
  - body closes #138 and references #137
  - assignee, milestone, and labels mirrored from live issue #138 metadata
  - final section exactly `## DoD Status`
- [ ] Verify live PR metadata:

```bash
gh pr view <number> --json body,assignees,labels,milestone,state,isDraft
```

- [ ] Verify parent epic #137 remains open:

```bash
gh issue view 137 --json state
```

- [ ] Watch/check CI with `gh pr checks <number>` and proceed to final DoD only
  after checks are success or an evidence-backed blocker is reported.

## Step 3-R Self Review

- Spec coverage: every #138 acceptance criterion maps to Tasks 1-7.
- Placeholder scan: no `TBD`, `TODO`, or open-ended implementation steps remain.
- Type consistency: module name, package name, test class, diagram filenames,
  and Gradle task names are consistent across tasks.
- Concurrency helpers: not applicable; this module has no race, structured
  concurrency, virtual-thread, or suspend stress behavior.
- Testcontainers: not applicable; default path is mock REST client plus H2 SQL
  generation DB.
