# 이슈 #238 Exposed measured 단위 컬럼 예제 설계

## 목표

이슈 [#238](https://github.com/bluetape4k/exposed-workshop/issues/238)의 목표는
`bluetape4k-exposed-measured` provider가 제공하는 측정값 컬럼 타입을 JDBC 기반
Exposed 예제로 고정하는 것이다. 하나의 상품 도메인에 길이, 질량, 절대온도 컬럼을
매핑하고 DSL 조회와 DAO 조회를 모두 보여 주며, 입력 단위가 기준 단위로 정규화되어
왕복되는 의미를 테스트와 문서로 보존한다.

이번 변경은 한 명의 개발자가 유지하는 workshop의 최소 변경으로 제한한다. 새
예제는 `06-advanced/13-exposed-measured`에 격리하며 기존 모듈의 스키마나 커스텀
컬럼을 수정하지 않는다. 구현과 검증은 Exposed JDBC에 한정하고, R2DBC 구현은
`exposed-r2dbc-workshop` 저장소의 별도 이슈에서 다룬다.

## 현재 근거와 source ledger

| 근거 | 확인한 계약 |
|---|---|
| 이슈 [#238](https://github.com/bluetape4k/exposed-workshop/issues/238) | `06-advanced` 아래 measured alias/모듈, 상품 길이·질량·온도 컬럼, DAO/DSL 조회, nullable·정밀도·변환·부적합 입력 검증, EN/KO README와 SVG/PNG 다이어그램을 요구한다. |
| `gradle/libs.versions.toml` | `bluetape4k-dependencies = "1.4.0"` BOM을 사용하며 `exposed-core`, `exposed-dao`, `jetbrains-exposed-jdbc` 등 consumer alias가 있다. `exposed-measured` alias는 아직 없다. |
| `/Users/debop/work/bluetape4k/bluetape4k-dependencies/gradle/libs.versions.toml` | `bluetape4k-exposed-measured` 좌표를 `bluetape4k-exposed-bom` 버전으로 관리한다. workshop catalog에는 버전을 하드코딩하지 않고 이 BOM 계약을 따른다. |
| `/Users/debop/work/bluetape4k/bluetape4k-exposed/exposed/measured/src/main/kotlin/io/bluetape4k/exposed/core/measured/MeasuredColumnTypes.kt` | `measure`, `length`, `mass`, `temperature`, `temperatureDelta` DSL을 제공하고 내부적으로 `DOUBLE`을 사용한다. `Measure`는 지정 기준 단위 값으로 저장하고 `Temperature`는 Kelvin, `TemperatureDelta`는 Kelvin 차이로 저장한다. DB에서 `Number`가 아닌 값은 `error(...)`로 실패한다. |
| `/Users/debop/work/bluetape4k/bluetape4k-projects/utils/measured/src/main/kotlin/io/bluetape4k/measured/Units.kt` | 같은 단위 계열 안에서 `Measure<T>`의 `in`/`as` 변환을 수행하며 값 객체는 불변이다. 제네릭 타입 경계가 길이·질량처럼 서로 다른 계열의 혼용을 컴파일 시점에 막는다. |
| `/Users/debop/work/bluetape4k/bluetape4k-projects/utils/measured/src/main/kotlin/io/bluetape4k/measured/Temperature.kt` | 절대 `Temperature`와 `TemperatureDelta`를 별도 타입으로 구분하고, 절대온도는 Kelvin 기준으로 복원한다. |
| `06-advanced/05-exposed-money` | `IntIdTable`, `IntEntity`, `EntityClass`, `withTables`, `AbstractExposedTest`, Korean KDoc/README 및 다이어그램을 사용하는 현재 workshop 패턴의 기준이다. |

provider 소스가 이슈 본문의 요약보다 우선한다. 따라서 예제는 provider가 노출한
DSL과 변환 방식을 직접 사용하고, 별도의 단위 코드 컬럼이나 로컬 `ColumnType`
복제본을 만들지 않는다.

## 책임 경계

### provider가 소유하는 책임

- `MeasureColumnType`, `TemperatureColumnType`, `TemperatureDeltaColumnType`의
  Exposed `DOUBLE` 매핑
- 입력 측정값을 컬럼의 기준 단위로 변환하는 계산
- DB 숫자값을 `Measure`, `Temperature`, `TemperatureDelta`로 복원
- 길이·질량·온도 및 기타 측정 계열의 타입 안전한 DSL 확장
- 절대온도와 온도차의 서로 다른 Kelvin 저장 의미

### workshop 예제가 소유하는 책임

- `ProductTable`과 `ProductEntity`를 이용한 도메인 모델/DAO 예시
- JDBC `transaction {}` 안의 insert, DSL select, DAO read-back 흐름
- nullable 값과 여러 표시 단위 입력을 포함한 학습 가능한 테스트
- `DOUBLE` 정밀도와 기준 단위 고정에 관한 문서 및 마이그레이션 주의사항
- 테스트용 격리 테이블과 H2/지원 JDBC dialect 실행 경계

두 책임을 같은 코드로 중복 구현하지 않는다. 예제는 provider 변환 함수를 감싸는
adapter나 단위 문자열 저장 컬럼을 추가하지 않고, 학습자가 provider 계약을
그대로 읽을 수 있는 얇은 도메인 경계만 제공한다.

## 선택지와 결정

### 선택지 A — 하나의 Product DSL + DAO 예제 모듈 (채택)

`06-advanced/13-exposed-measured` 모듈에 다음을 둔다.

- `ProductTable : IntIdTable("measured_products")`
- `length`, `mass`, `temperature` 필드와 선택적인 nullable `shippingLength`
  또는 `storageTemperature` 필드
- `ProductEntity : IntEntity`와 `EntityClass<Int, ProductEntity>`
- DSL insert/select와 DAO insert/read-back을 같은 테스트에서 비교

기본 컬럼은 `length("length")`, `mass("mass")`, `temperature("temperature")`로
선언한다. DB에는 각각 meter, kilogram, Kelvin의 `DOUBLE` 값이 저장되고 조회 시
provider가 해당 타입 객체를 복원한다. 표시 단위(millimeter/kilometer,
gram/kilogram, Celsius/Fahrenheit)는 애플리케이션 입력과 assertion에서 보여 주되
DB 스키마에는 저장하지 않는다.

한 모듈 안에서 DSL과 DAO를 함께 보여 주는 방식은 issue가 요구한 두 조회 경계를
충족하면서 파일 수와 스키마 수를 늘리지 않는 한 개발자용 최소 범위다.

### 선택지 B — DSL 전용 측정 컬럼 예제 (기각)

provider의 컬럼 DSL을 보여 주지만 DAO/EntityClass 학습 경계를 충족하지 못한다.
이미 `06-advanced`에 존재하는 단순 컬럼 예제와 차별성이 약해 issue의 수용 기준을
완성할 수 없으므로 채택하지 않는다.

### 선택지 C — 수치 컬럼 + 원래 단위 문자열 컬럼을 함께 저장 (기각)

provider가 보장하는 기준 단위 정규화 대신 애플리케이션이 단위 문자열을 해석해야
하고, 단위 변경·검색·마이그레이션 책임이 이 예제에 유입된다. provider의 타입 안전
계약을 우회하며 이번 issue의 최소 범위를 벗어나므로 채택하지 않는다.

### 선택지 D — DECIMAL 기반 정밀 측정 컬럼 또는 정확도 정책 추가 (기각)

현재 provider 구현은 `DoubleColumnType`/`DOUBLE`을 기준으로 한다. 이 issue에서
새로운 `BigDecimal` column type, 오차 예산, domain validation 정책까지 추가하면
provider 예제의 의미가 바뀐다. 대신 근사 비교와 큰/소수 값 왕복을 테스트하고
`DOUBLE`의 정밀도 한계를 README에 명시한다.

## 데이터 흐름과 저장 계약

```text
표시 단위 입력
  -> Measure<T> / Temperature
  -> provider ColumnType이 기준 단위로 변환
  -> JDBC DOUBLE (meter / kilogram / Kelvin)
  -> Number read-back
  -> provider가 Measure<T> / Temperature 복원
  -> DSL Row 또는 DAO Entity에서 지정 단위로 표현
```

- `150.centimeters()`는 길이 기준 단위인 meter 값 `1.5`로 저장되고, 조회된
  `Measure<Length>`는 `in Length.centimeters`로 다시 `150.0`에 가까워진다.
- `2.kilograms()`는 kilogram 기준 `2.0`으로 저장되며, gram assertion은
  `in Mass.grams`로 수행한다.
- `25.celsius()`는 `298.15` Kelvin으로 저장되고, 조회된 `Temperature`는
  `inCelsius()`로 표시한다. 절대온도에는 `TemperatureDelta`를 대입하지 않는다.
- nullable 컬럼은 Exposed의 `nullable()`을 적용한다. NULL은 NULL로 왕복하며
  임의의 0 또는 기준 단위 값을 대체하지 않는다.
- DB는 원래 입력 단위나 표시 형식을 기억하지 않는다. 기준 단위는 스키마와
  애플리케이션 계약의 일부이므로 바꾸려면 기존 데이터를 일괄 변환해야 한다.

## API 및 테스트 계약

예제 API의 핵심 형태는 다음과 같다. 실제 import는 `org.jetbrains.exposed.v1.*`
및 provider가 공개한 `io.bluetape4k.exposed.core.measured.*`를 사용한다.

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

테스트는 `AbstractExposedTest`, `enableDialects()`, `@MethodSource(ENABLE_DIALECTS_METHOD)`
및 `withTables(testDB, ProductTable)` 패턴을 따른다. 기본 확인 항목은 다음과 같다.

1. 길이: centimeters/meters/kilometers 입력이 meter 기준 `DOUBLE`로 저장되고,
   DSL select와 DAO read-back에서 `shouldBeNear`로 원래 수치를 복원한다.
2. 질량: grams/kilograms 입력과 DSL/DAO 조회가 kilogram 기준으로 왕복한다.
3. 절대온도: Celsius/Fahrenheit/Kelvin 입력이 Kelvin으로 저장되고
   `inCelsius()`/`inFahrenheit()`로 표시된다.
4. nullable: 지정하지 않은 nullable 측정값이 `null`로 조회된다.
5. 정밀도/크기: 소수와 큰 값의 왕복은 exact equality가 아니라 provider의
   `DOUBLE` 특성에 맞춰 허용 오차로 검증한다. 이 예제는 금융 수준 정확도나
   반올림 정책을 선언하지 않는다.
6. 타입 안전성: 길이 컬럼에 `Mass` 또는 `Temperature`를 대입하는 코드는
   컴파일되지 않는다는 사실을 README의 compile-time 예로 설명한다. 런타임
   reflection 우회나 인위적인 `assertFailsWith` 계약은 추가하지 않는다.
7. provider 실패 경계: `valueFromDB`가 `Number`가 아닌 값에 `error(...)`를
   발생시킨다는 provider 계약을 README와 source ledger에 문서화한다. workshop의
   정상 JDBC 경로에서 임의 타입을 주입하는 테스트는 provider 모듈의 책임이므로
   중복하지 않는다.

## 실패 모드와 대응

| 실패 모드 | 관찰 가능한 결과 | 대응/검증 |
|---|---|---|
| 기준 단위 변환을 우회하거나 표시 단위를 DB에 저장함 | 같은 물리량이 입력 단위에 따라 다른 DB 수치가 되고 단위 문자열 drift가 생김 | provider DSL만 사용하고 meter/kg/K 기준 수치의 DSL·DAO 왕복을 확인한다. |
| nullable 측정값을 0으로 대체함 | 미입력과 실제 0이 구분되지 않음 | `nullable()` 컬럼의 NULL insert/read-back을 별도 테스트한다. |
| `DOUBLE` 근사 오차를 exact equality로 검증함 | Celsius 변환 또는 큰 값 왕복 테스트가 dialect/JVM에 따라 불안정함 | `shouldBeNear` 허용 오차와 representative decimal/large values를 사용한다. |
| 절대온도와 온도차를 혼용함 | Celsius offset이 적용된 절대값을 delta로 저장하거나 컴파일 오류 발생 | `temperature()`와 `temperatureDelta()`를 별도 개념으로 문서화하고 이번 도메인은 절대온도만 사용한다. |
| 서로 다른 단위 계열을 런타임에 섞음 | 길이 컬럼에 질량/온도를 넣을 수 없거나 unsafe cast가 필요함 | `Measure<T>` 제네릭 API를 그대로 노출하고 compile-time boundary를 문서화한다. |
| migration에서 기준 단위를 변경함 | 기존 `DOUBLE` 값이 새 단위로 오해되어 수치가 배수만큼 틀어짐 | 기준 단위(m/kg/K)를 README에 고정하고 변경 시 데이터 변환 migration이 필요함을 명시한다. |
| DB driver가 숫자가 아닌 값을 반환함 | provider `valueFromDB`가 지원되지 않는 타입 오류를 던짐 | provider source 계약을 source ledger와 README에 기록하고 workshop 정상 JDBC 설정은 변경하지 않는다. |

## 의존성·구성 변경

workshop의 `gradle/libs.versions.toml`에 BOM 기반 consumer alias를 하나 추가한다.

```toml
exposed-measured = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-measured" }
```

새 모듈의 `build.gradle.kts`에는 `testImplementation(project(":exposed-shared-tests"))`,
Exposed core/dao/jdbc, `libs.exposed.measured`, `libs.bluetape4k.junit5`와 현재
shared test가 요구하는 JDBC driver/Testcontainers 의존성을 기존 `05-exposed-money`
패턴에 맞춰 추가한다. 버전을 직접 고정하거나 R2DBC/Caffeine/Redis 의존성을
추가하지 않는다. `settings.gradle.kts`의 `includeModules("06-advanced", false,
false)`가 `13-exposed-measured`를 자동으로 포함하는지 `gradlew projects`로
확인한다.

## 호환성·마이그레이션·롤백

- 기존 모듈, 기존 테이블, 기존 public API는 변경하지 않는다.
- 새 `measured_products`는 테스트 트랜잭션에서 생성되는 독립 학습 테이블이다.
- 저장 기준 단위는 길이 meter, 질량 kilogram, 절대온도 Kelvin으로 고정한다.
  DB에는 표시 단위 metadata가 없으므로 기준 단위를 바꾸는 migration은 이 예제의
  범위를 넘어선다.
- `DOUBLE`은 유한 정밀도이므로 금융/법정 계량 정확도 요구에 재사용하지 않는다.
  정확한 소수 정책은 별도 설계와 별도 provider가 필요하다.
- rollback은 새 모듈, consumer alias, README/diagram 변경을 함께 제거하는 한
  feature 커밋 단위로 가능해야 하며, 기존 `06-advanced` 모듈의 파일은 건드리지
  않는다.

## 문서·다이어그램

새 모듈에는 source-equivalent 문서를 둔다.

- `06-advanced/13-exposed-measured/README.md`
- `06-advanced/13-exposed-measured/README.ko.md`

두 README는 같은 순서로 다음을 설명한다.

1. provider alias와 JDBC-only 범위
2. `ProductTable`의 `DOUBLE` 기준 단위(m/kg/K) 선언
3. DSL select와 DAO read-back
4. nullable·근사 정밀도·타입 안전성 테스트
5. DB가 원래 표시 단위를 보존하지 않는다는 migration 주의
6. R2DBC는 `exposed-r2dbc-workshop` 별도 이슈 범위라는 안내

독자에게 보이는 prose가 있으므로 영어와 한국어 ERD/architecture asset pair를
각각 제공한다. 새 자산은 `docs/images/readme-diagrams/` 아래에 둔다.

- `06-advanced-13-exposed-measured-architecture-01.svg`
- `06-advanced-13-exposed-measured-architecture-01.png`
- `06-advanced-13-exposed-measured-architecture-01.ko.svg`
- `06-advanced-13-exposed-measured-architecture-01.ko.png`
- `06-advanced-13-exposed-measured-erd-01.svg`
- `06-advanced-13-exposed-measured-erd-01.png`
- `06-advanced-13-exposed-measured-erd-01.ko.svg`
- `06-advanced-13-exposed-measured-erd-01.ko.png`

architecture diagram은 입력 측정값, provider 기준 단위 변환, JDBC `DOUBLE`, DSL/
DAO read-back과 테스트 경계를 보여 준다. ERD는 `measured_products`의 id/name/
length/mass/temperature/nullable_mass 컬럼과 기준 단위 주석을 보여 준다.
Mermaid/Graphviz raw block은 README에 넣지 않고, SVG source를 직접 관리하며
CairoSVG scale 2 렌더링 및 XML·semantic·connector·arrowhead·geometry·visual·
asset-pair 감사를 수행한다.

`06-advanced/README.md`와 `README.ko.md`의 모듈 목록, 권장 학습 순서, 테스트
명령도 새 모듈과 source-equivalent가 되도록 갱신한다.

## 수용 기준

- [x] `gradle/libs.versions.toml`의 `exposed-measured` alias가
  `bluetape4k-dependencies:1.4.0` BOM으로 해석된다.
- [x] `06-advanced/13-exposed-measured`가 자동으로 Gradle project가 되고,
  `ProductTable`/`ProductEntity`가 `org.jetbrains.exposed.v1.*`와 provider DSL을
  사용한다.
- [x] 길이·질량·절대온도의 meter/kg/K 기준 `DOUBLE` 저장과 표시 단위 변환이
  DSL 및 DAO 테스트로 검증된다.
- [x] nullable 왕복, 소수/큰 값의 허용 오차, 절대온도/온도차 구분,
  서로 다른 단위 계열의 compile-time 경계가 테스트 또는 문서로 고정된다.
- [x] 기존 모듈/스키마/커스텀 컬럼과 R2DBC 범위를 변경하지 않는다.
- [x] English/Korean module README와 architecture/ERD SVG·PNG가
  source-equivalent이며 기준 단위·`DOUBLE` 한계·migration 주의를 포함한다.
- [x] `06-advanced` root README 양쪽 언어의 모듈 목록과 실행 명령이 일치한다.
- [x] module test, detekt/static check, docs/diagram audit, workflow registration,
  six-lens review, Korean lesson이 완료된다.
- [ ] Issue/PR metadata는 live read-back하고 PR 본문 마지막에 `## DoD Status`를
  둔다. merge-ready 보고는 exact head·CI·review/thread 확인 이후에만 한다.

## SPW writer gate

- [x] **SPW-01** — 독자, 목표, JDBC-only/R2DBC 제외 범위와 issue/provider/catalog
  근거를 명시했다.
- [x] **SPW-02** — 책임 경계, 선택지와 기각 사유, 데이터 흐름, API/테스트,
  실패 모드, 호환성·rollback, 문서·다이어그램, 수용 기준을 포함했다.
- [x] **SPW-03** — work document/KDoc 기준에 맞춰 한국어 prose를 사용하고
  code/API/identifier/command/URL/version은 원문을 보존했다.
- [x] **SPW-04** — local catalog/module pattern과 provider `MeasuredColumnTypes.kt`,
  `Units.kt`, `Temperature.kt` source를 대조했다.
- [x] **SPW-05** — Markdown 구조와 한국어 용어 감사 후 Step 2-R 여섯 렌즈
  검토를 수행한다.

## 설계 상태

`APPROVED-DESIGN / APPROVED` — 사용자 승인(2026-08-27)을 받은 선택지 A를
기준으로 작성했고 Step 2-R 통합 검토(P0=0/P1=0)를 완료했다. 승인된 설계를
implementation plan과 TDD 실행의 기준으로 사용한다.
