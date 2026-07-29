# 2026-05-20 — Benchmark result chart

## 배경

Workshop benchmark 문서에는 측정 table과 ASCII 비교 block 하나가 있었지만, latency와
throughput 해석에 바로 쓸 수 있는 chart image가 없었다.

## 결정

`docs/images/readme-charts/` 아래 static SVG + PNG chart를 추가하고 benchmark report
문서에서 link한다. Source table과 citation은 그대로 유지한다.

## 결과

Cache strategy latency, read-through cache hit/miss cost, Exposed vs JPA CRUD
latency, concurrent CRUD latency, virtual-thread JDBC throughput/load-test
summary chart를 추가했다.

## 검증

- `xmllint --noout docs/images/readme-charts/*.svg`
- `identify docs/images/readme-charts/*.png`
- 수정한 benchmark 문서에 Mermaid/ASCII chart block이 남아 있는지 검색했다.

## 향후 지침

Literature-review 문서에서는 명시적인 numeric value가 있는 table만 chart로 만들고,
qualitative claim을 인위적인 숫자로 바꾸지 않는다.
