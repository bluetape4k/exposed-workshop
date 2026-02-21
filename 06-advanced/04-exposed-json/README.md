# Exposed-Json: JSON/JSONB 데이터 저장 및 조회

이 모듈은 `exposed-json` 확장을 사용하여 `@Serializable` Kotlin 클래스를 네이티브 데이터베이스 `JSON`과
`JSONB` 컬럼에 매핑하는 방법을 단계별로 학습합니다. 관계형 데이터베이스 내에 복잡하고 스키마 없는 데이터를 직접 저장하고, 강력한 JSON 쿼리 함수를 활용합니다.

## 학습 목표

- `JSON`과 `JSONB` 컬럼 타입 정의 방법 이해
- 복잡하고 중첩된 Kotlin 객체를 단일 컬럼에 저장 및 조회
- `.extract<T>()` 함수로 JSON 필드 쿼리
- `.contains()`와 `.exists()`로 JSON 데이터 존재 여부 확인
- DSL과 DAO 스타일 모두에 JSON 컬럼 적용

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/json` 아래에 있습니다.

| 파일                    | 설명                   | 핵심 기능                           |
|-----------------------|----------------------|---------------------------------|
| `JsonTestData.kt`     | 공통 데이터 클래스 및 테이블     | `@Serializable` 클래스, `Table` 정의 |
| `Ex01_JsonColumn.kt`  | JSON 컬럼 (DSL & DAO)  | `json<T>()`, 기본 CRUD, 쿼리        |
| `Ex02_JsonBColumn.kt` | JSONB 컬럼 (DSL & DAO) | `jsonb<T>()`, 고급 쿼리, 인덱싱        |

## JSON vs JSONB 비교

| 구분     | JSON         | JSONB          |
|--------|--------------|----------------|
| 저장 방식  | 일반 텍스트       | 바이너리 (분해된 형태)  |
| 쓰기 속도  | 빠름           | 느림 (변환 오버헤드)   |
| 조회 속도  | 느림           | 빠름             |
| 인덱싱    | 미지원          | 지원             |
| 공백 보존  | 보존           | 제거             |
| 권장 사용처 | 로그 저장, 단순 조회 | 빈번한 쿼리, 복잡한 검색 |

## 핵심 개념

### 1. 데이터 클래스 정의

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    val team: String?
)

@Serializable
data class DataHolder(
    val user: User,
    val logins: Int,
    val active: Boolean
)

@Serializable
data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)
```

### 2. JSON/JSONB 컬럼 정의

```kotlin
import org.jetbrains.exposed.v1.json.json
import org.jetbrains.exposed.v1.json.jsonb
import kotlinx.serialization.json.Json

object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    
    // JSON 컬럼 (텍스트)
    val metadata = json<Map<String, String>>("metadata", Json.Default)
    
    // JSONB 컬럼 (바이너리)
    val profile = jsonb<DataHolder>("profile", Json.Default)
    
    // Nullable JSONB
    val settings = jsonb<UserSettings?>("settings", Json.Default).nullable()
}
```

### 3. 기본 CRUD 연산

```kotlin
// 삽입
Users.insert {
    it[name] = "John"
    it[metadata] = mapOf("role" to "admin", "department" to "IT")
    it[profile] = DataHolder(
        user = User("John Doe", "Team A"),
        logins = 15,
        active = true
    )
}

// 조회
val user = Users.selectAll().where { Users.id eq 1 }.single()
val profile = user[Users.profile]  // DataHolder 객체로 자동 역직렬화

// 수정
Users.update({ Users.id eq 1 }) {
    it[profile] = profile.copy(logins = profile.logins + 1)
}
```

### 4. JSON 쿼리 함수

#### extract() - 값 추출

```kotlin
// 중첩된 값 추출
val userName = Users.profile.extract<String>(".user.name", toScalar = true)

// 추출된 값으로 쿼리
Users.selectAll().where { userName eq "John Doe" }

// 숫자 추출
val logins = Users.profile.extract<Int>(".logins", toScalar = true)
Users.selectAll().where { logins greater 10 }
```

