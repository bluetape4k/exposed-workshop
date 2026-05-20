# 2026-05-20 — Benchmark result charts

## Context

Workshop benchmark documents had measured tables and one ASCII comparison block,
but no direct chart images for latency and throughput interpretation.

## Decision

Add static SVG + PNG charts under `docs/images/readme-charts/` and link them from
benchmark report documents. Keep source tables and citations in place.

## Outcome

Charts were added for cache strategy latency, read-through cache hit/miss cost,
Exposed vs JPA CRUD latency, concurrent CRUD latency, and virtual-thread JDBC
throughput/load-test summaries.

## Verification

- `xmllint --noout docs/images/readme-charts/*.svg`
- `identify docs/images/readme-charts/*.png`
- Searched touched benchmark documents for remaining Mermaid/ASCII chart blocks.

## Future

For literature-review documents, chart only tables with explicit numeric values
and avoid turning qualitative claims into artificial numbers.
