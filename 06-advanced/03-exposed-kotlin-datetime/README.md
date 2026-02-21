# Exposed-Kotlin-Datetime: kotlinx.datetime 지원

이 모듈은 `exposed-kotlin-datetime` 확장을 사용하여
`kotlinx.datetime` 라이브러리와 Exposed를 통합하는 방법을 단계별로 학습합니다. 이는 멀티플랫폼 Kotlin 프로젝트에서 날짜와 시간을 처리하는 권장 방식입니다.

## 학습 목표

- `kotlinx.datetime` 타입(`LocalDate`, `LocalDateTime`, `Instant`) 매핑 이해
- 날짜/시간 관련 SQL 함수 활용 방법 학습
- 서버 측 기본값 설정 기법 습득
- `WHERE` 절에서 날짜/시간 리터럴 올바르게 사용하는 방법 익히기
- `java.time` 모듈과의 차이점 이해

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/kotlin/datetime` 아래에 있습니다.

| 파일                        | 설명          | 핵심 기능                                |
|---------------------------|-------------|--------------------------------------|
| `Ex01_KotlinDateTime.kt`  | 기본 사용법 및 함수 | 날짜 추출 함수, 타임존 처리                     |
| `Ex02_Defaults.kt`        | 기본값 설정      | `clientDefault`, `defaultExpression` |
| `Ex03_DateTimeLiteral.kt` | 리터럴을 사용한 쿼리 | `dateLiteral()`, `dateTimeLiteral()` |

## java.time vs kotlinx.datetime 비교

| 구분            | java.time (exposed-java-time) | kotlinx.datetime (exposed-kotlin-datetime) |
|---------------|-------------------------------|--------------------------------------------|
| 플랫폼           | JVM 전용                        | 멀티플랫폼 (JVM, JS, Native)                    |
| LocalDateTime | `java.time.LocalDateTime`     | `kotlinx.datetime.LocalDateTime`           |
| Instant       | `java.time.Instant`           | `kotlinx.datetime.Instant`                 |
| 날짜 생성         | `LocalDate.of(2024, 1, 15)`   | `LocalDate(2024, 1, 15)`                   |
| 현재 시간         | `LocalDateTime.now()`         | `Clock.System.now().toLocalDateTime(...)`  |
| 권장 사용처        | 서버 전용 애플리케이션                  | KMP 프로젝트, 공유 코드                            |

## 지원 컬럼 타입

| Exposed 함수                    | Kotlin 타입                        | 데이터베이스 타입          |
|-------------------------------|----------------------------------|--------------------|
| `date(name)`                  | `kotlinx.datetime.LocalDate`     | DATE               |
| `time(name)`                  | `kotlinx.datetime.LocalTime`     | TIME               |
| `datetime(name)`              | `kotlinx.datetime.LocalDateTime` | TIMESTAMP/DATETIME |
| `timestamp(name)`             | `kotlinx.datetime.Instant`       | TIMESTAMP          |
| `timestampWithTimeZone(name)` | `java.time.OffsetDateTime`       | TIMESTAMPTZ        |
| `duration(name)`              | `kotlin.time.Duration`           | BIGINT             |

## 핵심 개념

### 1. 날짜/시간 컬럼 정의

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.kotlindatetime.date
import org.jetbrains.exposed.v1.kotlindatetime.time
import org.jetbrains.exposed.v1.kotlindatetime.datetime
import org.jetbrains.exposed.v1.kotlindatetime.timestamp

object Events : IntIdTable("events") {
    val name = varchar("name", 255)
    val eventDate = date("event_date")
    val startTime = time("start_time")
    val createdAt = datetime("created_at")
    val timestamp = timestamp("timestamp")
}
```

### 2. 날짜/시간 함수 사용

```kotlin
import org.jetbrains.exposed.v1.kotlindatetime.year
import org.jetbrains.exposed.v1.kotlindatetime.month
import org.jetbrains.exposed.v1.kotlindatetime.day
import org.jetbrains.exposed.v1.kotlindatetime.hour

transaction {
    // 날짜 부분 추출
    Events.select(
        Events.createdAt.year(),
        Events.createdAt.month(),
        Events.createdAt.day()
    )
    
    // 조건에 사용
    Events.select { Events.createdAt.year() eq 2024 }
    Events.select { Events.createdAt.month() eq Month.JANUARY }
}
```

