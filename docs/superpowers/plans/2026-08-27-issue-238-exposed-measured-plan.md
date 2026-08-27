# 이슈 #238 Exposed measured 예제 구현 계획

> **For agentic workers:** 이 계획은 구현과 검증을 같은 세션에서 수행한다. 각
> 단계는 checkbox로 추적하며, 의존 단계의 증거가 확보되기 전에는 다음 단계로
> 이동하지 않는다.

**목표:** `06-advanced/13-exposed-measured`에 `bluetape4k-exposed-measured`의
JDBC 컬럼 DSL을 사용하는 상품 길이·질량·절대온도 예제를 추가한다. 기준 단위
저장, nullable 왕복, `DOUBLE` 근사 정밀도, DSL/DAO 조회, 타입 안전 경계를
테스트와 한영 문서/다이어그램으로 검증한다.

**아키텍처:** provider가 `MeasureColumnType`과 온도 column type의 `DOUBLE`
변환/복원을 소유하고, workshop 테스트 코드가 `ProductTable : IntIdTable`과
`ProductEntity : IntEntity`를 통해 JDBC `transaction {}` 경계를 보여 준다. 길이,
질량, 절대온도는 각각 meter, kilogram, Kelvin을 안정적인 저장 기준으로 사용하며
표시 단위 metadata는 저장하지 않는다. R2DBC는 `exposed-r2dbc-workshop`의 별도
작업으로 제외한다.

**기술 스택:** Kotlin 2.3, Java 21/25 CI, Exposed v1, bluetape4k BOM 1.4.0,
`bluetape4k-exposed-measured` 1.12.1 provider, Gradle version catalog, JUnit 5,
`AbstractExposedTest`/`TestDB`, H2 fast profile와 JDBC Testcontainers dialect.

## 변경 경로와 소유 범위

| 영역 | 경로 | 목적 |
|---|---|---|
| catalog | `gradle/libs.versions.toml` | BOM 기반 `exposed-measured` consumer alias 추가 |
| 모듈 빌드 | `06-advanced/13-exposed-measured/build.gradle.kts` | Exposed JDBC, measured provider, shared test와 JDBC dialect 의존성 등록 |
| 예제 모델 | `06-advanced/13-exposed-measured/src/test/kotlin/exposed/examples/measured/MeasuredData.kt` | `ProductTable`, `ProductEntity`, 기준 단위 컬럼 선언 |
| 회귀 테스트 | `06-advanced/13-exposed-measured/src/test/kotlin/exposed/examples/measured/Ex01MeasuredColumns.kt` | DSL/DAO round-trip, nullable, 정밀도; provider 부적합 DB 값 계약은 문서화 |
| 테스트 설정 | `06-advanced/13-exposed-measured/src/test/resources/junit-platform.properties` | 기존 shared test의 직렬 실행/생명주기 설정 재사용 |
| 테스트 로그 | `06-advanced/13-exposed-measured/src/test/resources/logback-test.xml` | 모듈 package와 Exposed 로그 설정 |
| 모듈 문서 | `06-advanced/13-exposed-measured/README.md`, `README.ko.md` | source-equivalent 사용법, 기준 단위/정밀도/migration 설명 |
| chapter index | `06-advanced/README.md`, `README.ko.md` | 모듈 목록·학습 순서·실행 명령 등록 |
| repository index | `README.md`, `README.ko.md` | 고급 기능 목록에 새 모듈 링크 등록 |
| CI/nightly | `.github/workflows/nightly.yml` | PostgreSQL/MySQL/MariaDB shard에 모듈 test task 등록; H2 전체 test는 기존 경로로 포함 |
| 다이어그램 | `docs/images/readme-diagrams/06-advanced-13-exposed-measured-*` | 한영 architecture/ERD SVG·PNG와 semantic ledger |
| 리뷰/교훈 | `docs/superpowers/reviews/*`, `docs/review/*`, `docs/lessons/*` | plan/final/performance 검토와 재사용 가능한 결정 보존 |

