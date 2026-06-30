# Issue #145 DDD Modulith Boundaries Lesson

## Context

The module demonstrates DDD bounded contexts with Spring Modulith verification
and Exposed persistence.

## Decision

Keep schema initialization inside each module package. A shared root initializer
that imports `orders.internal` or `shipping.internal` is itself a boundary
violation and `ApplicationModules.verify()` correctly rejects it.

## Outcome

The example uses `orders.events` as the single named interface and includes a
negative test fixture proving that direct `shipping -> orders.internal`
dependencies fail verification.

## Future Guidance

For Modulith examples, run `ApplicationModules.verify()` before documenting the
architecture. Treat failures as real design feedback, not only test failures.
