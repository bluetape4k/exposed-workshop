# Issue #138 BigQuery dry-run workshop lesson

## 배경

Issue #138은 13장 아래 첫 runnable child module이다. Cloud credential이나 network call 없이
Exposed-generated query에서 `bluetape4k-exposed` BigQuery dry-run validation을 보여 준다.

## 결정

Default workshop path는 mock-only로 유지한다. Exposed read-model query를 만들고
`BigQueryContext.validateQuery`에 위임하는 작은 production helper를 구현하며, test는
`Bigquery.Jobs.query`를 통해 전달되는 실제 Google API `QueryRequest`를 MockK-capture한다.

## 결과

모듈은 generated SQL fragment, dry-run request mapping, success response, BigQuery error
conversion을 검증한다. README file은 dry-run과 execution의 차이를 설명하고 no-credential
default를 명시한다.

## 향후 지침

- Cloud-adjacent example에서는 plan에서 wrapper를 새로 꾸미지 말고 mock되는 실제 API boundary를
  이름으로 명시한다.
- Ordered call이나 request/response behavior를 보여 주는 README diagram은 완료 보고 전에
  `$bluetape4k-diagram`을 gate로 적용하고, generic flowchart보다 sequence diagram을 선호한다.
- Style parity를 받아들이기 전에 sequence diagram을 local best-practices sequence family와
  비교한다. 이 모듈에서는 `leader-redis-lettuce-sequence-02`가 frame/header/lifeline/activation,
  numbered pill label, semantic line color, fixed solid marker의 기준이었다.
- SVG/XML과 visual inspection만으로 diagram checklist success를 보고하지 않는다. PASS evidence를
  기록하기 전에 geometry audit, endpoint audit, marker/font check, CairoSVG CLI rendering,
  full-size PNG inspection을 실행한다.
- Child module을 도입하는 같은 PR에서 runnable Gradle task를 `.github/workflows/examples.yml`에
  추가한다.
- 새 모듈이 CI, Nightly, summary `needs`, coverage artifact를 요구하지 않으면 명시적인 N/A
  evidence를 기록한다.