기존 모듈의 source, persistent schema, custom column 구현, `exposed-r2dbc-workshop`
저장소, Redis/통화/정확도 정책은 변경하지 않는다. `.github/workflows/examples.yml`
의 fixed weekly 목록은 현재 `06-advanced`를 범위로 삼지 않으므로 억지로 확장하지
않고, full CI와 Nightly의 기존 module task에 새 모듈을 등록한다.

## 단계 1 — 모듈·의존성 등록과 baseline

- [x] `gradle/libs.versions.toml`의 `# exposed` 영역에 다음 versionless alias를
  추가한다. 버전은 `bluetape4k-dependencies = "1.4.0"` BOM에서 해석한다.

  ```toml
  exposed-measured = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-measured" }
  ```

- [x] `06-advanced/13-exposed-measured/build.gradle.kts`를 기존
  `06-advanced/05-exposed-money` 패턴으로 만든다.

  - `testImplementation(project(":exposed-shared-tests"))`
  - `libs.jetbrains.exposed.core`, `libs.jetbrains.exposed.dao`,
    `libs.jetbrains.exposed.jdbc`
  - `libs.exposed.core`와 새 `libs.exposed.measured`
  - `libs.bluetape4k.junit5`, `libs.bluetape4k.testcontainers`,
    `libs.testcontainers`, MariaDB/MySQL/PostgreSQL Testcontainers
  - H2, MariaDB, MySQL, PostgreSQL, pgjdbc-ng test runtime driver
  - `testImplementation`이 `compileOnly`/`runtimeOnly`를 상속하는 현재 설정

  R2DBC, Redis, Caffeine, 새 버전 override는 추가하지 않는다.

- [x] `settings.gradle.kts`의 `includeModules("06-advanced", false, false)`가
  새 leaf directory를 `:13-exposed-measured`로 자동 발견하는지 확인한다.

- [x] alias와 provider 좌표를 다음 명령으로 검증한다.

  ```bash
  ./gradlew projects --no-daemon --no-configuration-cache
  ./gradlew :13-exposed-measured:dependencyInsight \
    --dependency bluetape4k-exposed-measured \
    --configuration testRuntimeClasspath \
    --no-daemon --no-configuration-cache
  ```

  기대 결과는 project 목록에 `:13-exposed-measured`가 나타나고 provider 버전이
  BOM `1.4.0`에서 선택되는 것이다.

- [x] 코드/테스트를 건드리기 전 현재 baseline을 feature worktree에서 보존한다.

  ```bash
  USE_FAST_DB=true repo-test-summary -- \
    ./gradlew :05-exposed-money:test --no-build-cache --no-daemon --no-configuration-cache
  git diff --check
  ```

## 단계 2 — TDD RED: 측정값 계약을 먼저 고정한다

- [x] `Ex01MeasuredColumns.kt`에 테스트 이름과 기대 동작을 먼저 작성한다.
  `MeasuredData.kt`가 아직 없을 때의 compile RED를 기록한 뒤, 테스트가 실행될 수
  있도록 최소한의 table/entity 선언만 추가한다. provider 변환 로직은 복제하지
  않는다.

  1. `length`, `mass`, `temperature`를 서로 다른 표시 단위로 insert하고 DSL
     select에서 meter/kg/K 기준 값과 표시 값이 `shouldBeNear`로 왕복되는지 확인한다.
  2. 같은 상품을 `ProductEntity.new {}`로 만들고 DAO property에서 동일한 값과
     단위 변환을 읽는지 확인한다. DSL과 DAO가 같은 테이블을 사용해야 한다.
  3. `nullableMass`를 생략한 row가 NULL로 읽히고 0으로 치환되지 않는지 확인한다.
  4. 소수 길이, 큰 질량 또는 큰 길이 값을 `DOUBLE` 허용 오차로 검증하고
     DECIMAL precision/scale 정책을 발명하지 않는다.
  5. provider의 `MeasureColumnType`/`TemperatureColumnType`이 숫자가 아닌
     DB 값을 거부한다는 source 계약을 README와 source ledger에 기록한다. 정상
     JDBC driver가 숫자를 반환하는 경계와 workshop의 정상 경로를 분리하며,
     provider 책임인 임의 타입 주입 테스트는 중복하지 않는다.
  6. `temperature()`가 `Temperature`를, `temperatureDelta()`가 별도
     `TemperatureDelta`를 받는다는 타입 경계를 KDoc/README compile-time 예로
     남긴다. runtime unsafe cast 테스트는 추가하지 않는다.

