---
title: "JetBrains Exposed Official Manual"
tags: [exposed, jetbrains, official, manual, reference]
category: reference
created: 2026-04-13
updated: 2026-04-13
---

# JetBrains Exposed Official Manual

JetBrains Exposed 공식 문서 요약. 원본: https://www.jetbrains.com/help/exposed/

## Overview

Exposed는 JetBrains가 개발한 Kotlin용 경량 SQL ORM 프레임워크로, 두 가지 데이터베이스 접근 방식을 제공한다:

- **DSL (Domain-Specific Language)**: 타입 안전한 SQL 빌더. `object Table`로 테이블을 정의하고 `transaction {}` 블록 안에서 CRUD 수행
- **DAO (Data Access Objects)**: Entity 기반 ORM. `Entity`/`EntityClass`를 상속하여 객체지향 방식으로 데이터 접근

**지원 데이터베이스**: H2, PostgreSQL, MySQL, MariaDB, Oracle, SQLite, SQL Server

**요구 사항**: Kotlin 2.1+, JDK 8+ (Spring Boot 사용 시 JDK 17+)

## Getting Started

### Gradle 의존성 (최소 구성)

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jetbrains.exposed:exposed-core:1.2.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.2.0")
    implementation("org.jetbrains.exposed:exposed-dao:1.2.0")  // Optional (DAO API)
}
```

### 데이터베이스 연결

```kotlin
import org.jetbrains.exposed.v1.jdbc.Database

// URL 기반 연결
Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")

// DataSource 기반 연결
Database.connect(datasource)

// 사용자/비밀번호 포함
Database.connect(
    url = "jdbc:postgresql://localhost:5432/mydb",
    driver = "org.postgresql.Driver",
    user = "user",
    password = "password"
)
```

`Database.connect()`는 연결 설정만 저장하며, 실제 연결은 첫 트랜잭션 실행 시 수립된다.

## DSL API

### 테이블 정의

```kotlin
import org.jetbrains.exposed.v1.core.Table

object Tasks : Table("tasks") {
    val id = integer("id").autoIncrement()
    val title = varchar("name", 128)
    val description = varchar("description", 128)
    val isCompleted = bool("completed").default(false)

    override val primaryKey = PrimaryKey(id)
}
```

### INSERT

```kotlin
import org.jetbrains.exposed.v1.jdbc.insert

val taskId = Tasks.insert {
    it[title] = "Learn Exposed"
    it[description] = "Go through the tutorial"
} get Tasks.id
```

### SELECT

```kotlin
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.select

// 전체 조회
val allTasks = Tasks.selectAll().toList()

// 조건부 조회
Tasks.selectAll().where { Tasks.isCompleted eq true }.forEach {
    println(it[Tasks.title])
}

// 특정 컬럼 + 집계
Tasks.select(Tasks.id.count(), Tasks.isCompleted)
    .groupBy(Tasks.isCompleted)
    .forEach { println("${it[Tasks.isCompleted]}: ${it[Tasks.id.count()]}") }
```

### UPDATE

```kotlin
import org.jetbrains.exposed.v1.jdbc.update

Tasks.update({ Tasks.id eq taskId }) {
    it[isCompleted] = true
}
```

### DELETE

```kotlin
import org.jetbrains.exposed.v1.jdbc.deleteWhere

Tasks.deleteWhere { id eq secondTaskId }
```

### JOIN

```kotlin
val query = UsersTable
    .innerJoin(UserRatingsTable)
    .innerJoin(StarWarsFilmsTable)
    .select(UsersTable.columns)
    .where {
        StarWarsFilmsTable.sequelId eq 8 and (UserRatingsTable.value greater 5)
    }
    .withDistinct()
