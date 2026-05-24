# Issue 111 Source-Derived README Diagrams

## Context

The first README diagram refresh still depended too much on existing SVG labels. That preserved stale Mermaid-like structure and allowed non-guide assets to survive.

## Decision

Regenerate README diagram assets from README context plus current Kotlin sources. Parent README diagrams must include child Gradle module sources when the parent directory has no direct source set. Localized READMEs must not drive image text; `README.md` is the preferred source for image titles and labels.

## Outcome

All 175 README diagram SVG/PNG pairs were regenerated with source-derived architecture panels, UML-like class sections with supertypes above concrete implementations, sequence interaction bands, and ERD table compartments with FK relationships.

## Verification

- `node scripts/regenerate-readme-diagrams.js`
- README diagram audit: `missing=0`, `svgRefs=0`, `fontMissing=0`, `rawMermaid=0`, `nonEnglish=0`
- `git diff --check`

## Future Guidance

Do not repair diagram assets by editing SVG text in place. Fix the source model or generator first, then regenerate SVG and PNG together.
