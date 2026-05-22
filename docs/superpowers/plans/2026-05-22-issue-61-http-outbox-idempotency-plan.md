# Issue 61 HTTP Outbox Idempotency Plan

## Retrieval

- qmd similar work lookup: complete.
- qmd caution lookup: complete.
- Current issue body: verified from GitHub issue #61.
- `$bluetape4k-patterns`: applied to all Kotlin tasks.

## Tasks

1. Create Spring Boot 4 module.
   - Add Gradle build.
   - Add Exposed persistence/repository.
   - Add `RestClient` outbound client.
   - Add service and MVC controller/advice.
   - Add repository, service, and MockMvc tests.

2. Create Ktor module.
   - Add Gradle build.
   - Add Exposed persistence/repository.
   - Add Ktor `HttpClient` outbound client.
   - Add service, plugins, and routes.
   - Add repository, service, and route tests.

3. Update docs and workflow.
   - Add module README files in English and Korean.
   - Update chapter README files.
   - Update root README files.
   - Add both modules to `.github/workflows/examples.yml`.

4. Verify.
   - `./gradlew projects --no-daemon`
   - `./gradlew :03-spring-http-outbox-idempotency:test --no-daemon`
   - `./gradlew :04-ktor-http-outbox-idempotency:test --no-daemon`
   - Examples workflow equivalent build.
   - `actionlint .github/workflows/examples.yml`
   - `git diff --check`

5. Delivery.
   - Record concise lesson.
   - Commit with Lore trailers.
   - Open PR assigned to `debop`.
   - Wait for Examples success.
   - Merge and sync local `develop`.
