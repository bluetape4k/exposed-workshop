# Issue 141 - StarRocks Local-First OLAP Plan

## Lane

Type A new workshop module under `13-ecosystem-integrations`.

## Steps

1. Read issue #141, epic #137, prior chapter examples, and the current
   `bluetape4k-exposed` StarRocks helper source.
2. Add a RED test suite for typed profile validation, StarRocks DDL shape,
   query rendering, and local aggregation.
3. Implement the minimum module code to pass the tests.
4. Add README.md, README.ko.md, root/chapter links, and Examples workflow
   coverage.
5. Create a source-backed architecture diagram as SVG plus PNG and validate it
   with the current `$bluetape4k-diagram` checklist.
6. Run targeted module tests, build, project discovery, workflow lint, diagram
   checks, and `git diff --check`.
7. Run code review, commit with Lore trailers, open a PR, verify live PR body and
   metadata, then monitor CI.

## Validation Commands

- `./gradlew :04-starrocks-olap-local:test --no-daemon --no-configuration-cache`
- `./gradlew :04-starrocks-olap-local:build --no-daemon --no-configuration-cache`
- `./gradlew projects --quiet --no-daemon --no-configuration-cache`
- `actionlint .github/workflows/examples.yml`
- `xmllint --noout docs/images/readme-diagrams/04-starrocks-olap-local-architecture-01.svg`
- `python3 /Users/debop/.codex/skills/bluetape4k-diagram/references/diagram-geometry-audit.py docs/images/readme-diagrams/04-starrocks-olap-local-architecture-01.svg`
- `python3 /Users/debop/.codex/skills/bluetape4k-diagram/references/diagram-endpoint-audit.py docs/images/readme-diagrams/04-starrocks-olap-local-architecture-01.svg`
- `~/.local/bin/cairosvg docs/images/readme-diagrams/04-starrocks-olap-local-architecture-01.svg -o docs/images/readme-diagrams/04-starrocks-olap-local-architecture-01.png -s 2`
- `git diff --check`
