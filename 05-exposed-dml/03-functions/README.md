# 05 Exposed DML: SQL 함수

이 모듈(`03-functions`)은 Exposed 쿼리 내에서 다양한 SQL 함수를 활용하는 방법을 단계별로 학습합니다. 일반 함수부터 윈도우 함수까지 실무에서 자주 사용하는 함수들을 다룹니다.

## 학습 목표

- Exposed에서 SQL 함수를 사용하는 기본 패턴 이해
- 문자열, 수학, 통계 함수 활용 방법 습득
- 날짜/시간 함수를 사용한 데이터 처리 기법 익히기
- 윈도우 함수를 통한 고급 분석 쿼리 작성 방법 학습

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/functions` 아래에 있습니다.

| 파일                                | 설명              | 핵심 함수                                         |
|-----------------------------------|-----------------|-----------------------------------------------|
| `Ex00_FunctionBase.kt`            | 공통 테스트 데이터 및 설정 | 테이블 정의, 샘플 데이터                                |
| `Ex01_Functions.kt`               | 일반 SQL 함수       | `trim()`, `lower()`, `upper()`, `substring()` |
| `Ex02_MathFunction.kt`            | 수학 함수           | `abs()`, `round()`, `ceil()`, `floor()`       |
| `Ex03_StatisticsFunction.kt`      | 통계/집계 함수        | `count()`, `sum()`, `avg()`, `min()`, `max()` |
| `Ex04_TrigonometricalFunction.kt` | 삼각 함수           | `sin()`, `cos()`, `tan()`, `asin()`           |
| `Ex05_WindowFunction.kt`          | 윈도우 함수          | `rowNumber()`, `rank()`, `lead()`, `lag()`    |

## 핵심 개념

### 1. 문자열 함수

```kotlin
// 문자열 조작
val query = Users.select(
    Users.name.trim(),
    Users.name.lower(),
    Users.name.upper(),
    Users.name.substring(1, 10)
)

// 문자열 연결
val fullName = Users.firstName + " " + Users.lastName

// LIKE 검색
Users.select { Users.name like "%kim%" }
Users.select { Users.name ilike "%KIM%" }  // 대소문자 무시
```

### 2. 수학 함수

```kotlin
// 기본 수학 함수
val query = Products.select(
    Products.price.abs(),
    Products.price.round(2),
    Products.price.ceil(),
    Products.price.floor()
)

// 산술 연산
val discounted = Products.price * 0.9
val total = Products.price + Products.tax
```

### 3. 집계/통계 함수

```kotlin
// 기본 집계
val count = Users.selectAll().count()
val totalPrice = Orders.select(Orders.price.sum()).single()
val avgAge = Users.select(Users.age.avg()).single()
val (minPrice, maxPrice) = Products.select(
    Products.price.min(),
    Products.price.max()
).single()

// GROUP BY와 함께 사용
Users
    .groupBy(Users.cityId)
    .select(Users.cityId, Users.id.count())
    .having { Users.id.count() greater 5L }
```

### 4. 날짜/시간 함수

```kotlin
// 날짜 부분 추출
val query = Orders.select(
    Orders.orderDate.year(),
    Orders.orderDate.month(),
    Orders.orderDate.day(),
    Orders.orderDate.hour()
)

// 날짜 비교
Orders.select { Orders.orderDate.date() eq LocalDate.of(2024, 1, 1) }

// 현재 날짜/시간
Orders.select(CurrentDate, CurrentDateTime, CurrentTimestamp)
```

### 5. 윈도우 함수

```kotlin
// ROW_NUMBER
val ranked = Products
    .select(
        Products.name,
        Products.price,
        Products.id.rowNumber().over().orderBy(Products.price, SortOrder.DESC)
    )

// RANK와 DENSE_RANK
val ranked = Sales
    .select(
        Sales.employeeId,
        Sales.amount,
        Sales.amount.rank().over().orderBy(Sales.amount, SortOrder.DESC),
        Sales.amount.denseRank().over().orderBy(Sales.amount, SortOrder.DESC)
    )

// LEAD와 LAG (이전/다음 값)
val withLag = DailySales
    .select(
        DailySales.date,
        DailySales.amount,
        DailySales.amount.lag(1).over().orderBy(DailySales.date),
        DailySales.amount.lead(1).over().orderBy(DailySales.date)
    )

// 파티션별 집계
val byCategory = Products
    .select(
        Products.categoryId,
        Products.name,
        Products.price,
        Products.price.sum().over().partitionBy(Products.categoryId)
    )
```

### 6. 조건부 표현식

```kotlin
// CASE WHEN
val category = Cases(
    When(Products.price less 1000.0, stringLiteral("저가")),
    When(Products.price between 1000.0 and 10000.0, stringLiteral("중가")),
    Else(stringLiteral("고가"))
)

// COALESCE
val value = Users.nickname.coalesce(Users.name, stringLiteral("Unknown"))

// NULLIF
val result = Users.score.nullif(0)
```

## 함수 카테고리별 요약

### 문자열 함수

| 함수            | 설명     | 예시                           |
|---------------|--------|------------------------------|
| `trim()`      | 공백 제거  | `name.trim()`                |
| `lower()`     | 소문자 변환 | `name.lower()`               |
| `upper()`     | 대문자 변환 | `name.upper()`               |
| `substring()` | 부분 문자열 | `name.substring(1, 10)`      |
| `length()`    | 문자열 길이 | `name.length()`              |
| `concat()`    | 문자열 연결 | `firstName + " " + lastName` |

### 수학 함수

| 함수        | 설명   | 예시               |
|-----------|------|------------------|
| `abs()`   | 절대값  | `price.abs()`    |
| `round()` | 반올림  | `price.round(2)` |
| `ceil()`  | 올림   | `price.ceil()`   |
| `floor()` | 내림   | `price.floor()`  |
| `sqrt()`  | 제곱근  | `value.sqrt()`   |
| `power()` | 거듭제곱 | `value.power(2)` |

### 집계 함수

| 함수        | 설명  | 예시            |
|-----------|-----|---------------|
| `count()` | 행 수 | `id.count()`  |
| `sum()`   | 합계  | `price.sum()` |
| `avg()`   | 평균  | `age.avg()`   |
| `min()`   | 최소값 | `price.min()` |
| `max()`   | 최대값 | `price.max()` |

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :05-exposed-dml:03-functions:test

# 특정 테스트만 실행
./gradlew :05-exposed-dml:03-functions:test --tests "exposed.examples.functions.Ex05_WindowFunction"
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.

## 더 읽어보기

- [7.3 Functions](https://debop.notion.site/1ca2744526b0805e9689efa4a03d01df?v=1ca2744526b08138857a000c9847c052)
- [Exposed Wiki: DSL](https://github.com/JetBrains/Exposed/wiki/DSL)
