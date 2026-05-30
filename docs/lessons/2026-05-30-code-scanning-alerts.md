# Code scanning alerts

## Context

GitHub CodeQL reported workflow token permission alerts for CI, Nightly, and
Examples, plus JavaScript alerts in README diagram tooling and checked-in
Gatling reports.

## Decision

Declare explicit workflow-level `contents: read` permissions first, then repair
the alerted JavaScript helpers and remove generated report artifacts when they
are not source documentation.

## Outcome

Workflow token defaults are now least-privilege for checkout-based jobs. Static
resource fixes stay scoped to the alerted files.

## Verification

- `actionlint .github/workflows/ci.yml .github/workflows/nightly.yml .github/workflows/examples.yml`
- `yq` inspection of workflow permissions
- `git diff --check`

## Future guard

Keep generated benchmark/report assets out of source control unless they are
actively maintained as documentation and pass CodeQL as static web content.