- [x] `MeasuredData.kt`는 다음 범위의 최소 모델만 제공한다.

  ```kotlin
  object ProductTable : IntIdTable("measured_products") {
      val name = varchar("name", 100)
      val length = length("length")
      val mass = mass("mass")
      val temperature = temperature("temperature")
      val nullableMass = mass("nullable_mass").nullable()
  }

  class ProductEntity(id: EntityID<Int>) : IntEntity(id) {
      companion object : EntityClass<Int, ProductEntity>(ProductTable)

      var name by ProductTable.name
      var length by ProductTable.length
      var mass by ProductTable.mass
      var temperature by ProductTable.temperature
      var nullableMass by ProductTable.nullableMass
  }
  ```

  실제 구현에서는 provider extension receiver가 property 이름에 가려지지 않도록
  필요한 경우 import alias 또는 명시적 receiver를 사용한다. 모든 Exposed import는
  `org.jetbrains.exposed.v1.*` 계열만 사용한다. table/entity와 테스트의 reader-facing
  KDoc 및 주석은 한국어로 작성한다.

- [x] 기존 모듈에서 복사한 `junit-platform.properties`와 `logback-test.xml`은
  package/logger 이름만 새 모듈에 맞추고, `junit.jupiter.execution.parallel.enabled=false`
  및 `maxParallelUsages = 1` 계약을 유지한다.

- [x] provider 연결 전 RED 명령을 실행하고, 실패가 “provider DSL/모듈 심볼 부재 또는
  기대 assertion 불일치”인지 원시 출력으로 기록한다.

  ```bash
  USE_FAST_DB=true ./gradlew :13-exposed-measured:test \
    --tests 'exposed.examples.measured.Ex01MeasuredColumns' \
    --no-build-cache --no-daemon --no-configuration-cache
  ```

## 단계 3 — TDD GREEN: provider DSL/DAO 구현을 최소화한다

- [x] RED에서 고정한 테스트를 만족하도록 `MeasuredData.kt`의 DSL 선언과 DAO
  delegated property를 완성한다. `MeasureColumnType`, `TemperatureColumnType`,
  `TemperatureDeltaColumnType`를 로컬에 재구현하지 않고 `libs.exposed.measured`의
  공개 API를 그대로 호출한다.

- [x] JDBC 테스트는 `AbstractExposedTest`, `TestDB`, `withTables`,
  `@ParameterizedTest`, `@MethodSource(ENABLE_DIALECTS_METHOD)`를 사용한다.
  `transaction {}` 밖의 전역 DB 연결이나 수동 connection pool을 만들지 않는다.

- [x] 길이 입력은 centimeters/meters/kilometers, 질량 입력은 grams/kilograms,
  온도 입력은 Celsius/Fahrenheit/Kelvin을 최소 한 번씩 보여 준다. assertion은
  `io.bluetape4k.assertions.shouldBeNear`를 사용해 대표적인 허용 오차를 명시한다.

- [x] `DOUBLE` serialization/read-back의 기준 단위를 확인한다. 필요한 경우
  `columnType.sqlType() == "DOUBLE"`와 `MeasureColumnType` base unit을 단위
  테스트에서 확인하되 provider 전체 테스트를 중복하지 않는다.

