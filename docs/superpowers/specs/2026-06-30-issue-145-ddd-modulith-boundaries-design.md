# Issue #145 DDD Modulith Boundary Design

## Context

Issue #145 adds a runnable workshop example for combining DDD bounded contexts,
Spring Modulith boundary verification, and Exposed persistence. The example
belongs to `13-ecosystem-integrations/08-ddd-modulith-boundaries`.

## Requirements

- Model at least two bounded contexts with separate packages, Exposed tables,
  repositories, and transaction scopes.
- Publish domain events from the order context instead of allowing direct
  repository access from another context.
- Mark the exported event package as a Spring Modulith named interface.
- Configure the consuming context with `allowedDependencies` so it can depend
  only on that named interface.
- Include a positive verifier test using `ApplicationModules.verify()`.
- Include a negative test fixture that proves a direct dependency on another
  context's internal repository fails boundary verification.
- Keep the example local-first with H2 and no external credentials.
- Provide bilingual README files and a generated diagram asset that passes the
  `bluetape4k-diagram` checklist and visual inspection.
- Register the module in Chapter 13 docs and the examples workflow.

## Design

The module has two Spring Modulith modules:

- `orders`: accepts an order, persists it with Exposed, and publishes
  `OrderAcceptedEvent`.
- `shipping`: listens to `OrderAcceptedEvent` and persists a shipment
  reservation with its own Exposed table and repository.

The `orders.events` package is the only exported named interface. The
`shipping` module metadata declares `allowedDependencies = ["orders :: events"]`.
The negative test fixture imports an `orders.internal` repository from
`shipping`, which must fail verification.

## Non-goals

- No distributed event broker or external database.
- No REST API; the tests and README are the workshop interface.
- No new production dependency beyond the Spring Modulith and Exposed
  dependencies already used by Chapter 13 Modulith examples.

## Verification

- Red test run before production code exists:
  `./gradlew :08-ddd-modulith-boundaries:test --no-daemon --no-configuration-cache --rerun-tasks`
- Targeted test and build:
  `./gradlew :08-ddd-modulith-boundaries:test --no-daemon --no-configuration-cache --rerun-tasks`
  `./gradlew :08-ddd-modulith-boundaries:build --no-daemon --no-configuration-cache --rerun-tasks --warning-mode all`
- Registration and docs:
  `./gradlew projects --no-daemon --no-configuration-cache`
  `actionlint .github/workflows/examples.yml`
  `git diff --check`
- Diagram:
  run the `bluetape4k-diagram` audits and inspect the generated PNG at full size.