#### contains() - 포함 여부 확인

```kotlin
// JSON 값 포함 확인
val hasActiveFlag = Users.profile.contains("""{"active":true}""")
Users.selectAll().where { hasActiveFlag }

// 경로 지정
val hasTeamA = Users.profile.contains("""{"team":"Team A"}""", ".user")
Users.selectAll().where { hasTeamA }
```

#### exists() - 존재 여부 확인

```kotlin
// 필드 존재 확인
val hasTeam = Users.profile.exists(".user.team")
Users.selectAll().where { hasTeam }

// 선택적 존재 확인
val hasOptionalField = Users.profile.exists(".optional_field", optional = true)
```

### 5. DAO와 함께 사용

```kotlin
object Products : IntIdTable("products") {
    val name = varchar("name", 255)
    val attributes = jsonb<ProductAttributes>("attributes", Json.Default)
}

class Product(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Product>(Products)
    
    var name by Products.name
    var attributes by Products.attributes
}

// 사용법
transaction {
    val product = Product.new {
        name = "Laptop"
        attributes = ProductAttributes(
            color = "Silver",
            weight = 1.5,
            specs = mapOf("ram" to "16GB", "storage" to "512GB")
        )
    }
    
    // 조회
    val found = Product.findById(product.id)
    println(found?.attributes?.color)  // "Silver"
    
    // 수정
    product.attributes = product.attributes.copy(color = "Black")
}
```

### 6. 컬렉션 타입

```kotlin
@Serializable
data class Order(
    val items: List<OrderItem>,
    val tags: Set<String>
)

@Serializable
data class OrderItem(
    val productId: Long,
    val quantity: Int,
    val price: Double
)

object Orders : IntIdTable("orders") {
    val data = jsonb<Order>("data", Json.Default)
}

// 사용법
transaction {
    val order = Order(
        items = listOf(
            OrderItem(1, 2, 29.99),
            OrderItem(2, 1, 49.99)
        ),
        tags = setOf("urgent", "gift")
    )
    
    Orders.insert { it[data] = order }
}
```

## 데이터베이스별 지원

| 기능         | PostgreSQL | MySQL | H2      |
|------------|------------|-------|---------|
| JSON 타입    | 지원         | 지원    | 지원 (제한) |
| JSONB 타입   | 지원         | 미지원   | 미지원     |
| 인덱싱        | 지원 (GIN)   | 지원    | 미지원     |
| extract()  | 지원         | 지원    | 제한적     |
| contains() | 지원 (`@>`)  | 지원    | 미지원     |

## 인덱스 생성 (PostgreSQL)

```kotlin
object Users : IntIdTable("users") {
    val profile = jsonb<DataHolder>("profile", Json.Default)
    
    init {
        // GIN 인덱스 생성
        index(isUnique = false, profile)
    }
}
```

## 성능 최적화 팁

1. **JSONB 사용**: 조회가 많은 경우 JSONB 사용
2. **인덱스 활용**: 자주 조회하는 JSON 필드에 인덱스 생성
3. **적절한 크기**: 큰 JSON 문서는 별도 테이블로 분리 고려
4. **부분 조회**: 필요한 필드만 `extract()`로 조회

## 테스트 실행

```bash
# 전체 테스트 실행 (PostgreSQL 권장)
./gradlew :06-advanced:04-exposed-json:test

# 특정 테스트만 실행
./gradlew :06-advanced:04-exposed-json:test --tests "exposed.examples.json.Ex02_JsonBColumn"
```

## 더 읽어보기

- [Exposed Json](https://debop.notion.site/Exposed-Json-1c32744526b080a9bee3d7b92463e90c)
- [Jackson 버전](../08-exposed-jackson/README.md)
- [Fastjson2 버전](../09-exposed-fastjson2/README.md)