- [x] GREEN focused test를 실행하고, provider의 invalid DB value source 계약은
  README/source ledger로 확인한다. 정상 JDBC driver 출력에 인위적인 값을
  주입하는 workshop 테스트는 추가하지 않는다.

  ```bash
  USE_FAST_DB=true ./gradlew :13-exposed-measured:test \
    --tests 'exposed.examples.measured.Ex01MeasuredColumns' \
    --no-build-cache --no-daemon --no-configuration-cache
  ```

## 단계 4 — README와 chapter/repository index를 source-equivalent로 작성한다

- [x] `06-advanced/13-exposed-measured/README.md`와 `README.ko.md`를 같은 섹션
  순서로 작성한다.

  1. 학습 목표와 `bluetape4k-exposed-measured` BOM alias
  2. `ProductTable`의 `DOUBLE` 기준 단위(meter/kg/K)와 `ProductEntity`
  3. DSL insert/select와 DAO read-back 코드
  4. nullable, 소수/큰 값 허용 오차, unsupported DB value source contract 문서,
     compile-time 계열 경계
  5. DB가 원래 표시 단위를 보존하지 않는다는 migration 주의
  6. `DOUBLE`은 금융/법정 계량 정확도 정책이 아니라는 범위
  7. JDBC/H2 fast profile 실행 명령과 지원 dialect 안내
  8. R2DBC는 `exposed-r2dbc-workshop` 별도 이슈라는 범위 안내
  9. architecture/ERD PNG 참조

- [x] `06-advanced/README.md`와 `README.ko.md`의 Included Modules/포함 모듈 표,
  권장 학습 순서, 실행 명령에 `13-exposed-measured`를 같은 위치와 의미로
  추가한다.

- [x] repository root `README.md`와 `README.ko.md`의 Advanced Features/고급
  기능 목록에 새 module link와 한영 설명을 추가한다. 예제 README의 코드,
  명령, API 이름, URL은 양쪽에서 동일하게 유지하고 prose만 번역한다.

- [x] `git diff --check`, writer terminology audit와 간단한 EN/KO heading/link
  parity 검사를 실행한다.

  ```bash
  git diff --check
  node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
    --series clinic-appointment \
    06-advanced/13-exposed-measured/README.md \
    06-advanced/13-exposed-measured/README.ko.md \
    06-advanced/README.md 06-advanced/README.ko.md \
    README.md README.ko.md
  ```

## 단계 5 — CI/Nightly 및 workflow registration을 고정한다

- [x] `.github/workflows/nightly.yml`의 PostgreSQL shard-1, MySQL shard-1,
  MariaDB smoke task에 `:13-exposed-measured:test`를 `:05-exposed-money:test`
  인접 위치로 추가한다. H2 matrix의 전체 `test`와 `.github/workflows/ci.yml`의
  전체 test/assemble/detekt는 새 Gradle project를 자동 포함하므로 변경하지
  않는다.

- [x] module discovery와 CI task registration을 다음으로 검증한다.

  ```bash
  ./gradlew projects --no-daemon --no-configuration-cache
  rg -n ':13-exposed-measured:test|13-exposed-measured' \
    .github/workflows/nightly.yml settings.gradle.kts 06-advanced
  actionlint .github/workflows/ci.yml .github/workflows/nightly.yml
  ```

- [x] changed-example selector는 현재 `06-advanced`를 mapping하지 않는 기존
  fixed weekly 정책을 유지한다는 것을 기록한다. 새 모듈 경로가 selector에
  누락되어 PR에서 조용히 빠지는 일이 없는지 `ci.yml` 전체 test와 nightly task
  양쪽을 근거로 확인한다.

## 단계 6 — 한영 architecture/ERD SVG·PNG를 생성하고 감사한다

- [x] 기존 자산 스타일을 확인한 뒤 다음 source pair를 만든다.

  - `docs/images/readme-diagrams/06-advanced-13-exposed-measured-architecture-01.svg`
  - `docs/images/readme-diagrams/06-advanced-13-exposed-measured-architecture-01.ko.svg`
  - `docs/images/readme-diagrams/06-advanced-13-exposed-measured-erd-01.svg`
  - `docs/images/readme-diagrams/06-advanced-13-exposed-measured-erd-01.ko.svg`