```

## DAO API

### 테이블 정의 (IdTable 사용)

```kotlin
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Tasks : IntIdTable("tasks") {
    val title = varchar("name", 128)
    val description = varchar("description", 128)
    val isCompleted = bool("completed").default(false)
}
```

### Entity 클래스

```kotlin
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Task(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Task>(Tasks)

    var title by Tasks.title
    var description by Tasks.description
    var isCompleted by Tasks.isCompleted
}
```

### CRUD 연산

```kotlin
transaction {
    // Create
    val task = Task.new {
        title = "Learn Exposed DAO"
        description = "Follow the DAO tutorial"
    }

    // Read
    val found = Task.findById(1)
    val completed = Task.find { Tasks.isCompleted eq true }.toList()

    // Update
    task.isCompleted = true  // 자동 반영

    // Delete
    task.delete()
}
```

### 관계 매핑

```kotlin
// Many-to-One (referencedOn)
class UserRatingEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserRatingEntity>(UserRatingsTable)

    var value by UserRatingsTable.value
    var film by StarWarsFilmEntity referencedOn UserRatingsTable.film
    var user by UserEntity referencedOn UserRatingsTable.user
}

// One-to-One back reference (backReferencedOn) - val 사용 필수
class UserWithSingleRatingEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserWithSingleRatingEntity>(UsersTable)

    var name by UsersTable.name
    val rating by UserRatingEntity backReferencedOn UserRatingsTable.user
}
```

### JOIN + Entity 래핑

```kotlin
val query = UsersTable.innerJoin(UserRatingsTable).innerJoin(StarWarsFilmsTable)
    .select(UsersTable.columns)
    .where { StarWarsFilmsTable.sequelId eq 8 }
    .withDistinct()

val users = UserEntity.wrapRows(query).toList()
```

## Transactions

### 기본 트랜잭션

```kotlin
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

transaction {
    // 모든 DSL/DAO 작업은 반드시 transaction 블록 안에서 실행
    SchemaUtils.create(Tasks)
    Tasks.insert { it[title] = "Hello" }
}
```

### 반환 값

```kotlin
val result = transaction {
    Tasks.selectAll().where { Tasks.isCompleted eq true }.toList()
}
```

### 코루틴 (Suspend Transaction)

```kotlin
// JDBC 기반 suspend
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

suspendTransaction {
    // suspend 함수 호출 가능
}

// R2DBC 기반 suspend
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspendTransaction {
    // 논블로킹 R2DBC 연산
}
```

### 중첩 트랜잭션 (Nested)

```kotlin
val db = Database.connect(/* ... */)
db.useNestedTransactions = true

transaction {
    FooTable.insert { it[id] = 1 }

    transaction {
        FooTable.insert { it[id] = 2 }
        rollback()  // 이 중첩 트랜잭션만 롤백
    }

    // 외부 트랜잭션의 데이터는 유지됨
}
```

### 재시도 설정

```kotlin
// DatabaseConfig에서 기본값 설정
val db = Database.connect(
    datasource = datasource,
    databaseConfig = DatabaseConfig {
        defaultMaxAttempts = 3
    }
)

// 개별 트랜잭션에서 오버라이드
transaction(db = db) {
    maxAttempts = 25
}
```

## Data Types

### 기본 컬럼 타입

```kotlin
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val bio = text("bio")
    val age = short("age")
    val rating = decimal("rating", 5, 2)  // 전체 5자리, 소수점 2자리
    val isActive = bool("is_active")

    override val primaryKey = PrimaryKey(id)
}
```

### 날짜/시간 (exposed-java-time)

```kotlin
import org.jetbrains.exposed.v1.java.time.datetime
import org.jetbrains.exposed.v1.java.time.timestamp

object Events : Table() {
    val createdAt = datetime("created_at")
    val updatedAt = timestamp("updated_at")
}
```

### JSON (exposed-json)

```kotlin
import org.jetbrains.exposed.v1.json.json
import org.jetbrains.exposed.v1.json.jsonb

object Configs : Table() {
    val data = json<Map<String, Any>>("data", Json.Default)
    val metadata = jsonb<Metadata>("metadata", Json.Default)
}
```

### 암호화 (exposed-crypt)

```kotlin
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.crypt.Algorithms

