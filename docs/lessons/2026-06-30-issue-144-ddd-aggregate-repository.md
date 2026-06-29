# 2026-06-30 Issue #144 - DDD aggregate repository example

## Context

Issue #144 adds the first chapter 13 domain-architecture example. The module
needed to show a DDD aggregate without turning Exposed table classes into the
domain model.

## Decision

Use a small `PurchaseOrder` aggregate with value objects, command methods,
pending events, and an `OrderRepository` that maps state, owned lines, and event
rows inside one Exposed transaction.

## Outcome

The example stays local-first with H2. Tests cover aggregate invariants,
persistence, rollback after event insertion, and domain event capture order.

## Future Guard

For the next DDD or Modulith example, keep the aggregate/business boundary
visible in tests first, then add Exposed mapping and diagrams. Do not let table
classes become the aggregate API.
