# Exposed-Java-Time: 현대적인 날짜와 시간 지원

이 모듈은 `exposed-java-time` 확장을 사용하여 현대적인 Java 8
`java.time` (JSR-310) API와 Exposed를 통합하는 방법을 단계별로 학습합니다. 이는 Exposed에서 날짜와 시간을 처리하는 표준적이고 권장되는 방법입니다.

## 학습 목표

- `LocalDate`, `LocalDateTime`, `Instant`, `OffsetDateTime` 등 `java.time` 타입 매핑 이해
- 날짜/시간 관련 SQL 함수(`year()`, `month()`, `day()`) 활용 방법 학습
- 서버 측 기본값(`CURRENT_TIMESTAMP`) 설정 기법 습득
- `WHERE` 절에서 날짜/시간 리터럴 올바르게 사용하는 방법 익히기
- 데이터베이스별 날짜/시간 처리 차이점 이해

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/java/time` 아래에 있습니다.

| 파일                        | 설명          | 핵심 기능                                |
|---------------------------|-------------|--------------------------------------|
| `Ex01_JavaTime.kt`        | 기본 사용법 및 함수 | 날짜 추출 함수, 타임존 처리                     |
| `Ex02_Defaults.kt`        | 기본값 설정      | `clientDefault`, `defaultExpression` |
| `Ex03_DateTimeLiteral.kt` | 리터럴을 사용한 쿼리 | `dateLiteral()`, `dateTimeLiteral()` |
| `Ex04_MiscTable.kt`       | 종합 통합 테스트   | 모든 날짜/시간 타입                          |

## 지원 컬럼 타입

| Exposed 함수                    | Java 타입          | 데이터베이스 타입          |
|-------------------------------|------------------|--------------------|
| `date(name)`                  | `LocalDate`      | DATE               |
| `time(name)`                  | `LocalTime`      | TIME               |
| `datetime(name)`              | `LocalDateTime`  | TIMESTAMP/DATETIME |
| `timestamp(name)`             | `Instant`        | TIMESTAMP          |
| `timestampWithTimeZone(name)` | `OffsetDateTime` | TIMESTAMPTZ        |
| `duration(name)`              | `Duration`       | BIGINT             |

## 핵심 개념

### 1. 날짜/시간 컬럼 정의

```kotlin
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.time
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object Events : IntIdTable("events") {
    val name = varchar("name", 255)
    
    // 날짜
    val eventDate = date("event_date")
    
    // 시간
    val startTime = time("start_time")
    
    // 날짜/시간
    val createdAt = datetime("created_at")
    
    // 타임스탬프 (UTC)
    val timestamp = timestamp("timestamp")
    
    // 타임존 포함 타임스탬프
    val zonedTime = timestampWithTimeZone("zoned_time")
    
    // 기간
    val duration = duration("duration")
}
```

### 2. 날짜/시간 함수 사용

```kotlin
import org.jetbrains.exposed.v1.javatime.year
import org.jetbrains.exposed.v1.javatime.month
import org.jetbrains.exposed.v1.javatime.day
import org.jetbrains.exposed.v1.javatime.hour
import org.jetbrains.exposed.v1.javatime.minute
import org.jetbrains.exposed.v1.javatime.second

transaction {
    // 날짜 부분 추출
    val query = Events.select(
        Events.createdAt.year(),
        Events.createdAt.month(),
        Events.createdAt.day(),
        Events.createdAt.hour(),
        Events.createdAt.minute()
    )
    
    // 조건에 사용
    Events.select { Events.createdAt.year() eq 2024 }
    Events.select { Events.createdAt.month() eq Month.JANUARY }
    Events.select { Events.createdAt.day() eq 15 }
    
    // 날짜 비교
    Events.select { Events.eventDate eq LocalDate.of(2024, 1, 15) }
    Events.select { Events.createdAt greaterEq LocalDateTime.now().minusDays(7) }
}
```

### 3. 기본값 설정

```kotlin
import org.jetbrains.exposed.v1.javatime.CurrentDate
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp

object Orders : IntIdTable("orders") {
    // 클라이언트 측 기본값 (앱에서 생성)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    
    // 서버 측 기본값 (DB에서 생성)
    val orderDate = date("order_date").defaultExpression(CurrentDate)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    
    // 상수 기본값
    val status = varchar("status", 20).default("PENDING")
}
```

### 4. 리터럴을 사용한 쿼리

```kotlin
import org.jetbrains.exposed.v1.javatime.dateLiteral
import org.jetbrains.exposed.v1.javatime.dateTimeLiteral
import org.jetbrains.exposed.v1.javatime.timestampLiteral

