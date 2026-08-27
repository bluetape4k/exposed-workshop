# Exposed 측정값 컬럼

[English](./README.md) | 한국어

JDBC 기반 Exposed 테이블에서 `bluetape4k-exposed-measured` provider를 사용하여
타입이 있는 측정값을 저장하는 예제입니다. `Length`, `Mass`, 절대 온도인
`Temperature`를 provider가 정한 기준 단위의 `DOUBLE` 값으로 저장하고, 조회 시
타입이 있는 측정값으로 복원합니다. 이 저장소에서는 R2DBC를 실행하지 않습니다.

## 이 예제에서 다루는 내용

- DSL 컬럼: `length("length")`, `mass("mass")`, `temperature("temperature")`.
- `IntEntity`와 `EntityClass`를 사용하여 같은 컬럼을 DAO 속성으로 접근하는 방법.
- `150.centimeters()`, `77.fahrenheit()`처럼 입력 단위를 변환하고 조회 시
  기준 단위로 변환하는 방법.
- 대표 표시 단위인 센티미터·미터·킬로미터, 그램·킬로그램,
  섭씨·화씨·켈빈을 사용하는 방법.
- nullable 측정값과 명시적인 허용 오차를 적용한 부동소수점 비교.
- provider가 호환되지 않는 DB 값 타입을 거부하는 경계.

## 아키텍처

![측정값 Exposed JDBC 아키텍처](../../../docs/images/readme-diagrams/06-advanced-13-exposed-measured-architecture-01.ko.png)

![측정값 상품 ERD](../../../docs/images/readme-diagrams/06-advanced-13-exposed-measured-erd-01.ko.png)

물리 스키마는 의도적으로 작게 유지합니다.

| 컬럼 | Kotlin 값 | 물리 타입 | 저장 기준 단위 |
| --- | --- | --- | --- |
| `length` | `Measure<Length>` | `DOUBLE` | 미터 (`m`) |
| `mass` | `Measure<Mass>` | `DOUBLE` | 킬로그램 (`kg`) |
| `nullable_mass` | `Measure<Mass>?` | `DOUBLE NULL` | 킬로그램 (`kg`) |
| `temperature` | `Temperature` | `DOUBLE` | 켈빈 (`K`) |

provider의 기준 단위를 바꾸는 일은 스키마와 마이그레이션 결정입니다. 이 예제는
기존 테이블을 변경하지 않고, 측정 정확도 정책이나 통화/측정 통합 추상화도
정의하지 않습니다. R2DBC는 의도적으로 범위에서 제외했으며
`exposed-r2dbc-workshop` 저장소에서 다룹니다.

## DSL과 DAO 사용

```kotlin
internal object ProductTable: IntIdTable("measured_products") {
    val length = length("length")
    val mass = mass("mass")
    val nullableMass = mass("nullable_mass").nullable()
    val temperature = temperature("temperature")
}

internal class ProductEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: EntityClass<Int, ProductEntity>(ProductTable)

    var length by ProductTable.length
    var mass by ProductTable.mass
    var nullableMass by ProductTable.nullableMass
    var temperature by ProductTable.temperature
}

transaction {
    ProductTable.insert {
        it[length] = 150.centimeters()
        it[mass] = 2.5.kilograms()
        it[temperature] = 77.fahrenheit()
    }
}
```

provider는 `1.kilometers()`, `500.grams()`, `273.15.kelvin()`도 지원합니다.
각 입력값은 JDBC에 기록하기 전에 컬럼의 기준 단위로 정규화됩니다.

제네릭 `Measure<T>` 경계는 컴파일 시 질량 값을 길이 컬럼에 대입하는 실수를
막습니다. 조회 시에는 원시 `DOUBLE` 표현을 직접 비교하지 말고 원하는 단위로
변환하여 비교합니다.

```kotlin
(row[ProductTable.length] `in` Length.meters).shouldBeNear(1.5, 1e-10)
row[ProductTable.temperature].inCelsius().shouldBeNear(25.0, 1e-10)
```

다음 주석 처리한 대입은 의도적으로 잘못된 코드이며 컴파일 오류로 남아야
합니다. `Measure<Mass>`는 `Measure<Length>` 컬럼에 기록할 수 없습니다.

```kotlin
val lengthValue: Measure<Length> = 1.kilometers()
val massValue: Measure<Mass> = 500.grams()

ProductTable.insert {
    it[ProductTable.length] = lengthValue
    // it[ProductTable.length] = massValue // 컴파일 오류: Measure<Mass>는 Measure<Length>가 아님
}
```

## 테스트

```bash
./gradlew :13-exposed-measured:test
./gradlew :13-exposed-measured:test -PuseFastDB=true
```

`Ex01MeasuredColumns`는 공용 `TestDB` 매트릭스를 통해 JDBC 왕복, DAO nullable,
정밀도, 호환되지 않는 값 타입 거부 사례를 실행합니다.
