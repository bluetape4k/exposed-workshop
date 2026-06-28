# Issue #137 Ecosystem Integrations Chapter Design

## Context

Issue #137 is the parent epic for `bluetape4k-exposed 1.11.0` and adjacent
DDD/Modulith workshop examples. Its child issues (#138-#145) cover database
platform integrations, explicit Ktor integration, Spring Modulith publication
storage, and DDD aggregate/module-boundary examples.

The current repository already has `12-production-integration` for
production-style Spring Boot 4 and Ktor service patterns. That chapter is about
HTTP entrypoints, service boundaries, outbox/auth/realtime/observability, and
small production-facing applications. Putting BigQuery, Trino, StarRocks,
CockroachDB, DDD, and Modulith examples into chapter 12 would make the chapter
mix runtime production concerns with ecosystem/dialect/framework integration
concerns.

The accepted chapter name is `13-ecosystem-integrations`.

## Goals

- Add a new chapter boundary for ecosystem-level Exposed examples.
- Keep issue #137 as the parent epic and leave #138-#145 as implementable child
  examples.
- Make the future module order explicit before the child issues are
  implemented.
- Wire the new chapter into root documentation and workflow path filters so
  future module work triggers the existing Examples gate when child PRs touch
  the chapter.
- Avoid pretending that the child example implementations exist before they are
  built.

## Non-Goals

- Do not implement the BigQuery, Trino, CockroachDB, StarRocks, Ktor,
  Modulith, or DDD child examples in this foundation PR.
- Do not close #137 until the child issue chain is complete or the user
  explicitly chooses to treat the scaffold as the epic closeout.
- Do not add ad hoc dependency versions. Future child modules must continue to
  use the existing catalog/BOM conventions.
- Do not move current chapter 12 modules.

## Current Evidence

- `settings.gradle.kts` includes chapter directories through
  `includeModules("<chapter>", false, false)`.
- `12-production-integration/README.md` defines chapter 12 as Spring/Ktor
  production service pattern coverage.
- `docs/lessons/2026-05-22-issue-63-chapter-12-wiring.md` records that chapter
  wiring must update root README pairs, chapter README pairs, workflow coverage,
  and diagram assets.
- `docs/lessons/2026-06-06-issue-118-examples-weekly.md` records that downstream
  examples should extend the weekly Examples workflow scope.
- GitHub issue #137 lists #138-#145 as child example issues under milestone
  `exposed-1.11.0`.

## Design

Create `13-ecosystem-integrations/` as a chapter-level directory with
`README.md` and `README.ko.md`. The chapter README describes the integration
map and lists the planned child modules as issue-backed placeholders, not as
implemented examples.

The root README pair should link only the chapter overview until child modules
exist. Planned child examples belong in the chapter README pair with explicit
`Planned` status and GitHub issue links.

Planned child module order:

| Issue | Status | Planned directory | Gradle project/task | README title | Lane |
|---|---|---|---|---|---|
| #138 | Planned | `13-ecosystem-integrations/01-bigquery-dry-run` | `:01-bigquery-dry-run:build` | BigQuery Dry-Run Query Validation | Database platform adapters |
| #139 | Planned | `13-ecosystem-integrations/02-trino-session-options` | `:02-trino-session-options:build` | Trino Session Options and Pushdown Verification | Database platform adapters |
| #140 | Planned | `13-ecosystem-integrations/03-cockroachdb-retry` | `:03-cockroachdb-retry:build` | CockroachDB Serializable Retry | Database platform adapters |
| #141 | Planned | `13-ecosystem-integrations/04-starrocks-olap-local` | `:04-starrocks-olap-local:build` | StarRocks Local-First OLAP | Database platform adapters |
| #142 | Planned | `13-ecosystem-integrations/05-ktor-exposed-integration` | `:05-ktor-exposed-integration:build` | Explicit Ktor Exposed Integration | Runtime and framework integration |
| #143 | Planned | `13-ecosystem-integrations/06-spring-modulith-publications` | `:06-spring-modulith-publications:build` | Spring Modulith Publication Store with Exposed | Runtime and framework integration |
| #144 | Planned | `13-ecosystem-integrations/07-ddd-aggregate-repository` | `:07-ddd-aggregate-repository:build` | DDD Aggregate Lifecycle with Exposed Repository | Domain architecture |
| #145 | Planned | `13-ecosystem-integrations/08-ddd-modulith-boundaries` | `:08-ddd-modulith-boundaries:build` | DDD Bounded Context and Modulith Boundary Verification | Domain architecture |

Register the chapter in:

- `settings.gradle.kts` with `includeModules("13-ecosystem-integrations", false, false)`.
  This adds a base-directory scan hook. It does not create a Gradle project
  until a child module directory with `build.gradle.kts` exists.
- Root `README.md` and `README.ko.md` with a new module-list section.
- Repo-local `AGENTS.md` layout table.
- `.github/workflows/examples.yml` path filters, so future chapter 13 module
  PRs trigger the selected Examples gate. This foundation PR should not add
  non-existent Gradle tasks to the hard-coded Examples build list. Each child
  module PR must add its own Gradle task to that selected build list when it
  creates a runnable module.

Add one chapter-level architecture diagram:

- `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`
- `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`

The diagram should show three lanes:

1. Database platform adapters: BigQuery, Trino, CockroachDB, StarRocks.
2. Runtime/framework integration: explicit Ktor and Spring Modulith.
3. Domain architecture: DDD aggregate lifecycle and Modulith boundary
   verification.

## External Service And Credential Policy

Future child modules must be safe by default:

- No checked-in credentials, tokens, service-account files, project IDs, or
  endpoint secrets.
- No automatic use of real cloud credentials such as ADC or local credential
  files in default tests.
- Default tests must use fake/local/Testcontainers/emulator-style paths or be
  deterministic documentation-only checks.
- Real external-service execution must be opt-in through explicit environment
  variables, Gradle properties, or test tags, and skipped by default in CI.
- Child module PRs must document whether their selected workflow coverage
  belongs in the weekly Examples gate, the full Nightly matrix, or an explicit
  opt-in/manual lane. The default path must remain local and deterministic.
- README files for cloud or external-service examples must include cost,
  network, and credential warnings before any real-service command.

## Rejected Approaches

### Extend `12-production-integration`

Rejected because chapter 12 already has a clear production-service purpose.
Adding cloud warehouse, OLAP, dialect, DDD, and Modulith examples there would
make the chapter too broad and would hide the new Exposed 1.11 ecosystem theme.

### Name the chapter `13-exposed-1-11`

Rejected because version-named chapters age quickly. The milestone and issue
metadata already preserve the release context. The chapter name should describe
the durable learning theme.

### Create empty child modules for #138-#145 now

Rejected because empty Gradle projects would imply runnable examples exist.
The child issues should create their modules with tests and README pairs when
each example is implemented.

## Risks And Mitigations

- Risk: documentation claims future examples are implemented. Mitigation: mark
  them as planned issue-backed examples and link to issue numbers.
- Risk: future chapter changes do not run Examples workflow. Mitigation: add
  `13-ecosystem-integrations/**` to Examples workflow path filters, and require
  child PRs to add hard-coded Gradle task entries only when their runnable
  modules exist.
- Risk: future cloud examples accidentally depend on real credentials or
  billable services. Mitigation: make local/fake execution the default and
  require real-service tests to be opt-in and skipped by default in CI.
- Risk: Gradle discovery is not proven. Mitigation: run `./gradlew projects`
  after adding `settings.gradle.kts` registration and record that the
  foundation PR creates no new Gradle project yet because no child module
  directory exists.
- Risk: diagram assets fail README quality expectations. Mitigation: render SVG
  to PNG with CairoSVG, validate the SVG as XML, inspect the PNG, and verify
  README references point to committed SVG/PNG siblings.

## Acceptance Criteria

- `13-ecosystem-integrations/README.md` and `README.ko.md` exist and explain the
  chapter purpose.
- Root `README.md` and `README.ko.md` list the new chapter.
- Root README entries link only the chapter overview until child modules exist.
- Chapter README pairs include the same planned issue table, language switch,
  and diagram reference in both locales.
- `AGENTS.md` lists the chapter in the repo layout table.
- `settings.gradle.kts` adds the chapter base-directory scan hook.
- `.github/workflows/examples.yml` includes chapter 13 path filters.
- `.github/workflows/examples.yml` does not reference missing chapter 13 Gradle
  tasks before child modules exist.
- Future child modules are documented as credential-free by default with
  opt-in real-service execution only.
- Chapter diagram SVG and PNG are committed and referenced from both README
  locales.
- SVG XML validation passes for the chapter diagram.
- README references to the chapter diagram resolve to committed files.
- Chapter diagram dimensions/viewBox are sane for README rendering and the PNG
  is visually inspected.
- `./gradlew projects` runs successfully; the expected foundation result is no
  new chapter 13 Gradle project until a child module exists.
- `git diff --check` and `actionlint .github/workflows/examples.yml` pass.
- README link/reference checks cover the new root and chapter links.
- The PR references #137 but does not close it unless child issues are also
  complete.
