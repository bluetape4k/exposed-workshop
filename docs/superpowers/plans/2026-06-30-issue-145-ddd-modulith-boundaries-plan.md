# Issue #145 DDD Modulith Boundary Plan

## Step 1 - Scaffold Tests First

Action:
- Add `08-ddd-modulith-boundaries` Gradle module skeleton.
- Add Spring Boot integration tests for positive Modulith verification,
  negative boundary violation fixture, and event-driven persistence handoff.
- Run the module test command before adding production code and record the
  expected failure.

DoD:
- Tests compile or fail for missing production symbols only.
- Failure proves the new module is not implemented yet.

## Step 2 - Implement Bounded Contexts

Action:
- Implement `orders` and `shipping` bounded contexts.
- Keep Exposed tables and repositories internal to each context.
- Publish and consume `OrderAcceptedEvent` through the `orders.events` named
  interface.

DoD:
- Positive verifier test passes.
- Event handoff test proves `shipping` writes its own table without direct
  access to `orders.internal`.

## Step 3 - Document and Diagram

Action:
- Add `README.md`, `README.ko.md`, and a generated SVG/PNG diagram.
- Update Chapter 13 and root README links.

DoD:
- Both README files explain the DDD/Modulith/Exposed flow.
- Diagram passes automated audits and full-size visual inspection.

## Step 4 - Register CI Surface

Action:
- Add the module build task to `.github/workflows/examples.yml`.
- Verify module registration with Gradle project listing.

DoD:
- `:08-ddd-modulith-boundaries` appears in `./gradlew projects`.
- Workflow syntax validates with `actionlint`.

## Step 5 - Review and PR

Action:
- Run targeted tests/build, diff checks, verifier checklist, and review gates.
- Commit with Lore trailers and open a PR for issue #145.

DoD:
- PR mirrors issue assignee, milestone, and labels.
- PR body ends with `## DoD Status`.
- Live PR and issue metadata are verified with `gh`.
