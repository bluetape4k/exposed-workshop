# Issue 46 Ktor Multitenant Example

## Context

Issue #46 asked for a Ktor equivalent to the chapter 10 Spring multi-tenant examples.

## Decision

Use a Ktor plugin for tenant header validation and a coroutine `ThreadContextElement` for request-scoped tenant binding. Keep schema switching explicit inside the Exposed repository transaction.

## Outcome

Added `10-multi-tenant/07-multitenant-ktor` with H2-backed tenant schemas, focused route tests, English/Korean README files, and a reusable architecture diagram.

## Verification

Passed: `repo-test-summary -- ./gradlew :07-multitenant-ktor:test` with four passing tests.

## Future Guidance

For Ktor tenant examples, keep tenant resolution separate from repository schema switching so tests can prove missing header, invalid tenant, cleanup, and isolation behavior independently.
