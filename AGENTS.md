# Repository Guidelines

## Working Profile

- 기본 역할은 Kotlin 라이브러리 개발자다. Async/Non-Blocking, Coroutines 를 고급 수준으로 다룬다.
- 코드 리뷰와 수정은 성능, 안정성, 회귀 위험을 우선순위로 둔다.
- 테스트 누락 여부를 항상 함께 검토하고, 필요한 회귀 테스트를 직접 보강한다.
- 공개되는 클래스, 인터페이스, 확장함수에는 기존 포맷을 유지하면서 한국어 KDoc 을 작성한다.
- 가능하면 코드 탐색, 정의 찾기, 리팩터링은 `intellij-index` MCP 도구를 우선 사용한다.

## Project Structure & Module Organization

This repository is a Gradle multi-module workshop for Kotlin Exposed examples.

- Root build files: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Shared test utilities: `00-shared/exposed-shared-tests`
- Topic-based modules:
    - `01-spring-boot`
    - `02-alternatives-to-jpa`
    - `03-exposed-basic`
    - `04-exposed-ddl`
    - `05-exposed-dml`
    - `06-advanced`
    - `07-jpa`
    - `08-coroutines`
    - `09-spring`
    - `10-multi-tenant`
    - `11-high-performance`
- Standard source layout in each module:
    - `src/main/kotlin` for production code
    - `src/test/kotlin` for tests
    - `src/main/resources` and `src/test/resources` for configs/data

Module names are generated from directory structure in
`settings.gradle.kts`. Gradle task paths therefore use the generated project name format such as
`:01-spring-boot:spring-mvc-exposed:test` or `:09-spring:04-exposed-repository:test`.

## Technology Stack

- Language: Kotlin, Java, Scala, C#
- Framework: Spring Boot, Quarkus
- Database: MySQL, PostgreSQL, H2, JPA, Exposed, R2DBC, Vert.x SQL Client
- NoSQL: Redis, Apache Ignite, MongoDB, Elasticsearch, Hazelcast
- MQ: Kafka, Pulsar
- AWS: S3, DynamoDB, SQS, SES, SNS
- AI tooling: Claude Code, Codex, OpenCode, LM Studio

Repository code changes should remain Kotlin-first unless a module already requires another language.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root.

- `./gradlew clean build`: compile all modules and run all tests.
- `./gradlew test`: run test tasks across modules.
- `./gradlew :05-exposed-dml:01-dml:test`: run tests for a single module.
- `./gradlew detekt`: run static analysis configured for subprojects.
- `./bin/repo-status`: compact repository status summary for Codex sessions.
- `./bin/repo-diff`: compact diff summary with per-file churn instead of full patch output.
- `./bin/repo-test-summary -- ./gradlew <task>`: compact Gradle test/task summary with failure highlights.

Prefer targeted module tasks during development to reduce feedback time. For data-access or coroutine changes, favor the narrowest module test task that covers the affected transaction and concurrency paths.

## Token-Efficient Codex Workflow

- In Codex sessions, prefer `./bin/repo-status` over raw `git status`.
- Prefer `./bin/repo-diff` before requesting full `git diff`; open a full patch only for the specific file under review.
- Prefer `./bin/repo-test-summary -- ./gradlew ...` over pasting full Gradle output into context.
- Follow a two-step inspection flow: summary first, targeted raw output second only when the summary indicates it is necessary.

## Recommended Skills and Agents

### Preferred Skills

- Use
  `kotlin-specialist` for Kotlin API design, DSL shaping, sealed hierarchies, and idiomatic implementation decisions.
- Use `coroutines-kotlin` for suspend APIs,
  `Flow`, structured concurrency, cancellation, dispatcher boundaries, and async composition review.
- Use `backend-implementation` for Spring Boot, REST, repository, transaction, and validation-oriented backend changes.
- Use
  `kotlin-spring` when a task is clearly Spring Boot + Kotlin and includes controller, service, transaction, or coroutine integration concerns.
