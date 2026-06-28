# Issue #138 BigQuery Dry-Run Workshop Lesson

## Context

Issue #138 is the first runnable child module under chapter 13. It demonstrates
`bluetape4k-exposed` BigQuery dry-run validation from an Exposed-generated query
without using cloud credentials or network calls.

## Decision

Keep the default workshop path mock-only. Build a small production helper that
constructs the Exposed read-model query and delegates to
`BigQueryContext.validateQuery`, while tests MockK-capture the actual Google
API `QueryRequest` sent through `Bigquery.Jobs.query`.

## Outcome

The module verifies generated SQL fragments, dry-run request mapping, success
responses, and BigQuery error conversion. README files explain dry-run versus
execution and make the no-credential default explicit.

## Future Guidance

- For cloud-adjacent examples, name the real API boundary being mocked instead
  of inventing a wrapper in the plan.
- For README diagrams that show ordered calls or request/response behavior,
  apply `$bluetape4k-diagram` as a gate before claiming completion and prefer a
  sequence diagram over a generic flowchart.
- Compare sequence diagrams against the local best-practices sequence family
  before accepting style parity. For this module, `leader-redis-lettuce-sequence-02`
  was the reference for frame/header/lifeline/activation, numbered pill labels,
  semantic line colors, and fixed solid markers.
- Do not report diagram checklist success from SVG/XML plus visual inspection
  alone. Run geometry audit, endpoint audit, marker/font checks, CairoSVG CLI
  rendering, and full-size PNG inspection before recording PASS evidence.
- Add the runnable Gradle task to `.github/workflows/examples.yml` in the same
  PR that introduces a child module.
- Record explicit N/A evidence for CI, Nightly, summary `needs`, and coverage
  artifacts when a new module does not require those surfaces.
