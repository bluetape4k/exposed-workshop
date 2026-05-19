# README Diagram Image Validation

## Context

README diagram assets were regenerated with the reviewed pastel infographic
style.

## Decision

Keep PNG README embeds with matching SVG sources. Mermaid `init` and theme
configuration blocks must be ignored when rendering ERD images because they are
not database tables.

## Outcome

The workshop README diagram assets were regenerated without changing README
links. ERD images no longer show `init:` or `themeVariables` pseudo-tables.
Class diagrams use wider vertical spacing so inheritance arrows keep visible
stems.

## Verification

- Full regeneration: `rendered=294`, `missing=[]`.
- README image links: `missing=0`.
- Local SVG image embeds: `0`.
- Mermaid residue: `0`.
- Asset counts: `png=147`, `svg=147`.
- `init`/theme ERD residue: `0`.
- Whitespace check: `git diff --check`.

## Future Guidance

For ERDs, treat renderer config blocks as non-domain metadata. For small ERDs,
compact output is acceptable when text remains readable and no relation is
hidden.