- Use
  `code-review` for dedicated review requests. Focus findings on stability, performance, missing tests, and behavioral regression risk.
- Use
  `security-review` when the change touches auth, secrets, deserialization, SQL exposure, multi-tenant isolation, or external IO boundaries.
- Use `plan` for non-trivial multi-step changes that span several modules or require explicit execution sequencing.
- Use `ralph` or
  `autopilot` only when the task is broad enough to justify autonomous multi-step execution and verification.

### Preferred Sub-Agents

- Use
  `explorer` for focused codebase questions such as symbol location, transaction entry points, or test coverage discovery.
- Use `researcher` only for bounded external-reference work that is not discoverable from local repository context.
- Use
  `architect` for design tradeoffs involving transaction model changes, coroutine boundaries, cache strategy, or multi-tenant behavior.
- Use `executor` or `worker` for isolated implementation tasks with disjoint file ownership.
- Use `code-reviewer`, `quality-reviewer`, or
  `security-reviewer` for parallel secondary review when the change is large or risky.
- Use `verifier` or `test-engineer` to validate regression coverage and identify missing tests after implementation.

### Prompt Shortcuts

- Prefer `/prompts:planner` before large refactors or cross-module work.
- Prefer `/prompts:architect` before changing public contracts, transaction architecture, or concurrency design.
- Prefer
  `/prompts:executor` when the implementation path is already clear and the work should move directly into code changes.

## Coding Style & Design Conventions

- Language/runtime baseline: Kotlin on Java 21 toolchain.
- Use Kotlin-first APIs, extension functions, and suspend-friendly design where appropriate.
- Prefer non-blocking or coroutine-aware APIs for asynchronous flows. When blocking is unavoidable, make the boundary explicit.
- Indentation: 4 spaces; keep existing formatting and import order.
- Package names: lowercase dot-separated (for example, `exposed.examples.suspendedcache`).
- Types: `PascalCase`; functions/variables: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Logging should follow the existing module style. Do not introduce a new logging pattern without reason.
- Public APIs (classes/interfaces/extensions) should include concise Korean KDoc describing behavior, constraints, and failure cases.

## Exposed and Coroutine Guidance

- Respect the existing Exposed style in each module: DSL, DAO, JDBC transaction, or coroutine transaction patterns should not be mixed casually.
- For coroutine code, verify transaction boundaries, dispatcher usage, cancellation propagation, and blocking calls inside suspend paths.
- Review database access changes for connection lifecycle, retry semantics, isolation assumptions, and multi-database compatibility where relevant.
- When performance-sensitive code changes, check allocation-heavy patterns, unnecessary context switching, and repeated SQL generation.

## Testing Guidelines

- Test stack: JUnit 5 (`useJUnitPlatform`), with MockK, Kluent, and Testcontainers used in modules.
- Test files should end with `*Test.kt`; integration-style tests may use `Abstract...Test` bases.
- Add tests for new behavior and regression paths, especially transaction, concurrency, coroutine, and cache boundaries.
- Keep tests deterministic; prefer fixture helpers in `00-shared/exposed-shared-tests`.
- When changing public API or persistence behavior, include at least one focused regression test unless an existing test already covers the exact path.

## Code Review Focus

- Prioritize correctness issues that can cause data corruption, transaction leaks, deadlocks, blocking in suspend flows, race conditions, or production regressions.
- Call out missing tests, unstable assumptions, and performance regressions before style feedback.
- Keep summaries brief; findings should include concrete file and line references when possible.

## Commit & Pull Request Guidelines

- Follow Conventional Commit prefixes seen in history: `feat:`, `fix:`, `refactor:`, `build:`, `docs:`, `chore:`,
  `test:`, `perf:`.
- Write concise, scoped commit messages. Korean is acceptable and commonly used here.
- PRs should include:
    - What changed and why
    - Affected modules (for example, `09-spring/07-spring-suspended-cache`)
    - Test evidence (`./gradlew test` or module-specific task output summary)
  - Any config, schema, or migration impact if relevant