### 3. 기본값 설정

```kotlin
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.kotlindatetime.CurrentDate
import org.jetbrains.exposed.v1.kotlindatetime.CurrentDateTime

object Orders : IntIdTable("orders") {
    // 클라이언트 측 기본값
    val createdAt = datetime("created_at").clientDefault { 
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    
    // 서버 측 기본값
    val orderDate = date("order_date").defaultExpression(CurrentDate)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
```

### 4. 리터럴을 사용한 쿼리

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.kotlindatetime.dateLiteral
import org.jetbrains.exposed.v1.kotlindatetime.dateTimeLiteral

transaction {
    // 날짜 리터럴
    val date = LocalDate(2024, 1, 15)
    Events.select { Events.eventDate eq dateLiteral(date) }
    
    // 날짜시간 리터럴
    val dateTime = LocalDateTime(2024, 1, 15, 10, 30)
    Events.select { Events.createdAt eq dateTimeLiteral(dateTime) }
    
    // 범위 쿼리
    Events.select { 
        Events.createdAt between 
            dateTimeLiteral(LocalDateTime(2024, 1, 1, 0, 0)) and
            dateTimeLiteral(LocalDateTime(2024, 1, 31, 23, 59))
    }
}
```

### 5. Entity에서 사용

```kotlin
object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val birthDate = date("birth_date").nullable()
    val lastLoginAt = datetime("last_login_at").nullable()
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)
    
    var name by Users.name
    var birthDate by Users.birthDate
    var lastLoginAt by Users.lastLoginAt
}

// 사용법
transaction {
    val user = User.new {
        name = "John Doe"
        birthDate = LocalDate(1990, 5, 15)
        lastLoginAt = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
```

### 6. Clock을 활용한 테스트 가능한 코드

```kotlin
class UserService(private val clock: Clock = Clock.System) {
    fun createUser(name: String): User {
        return transaction {
            User.new {
                this.name = name
                lastLoginAt = clock.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
    }
}

// 테스트에서 고정 시간 사용
class UserServiceTest {
    @Test
    fun testCreateUser() {
        val fixedInstant = Instant.parse("2024-01-15T10:30:00Z")
        val testClock = Clock.fixed(fixedInstant, TimeZone.UTC)
        
        val service = UserService(clock = testClock)
        val user = service.createUser("Test User")
        
        assertEquals(
            LocalDateTime(2024, 1, 15, 10, 30),
            user.lastLoginAt
        )
    }
}
```

## 마이그레이션 가이드 (java.time → kotlinx.datetime)

```kotlin
// java.time
import java.time.LocalDate
import java.time.LocalDateTime

object OldTable : IntIdTable("old") {
    val date = date("date")  // java.time.LocalDate
    val datetime = datetime("datetime")  // java.time.LocalDateTime
}

// kotlinx.datetime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

object NewTable : IntIdTable("new") {
    val date = date("date")  // kotlinx.datetime.LocalDate
    val datetime = datetime("datetime")  // kotlinx.datetime.LocalDateTime
}
```

## 언제 어떤 모듈을 사용해야 하나?

| 상황                        | 권장 모듈                   |
|---------------------------|-------------------------|
| 서버 전용 Spring Boot 앱       | exposed-java-time       |
| Kotlin Multiplatform 프로젝트 | exposed-kotlin-datetime |
| Android + 서버 공유 코드        | exposed-kotlin-datetime |
| 기존 java.time 코드가 많음       | exposed-java-time       |

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :06-advanced:03-exposed-kotlin-datetime:test

# 특정 테스트만 실행
./gradlew :06-advanced:03-exposed-kotlin-datetime:test --tests "exposed.examples.kotlin.datetime.Ex01_KotlinDateTime"
```

## 더 읽어보기

- [Exposed Kotlin DateTime Module](https://debop.notion.site/Exposed-Kotlin-DateTime-1c32744526b0807bb3e8f149ef88f5f5)
- [kotlinx-datetime 문서](https://github.com/Kotlin/kotlinx-datetime)