- [x] architecture는 `Input Measure`, `Provider ColumnType`, `JDBC DOUBLE`,
  `ProductTable`, `DSL/DAO read-back`, `Dialect Test`의 연결된 책임 흐름을
  8개 이하 노드로 표현한다. ERD는 `measured_products`의 `id`, `name`, `length`,
  `mass`, `temperature`, `nullable_mass`와 m/kg/K 기준 단위를 표현한다.
  reader-facing prose가 있으므로 영어/한국어 SVG를 별도로 유지한다.

- [x] 각 SVG의 visible text, branch/decision, long identifier를
  `.semantic.json` ledger에 기록한다. ledger의 `revision`은 생성 시점의
  실제 `git rev-parse HEAD`로 채우고, PNG는 SVG를 CairoSVG scale 2로 재생성한다.
  raw Mermaid/Graphviz를 README에 넣지 않는다.

- [x] 다음 감사와 `view_image(detail="original")` 시각 검토를 순차 실행한다.

  ```bash
  for svg in docs/images/readme-diagrams/06-advanced-13-exposed-measured-*.svg; do
      png="${svg%.svg}.png"
      ledger="${svg%.svg}.semantic.json"
      xmllint --noout "$svg"
      cairosvg "$svg" -o "$png" -s 2
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-semantic-audit.py \
        --repo-root . --json "$ledger"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-connector-audit.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-arrowhead-audit.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-geometry-audit.py --fail-diagonal "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-endpoint-audit.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-mixed-corner-audit.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-svg-text-normalize.py "$svg"
      python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-visual-audit.py --require-opaque "$png"
  done
  python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-asset-pair-audit.py \
    --asset-dir docs/images/readme-diagrams \
    --readme 06-advanced/13-exposed-measured/README.md
  python3 /Users/debop/.codex/skills/bluetape-diagram/scripts/diagram-asset-pair-audit.py \
    --asset-dir docs/images/readme-diagrams \
    --readme 06-advanced/13-exposed-measured/README.ko.md
  ```

  `diagram-svg-text-normalize.py`가 SVG를 수정할 수 있으므로 실행 후 `git diff`
  를 다시 읽고 semantic ledger와 PNG를 재생성한다. 모든 heavyweight/visual
  검사는 병렬화하지 않는다.

## 단계 7 — 성능·안정성 위험 예측과 최종 검증

다음 위험은 provider/DB 예제의 범위에 실제로 걸리므로 Step 3-P에서 N/A로
처리하지 않는다.

| 위험 | 조기 신호 | 완화/검증 | rollback/rerun |
|---|---|---|---|
| provider alias가 BOM에서 해석되지 않음 | dependency insight/compile 실패 | catalog alias를 중앙 coordinate와 대조하고 직접 version override를 금지 | catalog/build.gradle만 되돌리고 단계 1부터 재실행 |
| DB dialect가 `DOUBLE` 또는 nullable 측정값을 다르게 처리함 | H2만 통과하고 PG/MySQL/MariaDB row 왕복 실패 | `ENABLE_DIALECTS_METHOD`, `USE_FAST_DB=true`, `-PuseDB=POSTGRESQL/MYSQL_V8/MARIADB` 순서 검증 | 테스트/driver 범위만 수정하고 provider 계약은 유지 |
| `DOUBLE` exact assertion이 flaky함 | Celsius offset/큰 값에서 간헐 실패 | `shouldBeNear`와 값 크기별 허용 오차, finite representative values | assertion만 조정하고 DECIMAL 정책은 추가하지 않음 |
| provider type과 DAO delegated property가 drift함 | DSL row와 DAO entity가 다른 단위 또는 null을 반환 | 같은 `ProductTable`을 사용한 두 read-back 테스트와 column type/base unit 확인 | `MeasuredData.kt`의 위임 선언을 되돌리고 GREEN 재실행 |
| 기존 테스트와 shared DB 설정이 충돌함 | 병렬 실행 lock/테이블 잔존/컨테이너 포화 | `maxParallelUsages = 1`, test resource 직렬 설정, Testcontainers는 순차 실행 | 새 module test resource만 되돌리고 다른 모듈은 건드리지 않음 |
| 기준 단위가 문서/다이어그램과 코드에서 어긋남 | README, ERD, source column 선언의 m/kg/K 불일치 | source-equivalent parity, semantic ledger, root/chapter index read-back | docs asset slice만 재생성하고 schema/API는 변경하지 않음 |
| Nightly/CI가 새 module을 실행하지 않음 | `:13-exposed-measured:test` registration 부재 | `projects`, nightly grep, full CI task와 actionlint 검증 | workflow task만 보정 후 affected checks 재실행 |

