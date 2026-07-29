# Issue 141 - StarRocks local-first OLAP 계획

## Lane

`13-ecosystem-integrations` 아래 Type A new workshop module이다.

## 단계

1. Issue #141, epic #137, prior chapter example, current `bluetape4k-exposed` StarRocks helper
   source를 읽는다.
2. Typed profile validation, StarRocks DDL shape, query rendering, local aggregation을 위한 RED
   test suite를 추가한다.
3. Test를 통과시키는 최소 module code를 구현한다.
4. `README.md`, `README.ko.md`, root/chapter link, Examples workflow coverage를 추가한다.
5. Source-backed architecture diagram을 SVG와 PNG로 만들고 current `$bluetape4k-diagram`
   checklist로 검증한다.
6. Targeted module test, build, project discovery, workflow lint, diagram check,
   `git diff --check`를 실행한다.
7. Code review를 실행하고 Lore trailer로 commit하며 PR을 열고 live PR body/metadata를 검증한 뒤
   CI를 monitor한다.

## 검증 명령

- `./gradlew :04-starrocks-olap-local:test --no-daemon --no-configuration-cache`
- `./gradlew :04-starrocks-olap-local:build --no-daemon --no-configuration-cache`
- `./gradlew projects --quiet --no-daemon --no-configuration-cache`
- `actionlint .github/workflows/examples.yml`
- `xmllint --noout docs/images/readme-diagrams/13-starrocks-olap-local-architecture-01.svg`
- `python3 /Users/debop/.codex/skills/bluetape4k-diagram/references/diagram-geometry-audit.py docs/images/readme-diagrams/13-starrocks-olap-local-architecture-01.svg`
- `python3 /Users/debop/.codex/skills/bluetape4k-diagram/references/diagram-endpoint-audit.py docs/images/readme-diagrams/13-starrocks-olap-local-architecture-01.svg`
- `~/.local/bin/cairosvg docs/images/readme-diagrams/13-starrocks-olap-local-architecture-01.svg -o docs/images/readme-diagrams/13-starrocks-olap-local-architecture-01.png -s 2`
- `git diff --check`
