# README Diagram Policy Refresh

## Context

The README diagram policy requires committed PNG embeds with editable SVG
sources. README files should not keep raw Mermaid blocks, ASCII tree diagrams,
or Mermaid renderer type names as user-facing diagram labels.

## Decision

Regenerate root README diagrams from current repository structure, add a
dedicated package-layout diagram for the Ktor chapter 12 architecture example,
and keep README links pointing to PNG files only.

## Outcome

The root feature map, API structure, overview, learning path, module structure,
and module composition chart now use refreshed SVG/PNG assets. The Ktor package
tree is represented as a diagram asset instead of a text tree, and Mermaid type
labels such as `classDiagram` and `flowchart` were removed from README headings
and alt text.

## Verification

- README scan confirmed no raw Mermaid blocks.
- README scan confirmed no ASCII tree diagram code blocks.
- README scan confirmed local image links resolve and do not point to SVG files.
- PNG dimensions were checked for all newly generated or regenerated assets.

## Next time

Use Mermaid or structured source only as an intermediate input. Commit the final
PNG and matching SVG source before linking a README.
