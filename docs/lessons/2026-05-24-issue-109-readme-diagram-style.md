# Issue 109 README Diagram Style

## Context

README diagram assets included many direct Mermaid-rendered SVG/PNG pairs, and
several newer diagrams missed the workspace typography contract.

## Decision

Regenerate every README diagram asset under `docs/assets/readme-diagrams/` and
`docs/images/readme-diagrams/` from recovered labels using deterministic
pastel SVG templates. Keep PNG links in README files and matching SVG sources
next to the PNG files.

## Outcome

Replaced Mermaid-rendered assets with custom architecture, class, sequence, and
ERD diagrams. Large labels use Architects Daughter, detail text uses a
Comic-style fallback stack, and connector routing stays in gaps between boxes.

## Verification

- README-linked diagram assets: `missing=0`.
- README local SVG diagram links: `0`.
- README-linked PNG diagrams without sibling SVG: `0`.
- README diagram SVGs with Mermaid renderer signatures: `0`.
- README diagram SVGs missing Architects Daughter: `0`.
- PNG render files missing or tiny: `0`.

## Future Guidance

Keep chart assets out of README diagram regeneration passes. Limit the target
set to `readme-diagrams/` unless a task explicitly asks to redraw charts.
