# Issue 63 Chapter 12 Diagram Images

## Context

Chapter 12 still had Mermaid diagram blocks in the Spring application
architecture and HTTP outbox/idempotency README pairs. The current README rule
requires example architecture diagrams to be committed as PNG images.

## Decision

Convert the remaining Mermaid blocks to committed SVG+PNG assets under
`docs/images/readme-diagrams/` and keep README embeds pointing to PNG files only.
The Mermaid source remains represented by the generated SVG source asset.

## Outcome

Updated the 02/03/04 English and Korean README files to use Architecture sections
with PNG diagram links. Added matching SVG+PNG assets for each converted
diagram.

## Verification

- Chapter 12 example README scan: `readmes=8`, `missingPng=0`,
  `missingArchitecture=0`, `mermaidResidue=0`, `missingFiles=0`.
- `git diff --check` passed.
- PNG dimensions verified with `file`/`sips` for the three new image assets.

## Future Notes

When adding chapter 12 examples, commit the diagram image assets before the
README link lands. Use Mermaid only as an intermediate source, not as the final
README rendering surface.
