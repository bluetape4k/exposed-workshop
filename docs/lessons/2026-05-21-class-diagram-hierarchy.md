# Class Diagram Hierarchy Audit

## Context

README class diagrams can become misleading when Mermaid-to-SVG conversion preserves a layout where inheritance or implementation arrows point downward to parent contracts.

## Decision

Keep interface, abstract, and base contract nodes above their implementors or subclasses when an inheritance or implementation edge exists. Re-route those edges as orthogonal paths so the open-triangle marker lands on the parent node.

## Outcome

Exposed workshop README class diagram assets were re-laid out top-down and PNGs were regenerated from the corrected SVG sources.

## Verification

- Scanned all workspace class SVGs for downward `inheritLine` and `implLine` endpoints: `COUNT 0`.
- Re-rendered changed PNG assets with `rsvg-convert`.
- Validated changed SVG files with `xmllint --noout`.

## Future Guidance

Before publishing README class diagrams, run an inheritance-direction scan against `docs/images/readme-diagrams/*class*.svg` and visually inspect at least one rendered PNG from each changed diagram family.
