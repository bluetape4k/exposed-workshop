# Source-Verified README Diagrams

## Context

High-performance cache strategy class diagrams used the old `entityTable` property name.

## Decision

Update diagram members to the current `table` override used by JDBC and coroutine cache repositories.

## Verification

Confirm repository source declarations before rerendering README diagram PNGs.

## Future Guidance

Class diagrams should show current public/override members only. Do not preserve stale member names from historical refactoring plans.