- [x] performance/stability review artifact를 `docs/review/2026-08-27-issue-238-exposed-measured-performance-stability.md`에 작성한다. 요청당 무거운 allocation/반복 변환을 새로 만들지 않는지, JDBC 자원 소유권/정리, Testcontainers 직렬성, `DOUBLE` 경계와 재실행 지점을 소스·테스트 근거로 기록한다. benchmark는 도메인 학습 예제에 포함하지 않으며 `N/A (production benchmark out of scope)`로 사유를 남긴다.

- [x] 모듈 검증을 작은 순서에서 넓은 순서로 실행한다.

  ```bash
  USE_FAST_DB=true repo-test-summary -- \
    ./gradlew :13-exposed-measured:test --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew :13-exposed-measured:build --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew :13-exposed-measured:detekt --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew detekt --no-build-cache --no-daemon --no-configuration-cache
  ./gradlew :13-exposed-measured:test -PuseDB=POSTGRESQL \
    --tests 'exposed.examples.measured.*' \
    --no-build-cache --no-daemon --no-configuration-cache --max-workers=1
  git diff --check
  ```

  Docker/Testcontainers가 필요한 dialect 검사는 macOS Colima context와
  `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` 상속 상태를 확인한
  뒤 순차 실행한다. skip/실패를 성공으로 취급하지 않는다.

## 단계 8 — traceability, final review, lesson, workflow receipt

- [x] issue #238 live metadata(title/body/assignee/milestone/labels)와 현재 diff를
  다시 읽어 모든 DoD를 다음 표에 매핑한다.

| Issue DoD | 계획 task | 증거 |
|---|---|---|
| 길이·질량·온도 3계열 round-trip | 2, 3 | `Ex01MeasuredColumns.kt`, H2 및 dialect 결과 |
| null·소수 정밀도·단위 변환·부적합 단위 동작 | 2, 3, 4 | near assertion, nullable, README compile-time 예와 provider source contract |
| base unit 저장과 migration 주의 | 3, 4, 6 | source declaration, README/ERD, semantic ledger |
| EN/KO README와 source-equivalent SVG/PNG | 4, 6 | parity/terms/diagram audits 및 original PNG inspection |
| module test/static check | 1, 3, 7 | Gradle test/build/detekt, full detekt |
| 신규 schema 격리/R2DBC 제외 | 1, 3, 4 | changed path audit, README scope, live issue read-back |

- [x] `docs/review/2026-08-27-issue-238-exposed-measured-final-review.md`에
  performance, stability, security, operator/ops, developer/API, user/caller 여섯
  관점과 통합 판정을 기록한다. 단일 개발자 lane이므로 parallel subagent는
  `N/A (single-developer lane)`로 명시하되 독립적인 관점 검토 자체는 leader가
  순차 수행한다. P0=0/P1=0이 아니면 구현/PR을 진행하지 않는다.

- [x] `docs/lessons/2026-08-27-issue-238-exposed-measured.md`에 provider 기준
  단위 source-of-truth, `DOUBLE`/near assertion, nullable/DAO parity,
  workflow registration과 재발 방지 guard를 한국어로 기록한다.

