# README Diagram Final QA

## Context

The README refresh regenerated source-derived diagrams for the root workshop and
all modules. The final pass rendered every SVG to PNG and reviewed grouped
contact sheets for connector placement, balanced margins, and text overflow.

## Decision

Diagram verification must include both render success and visual layout checks.
XML validity and successful PNG generation are necessary, but they do not catch
oversized canvases, uneven whitespace, or cards that become too small in README
rendering.

## Outcome

The final QA found four repeated JPA basic ERD assets whose content ended around
the first quarter of the canvas while the SVG viewBox kept an old height. The
fix trimmed the canvas/frame height and regenerated the paired PNG files.

## Verification

- Rendered all 175 SVG diagrams with CairoSVG.
- Checked SVG XML validity for all 175 SVG files.
- Verified README image references across 152 README files.
- Audited sequence `alt` regions for near-transparent fills.
- Inspected architecture, class, sequence, ERD, and miscellaneous contact
  sheets for connector, margin, and text fit issues.

## Next Time

For broad README diagram work, add an image dimension ratio sweep before the
visual pass. Flag unusually tall or wide diagrams, then inspect the rendered
PNG before committing.
