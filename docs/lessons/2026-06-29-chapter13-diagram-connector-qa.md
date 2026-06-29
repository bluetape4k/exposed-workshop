# Chapter 13 Diagram Connector QA

## Context

The Ktor Exposed integration README diagram passed SVG syntax and geometry
checks, but the rendered PNG still showed resource connectors crossing the
local-resource lane title and note text.

## Decision

Route connector-heavy diagrams through explicit side corridors and reserve
lane title/note bands as no-flow zones. Static audits are necessary but not
sufficient for README diagrams.

## Outcome

`05-ktor-exposed-integration-architecture-01.svg` now routes resource
connectors through the canvas margins and the blank band above resource cards,
then enters each card at a clear edge.

## Verification

- `xmllint --noout` for the six Chapter 13 README SVGs.
- `diagram-geometry-audit.py` for the six Chapter 13 README SVGs.
- `diagram-endpoint-audit.py` for the Chapter 13 architecture SVGs.
- `diagram-sequence-style-audit.py` for the sequence SVGs.
- Full-size rendered PNG inspection plus a six-diagram contact sheet.

## Future Work

For connector-heavy README diagrams, inspect the full-size PNG before reporting
the diagram checklist as passed. Contact sheets are useful for coverage, but
they do not replace targeted full-size inspection of dense connector areas.
