# 04 Exposed DDL: 스키마 정의 언어 (DDL)

이 모듈(
`02-ddl`)은 Exposed의 DDL(Data Definition Language) 기능을 단계별로 학습합니다. 테이블, 컬럼, 인덱스, 시퀀스 등 데이터베이스 스키마를 프로그래밍 방식으로 정의하고 관리하는 방법을 다룹니다.

## 학습 목표

- Exposed DSL로 테이블과 컬럼을 정의하는 방법 이해
- 다양한 컬럼 타입과 제약 조건 설정 방법 학습
- 인덱스 생성을 통한 쿼리 성능 최적화 기법 습득
- 시퀀스를 사용한 고유 값 생성 방법 익히기
- 커스텀 열거형 타입 정의 및 활용 방법 이해

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/ddl` 아래에 있습니다.

### 테이블 및 스키마 생성

| 파일                                     | 설명                  | 핵심 기능                                         |
|----------------------------------------|---------------------|-----------------------------------------------|
| `Ex01_CreateDatabase.kt`               | 데이터베이스 생성 (지원되는 경우) | `CREATE DATABASE`                             |
| `Ex02_CreateTable.kt`                  | 테이블 생성 기본           | `Table`, 기본키, 컬럼 정의                           |
| `Ex03_CreateMissingTableAndColumns.kt` | 누락된 테이블/컬럼 자동 생성    | `SchemaUtils.createMissingTablesAndColumns()` |

### 컬럼 정의 및 제약 조건

| 파일                         | 설명               | 핵심 기능                              |
|----------------------------|------------------|------------------------------------|
| `Ex04_ColumnDefinition.kt` | 컬럼 타입 및 제약 조건 정의 | `nullable()`, `uniqueIndex()`, 기본값 |

### 인덱스 및 시퀀스

| 파일                    | 설명          | 핵심 기능                   |
|-----------------------|-------------|-------------------------|
| `Ex05_CreateIndex.kt` | 인덱스 생성 및 관리 | 단일/복합 인덱스, 고유 인덱스       |
| `Ex06_Sequence.kt`    | 시퀀스 정의 및 사용 | `Sequence`, `nextVal()` |

### 고급 기능

| 파일                          | 설명            | 핵심 기능                 |
|-----------------------------|---------------|-----------------------|
| `Ex07_CustomEnumeration.kt` | 커스텀 열거형 타입 정의 | `customEnumeration()` |
| `Ex10_DDL_Examples.kt`      | 종합 DDL 예제     | 복합 스키마 정의             |

## 핵심 개념

### 1. 테이블 정의 기본

```kotlin
// 기본 테이블 정의
object Users: Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val age = integer("age").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

// IntIdTable 사용 (자동 증가 기본키)
object Products: IntIdTable("products") {
    val name = varchar("name", 255)
    val price = decimal("price", 10, 2)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
```

### 2. 컬럼 타입 및 제약 조건

```kotlin
object SampleTable: Table("sample") {
    // 기본 타입
    val intColumn = integer("int_column")
    val longColumn = long("long_column")
    val stringColumn = varchar("string_column", 255)
    val textColumn = text("text_column")
    val boolColumn = bool("bool_column")
    
    // 숫자 타입
    val decimalColumn = decimal("decimal_column", 10, 2)
    val doubleColumn = double("double_column")
    
    // 날짜/시간 타입
    val dateColumn = date("date_column")
    val datetimeColumn = datetime("datetime_column")
    val timestampColumn = timestamp("timestamp_column")
    
    // 제약 조건
    val nullableColumn = varchar("nullable_column", 100).nullable()
    val uniqueColumn = varchar("unique_column", 100).uniqueIndex()
    val defaultColumn = integer("default_column").default(0)
    val clientDefaultColumn = varchar("client_default", 100).clientDefault { "default_value" }
}
```

### 3. 인덱스 생성

```kotlin
object IndexedTable: Table("indexed_table") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val createdAt = datetime("created_at")
    
    // 단일 컬럼 인덱스
    val nameIndex = index("idx_name", false, name)
    
    // 복합 인덱스
    init {
        index("idx_email_created", false, email, createdAt)
        uniqueIndex("idx_unique_email", email)
    }
}
```

### 4. 시퀀스 사용

```kotlin
// 시퀀스 정의
object MySequence: Sequence("my_sequence") {
    val startWith = 1L
    val incrementBy = 1L
    val minValue = 1L
    val maxValue = Long.MAX_VALUE
}

// 시퀀스 사용
val nextId = MySequence.nextVal()
```

### 5. 커스텀 열거형

```kotlin
enum class UserStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

object CustomEnumTable: Table("custom_enum_table") {
    val status = enumerationByName("status", 20, UserStatus::class)
    // 또는 커스텀 변환
    val customStatus = customEnumeration(
        "custom_status",
        "VARCHAR(20)",
        { value -> UserStatus.valueOf(value as String) },
        { it.name }
    )
}
```

### 6. 스키마 동기화

```kotlin
// 테이블 생성
SchemaUtils.create(Users, Products)

// 누락된 테이블과 컬럼만 생성
SchemaUtils.createMissingTablesAndColumns(Users, Products)

// 테이블 삭제
SchemaUtils.drop(Users, Products)

// 테이블 존재 확인
SchemaUtils.tableExists(Users)
```

## 데이터베이스별 차이점

| 기능             | H2     | MySQL | PostgreSQL |
|----------------|--------|-------|------------|
| AUTO_INCREMENT | 지원     | 지원    | SERIAL 사용  |
| 시퀀스            | 지원     | 미지원   | 지원         |
| JSON 타입        | 지원(제한) | 지원    | 지원 (JSONB) |
| 배열 타입          | 지원     | 미지원   | 지원         |

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :04-exposed-ddl:02-ddl:test

# 특정 테스트만 실행
./gradlew :04-exposed-ddl:02-ddl:test --tests "exposed.examples.ddl.Ex02_CreateTable"
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.

## 더 읽어보기

- [6.2 DDL](https://debop.notion.site/1ad2744526b080c5b15cc5b9a53c44ce?v=1ad2744526b080c1e839000ce56f3744)
- [Exposed Wiki: Schema Definition](https://github.com/JetBrains/Exposed/wiki/Schema-Definition)
