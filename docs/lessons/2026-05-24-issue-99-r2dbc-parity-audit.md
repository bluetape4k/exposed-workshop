# Issue 99 R2DBC Parity Audit

## Context

`exposed-workshop` needed a final roadmap parity pass against `exposed-r2dbc-workshop` after the Ktor, chapter 10, and
chapter 12 example issues were completed.

## Decision

Track parity by concept, not by exact module name. Blocking JDBC modules and R2DBC modules keep separate architecture
choices when the underlying Exposed API model differs.

## Outcome

The audit found no portable gaps. `README.md`, `README.ko.md`, and the research note now record which issues and modules
cover each overlapping topic, and mark R2DBC connection-factory routing plus JDBC DAO/transaction-template/cache/benchmark
topics as platform-specific.

## Verification

- Confirmed `exposed-workshop` has only issue `#99` open.
- Confirmed `exposed-r2dbc-workshop` has no open issues.
- Checked closed roadmap issue sets: `exposed-workshop#45`-`#63`, `exposed-r2dbc-workshop#32`-`#49`, `#69`, and `#89`.

## Next time

When adding future example roadmap issues, first check the parity table and create counterpart issues only for portable
teaching concepts.