object Secrets : Table() {
    val secret = encryptedVarchar("secret", 256, Algorithms.AES_256_PBE_GCM("password", "salt"))
}
```

## Spring Boot Integration

### 의존성

```kotlin
// Spring Boot 4
dependencies {
    implementation("org.jetbrains.exposed:exposed-spring-boot4-starter:1.2.0")
}

// Spring Boot 3
dependencies {
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:1.2.0")
}
```

Starter에는 Exposed, `SpringTransactionManager`, Spring Boot Starter JDBC가 포함된다.

### Auto-Configuration 활성화

```kotlin
@SpringBootApplication
@ImportAutoConfiguration(
    value = [ExposedAutoConfiguration::class],
    exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
class ExposedSpringApplication
```

### 데이터베이스 설정 (application.properties)

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# SQL 로깅 활성화
spring.exposed.show-sql=true
```

### @Transactional 사용

Spring의 `@Transactional`을 사용하면 명시적 `transaction {}` 블록 없이 Exposed DSL/DAO를 바로 사용할 수 있다:

```kotlin
@Transactional
class MessageService {
    fun findMessageById(id: MessageId): Message? {
        return MessageEntity.selectAll()
            .where { MessageEntity.id eq id.value }
            .firstOrNull()
            ?.let { Message(id = MessageId(it[MessageEntity.id].value), text = it[MessageEntity.text]) }
    }
}
```

### 커스텀 트랜잭션 매니저 어노테이션

```kotlin
@Transactional(transactionManager = "springTransactionManager")
annotation class ExposedTransactional

@ExposedTransactional
fun doSomething() { /* ... */ }
```

## Modules

| Module | Description |
|--------|-------------|
| `exposed-core` | 핵심 모듈. 타입 안전 추상화, DSL API 제공 |
| `exposed-jdbc` | JDBC 전송 계층 구현 |
| `exposed-r2dbc` | R2DBC 전송 계층 (리액티브) |
| `exposed-dao` | DAO API (Entity/EntityClass 기반 ORM). `exposed-jdbc` 필요 |
| `exposed-java-time` | Java 8 Time API 기반 날짜/시간 확장 |
| `exposed-kotlin-datetime` | kotlinx-datetime 기반 날짜/시간 확장 |
| `exposed-jodatime` | Joda-Time 기반 날짜/시간 확장 |
| `exposed-json` | JSON / JSONB 컬럼 타입 확장 |
| `exposed-crypt` | 암호화 컬럼 타입 (클라이언트 사이드 인코딩/디코딩) |
| `exposed-money` | JavaMoney API (`MonetaryAmount`) 확장 |
| `exposed-spring-boot-starter` | Spring Boot 3 자동 설정 + SpringTransactionManager |
| `exposed-spring-boot4-starter` | Spring Boot 4 자동 설정 + SpringTransactionManager |

**전송 모듈 선택**: `exposed-jdbc` 또는 `exposed-r2dbc` 중 하나만 사용. 동시 사용 불가.

## Import 패턴 (v1)

Exposed v1부터 패키지 구조가 변경되었다:

```kotlin
// Core
import org.jetbrains.exposed.v1.core.*

// JDBC
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// DAO
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
```

## Quick Reference

| Operation | DSL | DAO |
|-----------|-----|-----|
| Create | `Table.insert { }` | `Entity.new { }` |
| Read all | `Table.selectAll()` | `Entity.all()` |
| Read by ID | `Table.selectAll().where { Table.id eq id }` | `Entity.findById(id)` |
| Read filtered | `Table.selectAll().where { condition }` | `Entity.find { condition }` |
| Update | `Table.update({ condition }) { }` | `entity.field = value` (자동 저장) |
| Delete | `Table.deleteWhere { condition }` | `entity.delete()` |
| Count | `Table.selectAll().count()` | `Entity.count()` |