transaction {
    // 날짜 리터럴
    val date = LocalDate.of(2024, 1, 15)
    Events.select { Events.eventDate eq dateLiteral(date) }
    
    // 날짜시간 리터럴
    val dateTime = LocalDateTime.of(2024, 1, 15, 10, 30)
    Events.select { Events.createdAt eq dateTimeLiteral(dateTime) }
    
    // 타임스탬프 리터럴
    val instant = Instant.parse("2024-01-15T10:30:00Z")
    Events.select { Events.timestamp eq timestampLiteral(instant) }
    
    // 범위 쿼리
    Events.select { 
        Events.createdAt between 
            dateTimeLiteral(LocalDateTime.of(2024, 1, 1, 0, 0)) and
            dateTimeLiteral(LocalDateTime.of(2024, 1, 31, 23, 59))
    }
}
```

### 5. Entity에서 사용

```kotlin
object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val birthDate = date("birth_date").nullable()
    val lastLoginAt = datetime("last_login_at").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)
    
    var name by Users.name
    var birthDate by Users.birthDate
    var lastLoginAt by Users.lastLoginAt
    val createdAt by Users.createdAt
}

// 사용법
transaction {
    val user = User.new {
        name = "John Doe"
        birthDate = LocalDate.of(1990, 5, 15)
        lastLoginAt = LocalDateTime.now()
    }
    
    // 나이 계산
    val age = Period.between(user.birthDate, LocalDate.now()).years
}
```

### 6. 날짜 연산

```kotlin
transaction {
    // 날짜 더하기/빼기
    Events.select { Events.eventDate plusEq 7.days }  // 7일 후
    Events.select { Events.eventDate minusEq 1.month }  // 1달 전
    
    // 날짜 차이
    val diff = Events.eventDate.minus(OtherEvents.eventDate)
    
    // between
    Events.select { 
        Events.eventDate between LocalDate.of(2024, 1, 1) and LocalDate.of(2024, 12, 31) 
    }
}
```

## 데이터베이스별 차이점

| 기능                      | H2        | MySQL     | PostgreSQL  |
|-------------------------|-----------|-----------|-------------|
| `datetime`              | TIMESTAMP | DATETIME  | TIMESTAMP   |
| `timestamp`             | TIMESTAMP | TIMESTAMP | TIMESTAMP   |
| `timestampWithTimeZone` | TIMESTAMP | TIMESTAMP | TIMESTAMPTZ |
| 나노초 정밀도                 | 지원        | 지원 (6자리)  | 지원          |
| `CURRENT_TIMESTAMP`     | 지원        | 지원        | 지원          |

## 실무 패턴

### 1. 감사(Auditing) 필드

```kotlin
abstract class AuditableTable(name: String) : IntIdTable(name) {
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    val createdBy = varchar("created_by", 100).nullable()
    val updatedBy = varchar("updated_by", 100).nullable()
}
```

### 2. 기간 검색

```kotlin
fun findEventsByMonth(year: Int, month: Month): List<Event> {
    val start = LocalDate.of(year, month, 1)
    val end = start.plusMonths(1).minusDays(1)
    
    return Event.find {
        Events.eventDate between dateLiteral(start) and dateLiteral(end)
    }.toList()
}
```

### 3. 만료 데이터 처리

```kotlin
object Sessions : IntIdTable("sessions") {
    val userId = reference("user_id", Users)
    val expiresAt = datetime("expires_at")
}

fun cleanExpiredSessions() {
    Sessions.deleteWhere { 
        Sessions.expiresAt less dateTimeLiteral(LocalDateTime.now()) 
    }
}
```

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :06-advanced:02-exposed-javatime:test

# 특정 테스트만 실행
./gradlew :06-advanced:02-exposed-javatime:test --tests "exposed.examples.java.time.Ex01_JavaTime"
```

## 더 읽어보기

- [Exposed Java Time Module](https://debop.notion.site/Exposed-Java-Time-1c32744526b0809d85e1d0425038dfdd)
- [kotlinx-datetime 버전](../03-exposed-kotlin-datetime/README.md)
