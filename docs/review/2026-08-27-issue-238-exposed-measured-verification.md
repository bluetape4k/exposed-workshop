# 이슈 #238 구현 검증 보고서

## 판정

`PASS` — 승인된 설계와 구현 계획의 범위를 충족한다. 새
`13-exposed-measured` 모듈은 provider의 기존 `DOUBLE` 기준 단위 계약을
재사용하고, 기존 스키마·모듈·R2DBC 저장소를 변경하지 않는다.

검증 기준 commit: `7c8f1251`

검증 worktree: `feat/issue-238-exposed-measured`
검증 시각: 2026-08-27 (KST)

## 요구사항 traceability

| 승인 요구사항 | 구현 근거 | 테스트/검증 근거 | 상태 |
| --- | --- | --- | --- |
| `06-advanced` 아래 JDBC 측정값 모듈 추가 | `06-advanced/13-exposed-measured/build.gradle.kts`, `gradle/libs.versions.toml` | `./gradlew projects`, `./gradlew assemble` | PASS |
| 길이·질량·절대 온도 DSL 컬럼 | `MeasuredData.kt`의 `ProductTable` | `DSL measured 컬럼은 기준 단위로 round-trip 된다` | PASS |
| DAO `Entity`/`EntityClass` 예제 | `MeasuredData.kt`의 `ProductEntity` | `DAO measured 컬럼은 nullable 값을 보존한다` | PASS |
| 입력 단위 변환과 기준 단위 조회 | `Ex01MeasuredColumns.kt` (`cm`, `m`, `km`, `g`, `kg`, `°F`, `°C`, `K`) | `:13-exposed-measured:test` 13 tests, failures/errors 0 | PASS |
| nullable 값 보존 | `nullableMass` 컬럼과 DAO 속성 | nullable DAO 파라미터 테스트 | PASS |
| Double 정밀도 경계 | 허용 오차를 둔 `shouldBeNear` 검증 | 정밀 측정값 파라미터 테스트 | PASS |
| 호환되지 않는 DB 값 거부와 계열 경계 | `MeasureColumnType.valueFromDB` 경계 테스트와 README의 `Measure<Mass>` → `Measure<Length>` 컴파일 오류 예 | `IllegalStateException` 기대 테스트와 source-equivalent 문서 확인 | PASS |
| EN/KO README와 SVG/PNG | 모듈 README 2개, architecture/ERD SVG·PNG·ledger | semantic, asset-pair, arrowhead, connector, endpoint, visual audit | PASS |
| CI/nightly 등록 | `.github/workflows/nightly.yml` PG/MySQL/MariaDB shard 항목 | YAML diff 및 Gradle build graph 확인 | PASS |
| 기존 스키마·R2DBC·정확도 정책 범위 유지 | 변경 파일에 기존 스키마 수정 없음; README 비목표 명시 | `git diff --name-only`, 승인 spec/plan 대조 | PASS |

## 계획 작업 reconciliation

| 계획 작업 | 결과 |
| --- | --- |
| 1. module/catalog 골격 | 완료 — `:13-exposed-measured`가 Gradle project로 검색됨 |
| 2. TDD RED | 완료 — fixture 추가 전 `ProductTable`/`ProductEntity` unresolved compiler failure 관찰 |
| 3. 최소 DSL/DAO fixture | 완료 |
| 4. JDBC round-trip/nullable/precision/failure tests | 완료 |
| 5. bilingual README와 diagrams | 완료 |
| 6. root/chapter README 등록 | 완료 |
| 7. nightly workflow 등록 | 완료 — 세 DB smoke/shard에 추가 |
| 8. static/build/docs verification | 완료 — 아래 fresh evidence 참조 |
| 9. PR/CI/review/lesson/merge | 다음 단계 — 현재 branch에서 진행 |

파일명은 detekt `MatchingDeclarationName` 규칙을 만족하도록 계획의
`Ex01_MeasuredColumns.kt`에서 `Ex01MeasuredColumns.kt`로 정리했다. 테스트 의미와
공개 경계의 변경은 없다.

## 위험 및 호환성 검증

| 위험 | 완화/관찰 결과 |
| --- | --- |
| 기준 단위 변경으로 기존 수치 의미가 달라질 수 있음 | 기존 스키마를 변경하지 않고 README와 ERD에 마이그레이션 경계를 명시 |
| `DOUBLE`의 부동소수 오차 | 절대 equality 대신 값별 허용 오차 사용 |
| 절대 온도와 온도 차이 혼동 | 이번 예제는 절대 `Temperature`만 노출하고 provider의 Kelvin 계약을 문서화 |
| DB별 `DOUBLE` read-back 타입 차이 | provider가 지원하는 숫자 타입을 사용하고 비지원 타입은 예외로 고정 |
| 실행 lane deadline 만료 | receipt diagnosis → recovery → checkpoint replacement lane ACK 절차로 복구; 구현 중 추가 변경 없음 |

## Fresh validation evidence

- `./gradlew :13-exposed-measured:test --no-daemon --no-configuration-cache` — BUILD SUCCESSFUL; JUnit XML `tests=13`, `failures=0`, `errors=0`, `skipped=0`.
- `./gradlew :13-exposed-measured:test -PuseFastDB=true --no-daemon --no-configuration-cache` — BUILD SUCCESSFUL; fast H2 JUnit XML `tests=5`, `failures=0`, `errors=0`, `skipped=0`.
- `./gradlew :13-exposed-measured:detekt --no-daemon --no-configuration-cache` — BUILD SUCCESSFUL.
- `./gradlew :13-exposed-measured:dependencyInsight --dependency bluetape4k-exposed-measured --configuration testRuntimeClasspath` — `io.github.bluetape4k.exposed:bluetape4k-exposed-measured:1.12.1` resolved.
- `./gradlew :13-exposed-measured:test :06-custom-columns:test :04-exposed-json:test -PuseFastDB=true` — BUILD SUCCESSFUL; 52 discovered tests, failures/errors 0. Existing JSON module reported 18 skipped cases.
- `./gradlew detekt --no-daemon --no-configuration-cache` — BUILD SUCCESSFUL.
- `./gradlew assemble -PuseFastDB=true --no-daemon --no-configuration-cache` — BUILD SUCCESSFUL.
- `diagram-semantic-audit.py` — architecture 2개와 erd 2개 모두 diagnostics 0.
- `diagram-arrowhead-audit.py`, `diagram-connector-audit.py`, `diagram-endpoint-audit.py` — 4 SVG 모두 PASS.
- `diagram-visual-audit.py` — 4 PNG 모두 PASS; 1400×760, aspect 1.84, margin imbalance 0.000.
- `diagram-asset-pair-audit.py` — repository pair audit PASS; missing PNG/SVG 0.
- `git diff --check` 및 한국어 용어 감사 — PASS.

## 남은 검증과 disposition

전체 `./gradlew test -PuseFastDB=true`는 120초 실행 한도 안에 완료 결론을
반환하지 못했다. 이 결과는 실패 판정으로 해석하지 않고, CI의 전체 test와 nightly
PostgreSQL/MySQL/MariaDB 실행을 A-10 live gate로 남긴다. 현재 로컬 변경에 대한
모듈·인접 모듈·detekt·assemble·문서/다이어그램 검증은 모두 완료되었다.

검증자 결론: `A-VER-01`부터 `A-VER-07`까지 현재 diff와 승인 artifact에 대해
PASS. PR 생성 전 최종 6관점 review에서 동일 traceability와 CI 결과를 다시 읽는다.