- [x] workflow helper에 `spec-plan`, `module-test`, `static-check`,
  `docs-parity`, `workflow-registration`, `review`, `lesson` evidence를 등록하고
  lane/topology changed paths가 write scope 안인지 확인한다. `lane-complete`,
  `check-result`, `component-evidence`, `completion-check`을 순서대로 실행한다.

- [x] 모든 durable artifact와 코드/문서/다이어그램 변경을 Lore commit trailer와
  함께 커밋한다. 각 commit은 한국어 intent line, Constraint/Rejected/Confidence/
  Scope-risk/Directive/Tested/Not-tested를 포함한다.

## 단계 9 — PR 및 merge handoff

- [ ] CG-01~CG-10과 A-01~A-09가 PASS이고 정확한 local head가 고정된 뒤에만
  `feat/issue-238-exposed-measured`를 `origin`에 push한다.
- [ ] PR 생성 직전에 user/workspace/repository `AGENTS.md`, `$bluetape-workflow`,
  `$bluetape-kotlin-patterns`, common gates, PR template, issue #238 metadata를
  다시 읽는다. PR body는 한국어로 작성하고 issue milestone/labels/assignee
  (`debop`)와 맞추며 마지막 heading을 `## DoD Status`로 둔다.
- [ ] exact head SHA, base/head, PR body, CI status, reviews/threads,
  mergeability를 live-read-back한다. single-developer lane의 human-review
  sub-item은 concrete scope evidence와 함께 `N/A`로 기록할 수 있지만 CI,
  independent final review, diagrams/lesson은 생략하지 않는다.
- [ ] CG-15에서 `Required checks: X/Y; N/A: N; Blocked: 0`와 exact PR/head를
  보고하고 fresh `승인` 전에는 merge/auto-merge하지 않는다. 승인 후에만 merge,
  merged SHA 확인, 실제 `develop` checkout sync, auto-deleted remote feature
  branch와 proven merged worktree cleanup을 수행한다.

## plan self-review / writer gate

- [x] **SPW-01** — 독자, 목표, 모듈/API 범위, JDBC-only/R2DBC 제외와 issue/provider
  근거를 명시한다.
- [x] **SPW-02** — ordered tasks, file impact, alternatives, tests, failure modes,
  docs/diagram, CI/nightly, rollback과 risk prediction을 포함한다.
- [x] **SPW-03** — work document 문장은 한국어로 쓰고 code/API/identifier/command/
  URL/version은 원문을 보존한다.
- [x] **SPW-04** — local `05-exposed-money`, shared test, catalog와 provider source
  계약을 재사용하며 새 dependency/abstraction의 이유를 기록한다.
- [x] **SPW-05** — plan Markdown/용어 감사와 Step 3-R 여섯 렌즈 및 main integration
  review를 수행하고 P0/P1=0으로 수렴한다.

## 계획 상태

`IMPLEMENTATION-VERIFIED / READY-FOR-PR` — 승인된 계획의 단계 1–8과 최종
여섯 관점 검토를 P0=0/P1=0으로 완료했다. 사용자 plan approval(2026-08-27)을
받아 구현·검증·문서화를 마쳤고, 이제 PR/CI live gate만 남았다.

## 중단 조건

- provider artifact가 BOM에서 해석되지 않거나 source 계약이 바뀌면 dependency를
  임의로 대체하지 않고 단계 1로 되돌아가 근거를 갱신한다.
- 기존 모듈의 dirty state, 다른 worktree, R2DBC 저장소에 영향을 주는 변경이
  발견되면 해당 경로를 보존하고 범위 밖 변경을 중단한다.
- module test/static/docs/diagram/final review 중 하나라도 fresh evidence 없이
  실패하면 원시 실패를 진단한 뒤 해당 단계부터 재실행한다.
- issue DoD와 plan 간 material mismatch가 발견되면 사용자 승인 없이 설계를
  확장하지 않는다.
