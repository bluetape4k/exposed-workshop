# README Mermaid SVG infographics

## Context

README files used live Mermaid diagrams. The documentation presentation needed
stable pastel SVG infographic assets while keeping sequence diagrams editable as
Mermaid source.

## Decision

Render every non-sequence README Mermaid block to SVG under
`docs/images/readme-diagrams/` and replace only those blocks with relative
image links.

## Outcome

Generated checked-in SVG assets for non-sequence diagrams. `sequenceDiagram`
blocks remain as Mermaid code blocks.

## Verification

Rendered SVG assets with Mermaid CLI 11.14.0, verified SVG link/file counts,
verified zero remaining non-sequence README Mermaid blocks, and ran
`git diff --check`.

## Future Guidance

Render first, finalize README links only after every SVG exists, and exclude
worktrees and build outputs from repository-wide documentation rewrites.
