# Issue 99 R2DBC 동등성 감사

## 맥락

Issue [#99](https://github.com/bluetape4k/exposed-workshop/issues/99)는 `exposed-workshop`과
`exposed-r2dbc-workshop` 사이의 concept-level 동등성 감사를 요청한다. 목표는 module name을 정확히
맞추는 것이 아니다. 각 roadmap topic이 counterpart repo에서 다뤄졌는지, 의도적으로 platform-specific인지,
또는 follow-up issue가 필요한 실제 gap인지 판단하는 것이다.

R2DBC 쪽 counterpart는 [exposed-r2dbc-workshop#89](https://github.com/bluetape4k/exposed-r2dbc-workshop/issues/89)이며,
이 결정과 mirror되어 닫혔다.

## 2026-05-24 기준 현재 이슈 상태

| Repository | 열린 example roadmap issue | 관련 닫힌 roadmap issue |
|------------|-----------------------------|--------------------------|
| `exposed-workshop` | `#99`만 해당 | `#45`-`#63` |
| `exposed-r2dbc-workshop` | 없음 | `#32`-`#49`, `#69`, `#89` |

## Parity 결정

| `exposed-workshop` topic | R2DBC counterpart | 결정 |
|--------------------------|-------------------|------|
| Ktor examples epic `#45`와 multi-tenant `#46` | R2DBC `#32`, `#33`; `10-multi-tenant/07-multitenant-ktor` | Counterpart가 다룬다. |
| Ktor cache/routing 구현 이슈 `#47`, `#48`, `#49`와 wiring `#50`; JDBC modules `11-high-performance/05-07-*` | R2DBC `#34`, `#35`, `#36`, `#69`; R2DBC modules `11-high-performance/04-06-*` | Counterpart가 다룬다. `#50`은 docs wiring이다. |
| Spring Boot tenant strategy epic `#51`, 구현 `#52`-`#55`, wiring `#56`; JDBC modules `10-multi-tenant/04-06-*`, `08-tenant-onboarding-spring-web` | R2DBC `#37`-`#42`; R2DBC modules `10-multi-tenant/03-06-*` | Counterpart가 다룬다. `#56`은 docs wiring이다. |
| Chapter 12 production integration epic `#57`와 split modules `12-production-integration/01-10-*` | R2DBC `#43`-`#49`; consolidated `12-production-integration/01-spring-production-integration`, `02-ktor-production-integration` | Counterpart가 다룬다. |
| R2DBC connection-factory-per-tenant | R2DBC `#39`; JDBC에는 대신 database-per-tenant `#53`과 schema-per-tenant `#52`가 있다. | Platform-specific이며 중복 이슈는 없다. |
| JDBC DAO/entities examples | R2DBC는 SQL DSL-only basic examples를 가진다. | Platform-specific이다. DAO는 R2DBC teaching target이 아니다. |
| JDBC transaction template, Spring cache, benchmark | R2DBC는 suspend transaction, suspended cache, routing example을 사용한다. | Platform-specific이며 중복 이슈는 없다. |

## 결과

새 follow-up issue는 필요하지 않다. 겹치는 모든 roadmap item은 두 repository에서 모두 닫혔고,
남은 차이는 blocking JDBC와 R2DBC API model 차이에서 비롯된다.

Public parity table은 `README.md`와 `README.ko.md`에 mirror되어 있으므로, 향후 roadmap triage는
중복 구현 이슈를 다시 열지 않고 stable document에 연결하면 된다.
