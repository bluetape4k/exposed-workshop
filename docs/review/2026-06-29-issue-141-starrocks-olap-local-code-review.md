# Issue 141 - StarRocks local-first OLAP code review

## 범위

- 새 모듈 `13-ecosystem-integrations/04-starrocks-olap-local`
- StarRocks alias in `gradle/libs.versions.toml`
- README locale pair, root/chapter indexes, Examples workflow
- Diagram asset `13-starrocks-olap-local-architecture-01.svg/png`

## 발견 사항

- P0: 0
- P1: 0

## 리뷰 노트

- 구현은 public `bluetape4k-exposed-starrocks` API를 사용한다:
  `StarRocksConnectionOptions` and `StarRocksTable`.
- Default test는 H2-only이며 Testcontainers를 instantiate하거나 StarRocks backend에 접속하지
  않는다.
- DDL test는 의도적으로 rendered shape만 검증한다. Real StarRocks storage, partitioning,
  distribution, cloud behavior는 scope 밖이며 opt-in으로 문서화됐다.
- Diagram validation은 `$bluetape4k-diagram`을 따랐다: source-backed scope, editable SVG,
  CairoSVG-rendered PNG, XML parse, geometry audit, endpoint audit, marker/style scan,
  full-size PNG inspection.

## 검증 근거

- RED: `:04-starrocks-olap-local:test`는 unresolved workshop symbol에서 실패했다.
- GREEN: `:04-starrocks-olap-local:test`는 test 5개, failure 0, error 0, skipped 0으로
  통과했다.
- Build: `:04-starrocks-olap-local:test :04-starrocks-olap-local:build` 통과.
- Matrix guard: `:04-starrocks-olap-local:test -PuseDB=H2` 통과.
- Registration: `./gradlew projects --quiet`가 `:04-starrocks-olap-local`을 표시했다.
- Workflow/static: `actionlint .github/workflows/examples.yml`,
  `node /Users/debop/work/bluetape4k/.omx/scripts/audit-readme-diagrams.mjs .`,
  `git diff --check` 통과.
- Diagram: XML parse, geometry audit, endpoint audit, marker/style scan, PNG render,
  visual inspection 통과.
