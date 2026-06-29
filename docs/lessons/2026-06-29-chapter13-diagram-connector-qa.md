# Chapter 13 Diagram Connector QA

## Context

The Ktor Exposed integration README diagram passed SVG syntax and geometry
checks, but the rendered PNG still showed resource connectors crossing the
local-resource lane title/note text and later still looked like a large
connector frame around the whole resource lane.

## Decision

Do not fix connector-heavy diagrams by adding longer outer detours. First
reduce routing pressure: merge cards when the source-backed distinction can
live inside the same reader-facing card, reposition cards around the real
relationship, and keep lane title/note bands as no-flow zones. Static audits
are necessary but not sufficient for README diagrams.

## Outcome

`05-ktor-exposed-integration-architecture-01.svg` now combines the JDBC
datasource and dispatcher into one caller-owned resource card and keeps the
resource connectors short, distinct, and away from lane text.

## Verification

- `xmllint --noout` for the six Chapter 13 README SVGs.
- `diagram-geometry-audit.py` for the six Chapter 13 README SVGs.
- `diagram-endpoint-audit.py` for the Chapter 13 architecture SVGs.
- `diagram-sequence-style-audit.py` for the sequence SVGs.
- Marker color audit, segment-crossing audit, and diagonal-connector audit for
  the touched SVG.
- Full-size rendered PNG inspection plus a six-diagram contact sheet.

## Future Work

For connector-heavy README diagrams, inspect the full-size PNG before reporting
the diagram checklist as passed. Contact sheets are useful for coverage, but
they do not replace targeted full-size inspection of dense connector areas.
If a connector starts to look like a lane border or frame, redesign the card
grouping and ports before trying another path-only patch.
