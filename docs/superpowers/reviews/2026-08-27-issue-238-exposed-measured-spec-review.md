# 이슈 #238 Exposed measured 설계 리뷰

## 검토 범위와 근거

- 대상: `docs/superpowers/specs/2026-08-27-issue-238-exposed-measured-design.md`
- 이슈: [#238](https://github.com/bluetape4k/exposed-workshop/issues/238)의 live
  title, body, assignee, milestone, labels
- local 근거: `gradle/libs.versions.toml`, `settings.gradle.kts`,
  `06-advanced/05-exposed-money`의 build/test/README 패턴
- provider 근거: `bluetape4k-exposed/exposed/measured`의
  `MeasuredColumnTypes.kt`, `bluetape4k-projects/utils/measured`의 `Units.kt`와
  `Temperature.kt`, 중앙 catalog의 `bluetape4k-exposed-measured` alias
- 검토 방식: 사용자 요청의 single-developer 제약에 따라 여섯 렌즈를 한 세션에서
  순차적으로 적용했다. 독립 write subagent는 사용하지 않았으며, 각 렌즈의
  evidence와 필요한 수정 여부를 분리해 기록했다.

## 렌즈별 판정

| 우선순위 | 렌즈 | 근거 | 필요한 수정 | 재검토 |
|---|---|---|---|---|
| P2 | Performance | provider는 측정값을 한 번의 `DOUBLE` 값으로 저장하며 예제는 단일 상품 row의 JDBC transaction만 다룬다. 반복 조회, batch, benchmark, 운영 throughput 목표는 issue 범위에 없다. | benchmark나 새 cache를 추가하지 않고, module test와 `DOUBLE` 허용 오차 검증만 유지한다. | 수용 — hot path/무제한 작업 없음 |
| P2 | Stability | `MeasureColumnType`/온도 column type은 DB 숫자를 값 객체로 복원하고 지원되지 않는 값에서 즉시 실패한다. 테스트는 `withTables` 격리와 기존 `AbstractExposedTest` lifecycle을 사용해야 한다. | provider 구현을 복제하지 않고, 정상 JDBC dialect 왕복·nullable read-back과 module cleanup을 검증한다. | 수용 — plan test gate에 반영 |
| P2 | Security | 입력은 애플리케이션 측정값이며 새 외부 endpoint, credential, SQL 문자열 조합, 역직렬화 경계가 없다. 단위 문자열을 저장하지 않아 값과 표시 metadata의 혼동도 줄인다. | README 예제에 secrets/동적 SQL을 넣지 않고 provider DSL을 직접 사용한다. | 수용 — 추가 조치 없음 |
| P2 | Operator/Ops | 새 테이블은 테스트 트랜잭션에서 생성되는 격리 학습 schema다. 기준 단위를 바꾸면 기존 `DOUBLE` 데이터를 변환해야 하고, `DOUBLE`은 금융/법정 계량 정확도를 보장하지 않는다. | README 양쪽에 meter/kg/K 고정, migration 변환 주의, 정확도 정책 제외를 명시한다. | 수용 — 문서 수용 기준에 포함 |
| P2 | Developer/API | `measure`/`length`/`mass`/`temperature` DSL과 `IntIdTable`/`IntEntity`가 현재 v1 import 및 workshop 패턴에 맞는다. local catalog alias는 BOM 버전만 사용해야 한다. | module build 계획에 alias, `org.jetbrains.exposed.v1.*`, DAO/DSL 예제를 고정하고 direct version override를 금지한다. | 통과 — 설계에 반영됨 |
| P2 | User/caller | caller는 centimeters/grams/Celsius 같은 표시 단위로 입력하지만 DB는 meter/kg/K만 기억한다. 다른 `Measure<T>` 계열 대입은 generic boundary로 컴파일 단계에서 막힌다. | README에서 source-equivalent 순서로 표시 단위 변환, compile-time misuse 예, `TemperatureDelta` 구분을 설명한다. | 통과 — 설계에 반영됨 |

## 통합 판정

- P0: **0**
- P1: **0**
- P2: 위 위험은 provider source 계약, 기존 test lifecycle, 문서의 기준 단위
  고정으로 수용 가능하다. 새 운영 dependency, R2DBC 구현, 기존 schema 변경은 없다.
- 모순 점검: issue의 `precision/scale` 표현은 provider가 `DOUBLE`을 사용한다는
  source 사실과 충돌하지 않는다. 설계는 DECIMAL precision/scale을 새로 발명하지
  않고 소수·큰 값의 허용 오차와 유한 정밀도 한계를 검증하는 것으로 해석했다.
- 범위 점검: 길이·질량·절대온도의 최소 3계열, nullable, 단위 변환, 부적합 조합,
  DSL/DAO read-back, EN/KO README, SVG/PNG asset pair를 모두 수용 기준에
  연결했다. R2DBC는 `exposed-r2dbc-workshop` 별도 이슈로 명시했다.
- single-developer 점검: parallel subagent review는 `N/A (single-developer
  lane)`로 기록하며, 여섯 렌즈와 최종 integration은 leader가 순차 수행한다.
- 미해결 사용자 결정: 없음. 선택지 A와 `DOUBLE`/기준 단위 계약은 설계 승인으로
  결정되었다.

## SPW writer gate

- [x] **SPW-01** — 독자·목표·JDBC-only 경계와 issue/provider/catalog 근거를
  확인했다.
- [x] **SPW-02** — 책임 경계, 선택지, 데이터 흐름, API/테스트, 실패 모드,
  호환성·rollback, 문서·다이어그램, 수용 기준의 누락을 여섯 렌즈로 점검했다.
- [x] **SPW-03** — 한국어 work document 문체와 code/API/identifier/command/URL/
  version 보존을 확인했다.
- [x] **SPW-04** — local module/test/catalog와 provider measured source를
  대조했다.
- [x] **SPW-05** — `git diff --check`, terminology audit, Markdown 구조 점검을
  실행했고 P0/P1 차단 항목이 없음을 확인했다.

## 최종 상태

`PASS` — P0/P1 차단 항목이 없고, P2는 구현 계획·테스트·README 수용 기준에
반영되었다. 사용자가 설계 문서를 검토한 뒤 implementation plan으로 진행할 수
있다.
