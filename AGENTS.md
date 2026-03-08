# Repository Guidelines

## Project Structure & Module Organization

This repository is a Gradle multi-module workshop for Kotlin Exposed examples.

- Root build files: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Shared test utilities: `00-shared/exposed-shared-tests`
- Topic-based modules: `01-spring-boot` through `11-high-performance`
- Standard source layout in each module:
    - `src/main/kotlin` for production code
    - `src/test/kotlin` for tests
    - `src/main/resources` and `src/test/resources` for configs/data

Module names are generated from folder structure (see `settings.gradle.kts`), so keep directory names clear and stable.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root.

- `./gradlew clean build`: compile all modules and run all tests.
- `./gradlew test`: run test tasks across modules.
- `./gradlew :05-exposed-dml:01-dml:test`: run tests for a single module.
- `./gradlew detekt`: run static analysis configured for subprojects.
- `./bin/repo-status`: compact repository status summary for Codex sessions.
- `./bin/repo-diff`: compact diff summary with per-file churn instead of full patch output.
- `./bin/repo-test-summary -- ./gradlew <task>`: compact Gradle test/task summary with failure highlights.

Prefer targeted module tasks during development to reduce feedback time.

## Token-Efficient Codex Workflow

- In Codex sessions, prefer `./bin/repo-status` over raw `git status`.
- Prefer `./bin/repo-diff` before requesting full `git diff`; open a full patch only for the specific file under review.
- Prefer `./bin/repo-test-summary -- ./gradlew ...` over pasting full Gradle output into context.
- Follow a two-step inspection flow: summary first, targeted raw output second only when the summary indicates it is necessary.

## Coding Style & Naming Conventions

- Language/runtime baseline: Kotlin on Java 21 toolchain.
- Use Kotlin-first APIs and extension functions where appropriate.
- Indentation: 4 spaces; keep existing formatting and import order.
- Package names: lowercase dot-separated (for example, `exposed.examples.suspendedcache`).
- Types: `PascalCase`; functions/variables: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Public APIs (classes/interfaces/extensions) should include concise KDoc in Korean.

## Testing Guidelines

- Test stack: JUnit 5 (`useJUnitPlatform`), with MockK/Kluent/Testcontainers used in modules.
- Test files should end with `*Test.kt`; integration-style tests may use `Abstract...Test` bases.
- Add tests for new behavior and regression paths, especially transaction, concurrency, and coroutine boundaries.
- Keep tests deterministic; prefer fixture helpers in `00-shared/exposed-shared-tests`.

## Commit & Pull Request Guidelines

- Follow Conventional Commit prefixes seen in history: `feat:`, `fix:`, `refactor:`, `build:`, `doc:`, `chore:`,
  `test:`.
- Write concise, scoped commit messages (Korean is acceptable and commonly used here).
- PRs should include:
    - What changed and why
    - Affected modules (for example, `09-spring/07-spring-suspended-cache`)
    - Test evidence (`./gradlew test` or module-specific task output summary)
    - Any config/schema impact and migration notes if relevant
