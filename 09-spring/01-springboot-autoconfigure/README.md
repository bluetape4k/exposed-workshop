# 09 Spring: Spring Boot AutoConfiguration

이 모듈은 Spring Boot의 자동 구성을 활용하여 Exposed를 설정하는 방법을 단계별로 학습합니다. 최소한의 설정으로 Exposed를 Spring Boot 애플리케이션에 통합하는 방법을 다룹니다.

## 학습 목표

- Spring Boot의 Exposed 자동 구성 메커니즘 이해
- `application.yml`을 통한 최소 설정으로 Exposed 연결
- Spring의 `@Transactional`과 Exposed 트랜잭션 통합
- 자동 구성된 `Database` 인스턴스 활용

## 주요 기능

| 기능           | 설명                                   |
|--------------|--------------------------------------|
| 자동 데이터베이스 연결 | `spring.datasource` 프로퍼티 기반 자동 구성    |
| 트랜잭션 관리      | `@Transactional` 어노테이션으로 선언적 트랜잭션 관리 |
| 단순화된 설정      | 상용구 코드 없이 Exposed 설정                 |
| Spring 통합    | Spring 생태계와 완벽한 통합                   |

## 설정

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
    
  # JPA DDL 자동 생성 비활성화 (Exposed가 관리)
  jpa:
    hibernate:
      ddl-auto: none

# 선택적 Exposed 설정
exposed:
  scan-packages: com.example.app.tables
```

### build.gradle.kts

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    
    // Spring Boot Starter for Exposed (사용 가능한 경우)
    // implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    
    runtimeOnly("com.h2database:h2")
}
```

## 코드 예제

### 메인 애플리케이션

```kotlin
@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
```

### 데이터 초기화

```kotlin
@Component
class DataLoader(
    private val transactionTemplate: TransactionTemplate
) : ApplicationRunner {
    
    override fun run(args: ApplicationArguments?) {
        transactionTemplate.execute {
            // 스키마 생성
            SchemaUtils.create(Users, Posts)
            
            // 샘플 데이터
            Users.insert {
                it[name] = "John Doe"
                it[email] = "john@example.com"
            }
            
            Users.selectAll().forEach {
                println("User: ${it[Users.name]}, Email: ${it[Users.email]}")
            }
        }
    }
}
```

### 테이블 정의

```kotlin
object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
```

### 서비스 레이어

```kotlin
@Service
class UserService {
    
    @Transactional
    fun createUser(name: String, email: String): Int {
        return Users.insertAndGetId {
            it[Users.name] = name
            it[Users.email] = email
        }.value
    }
    
    @Transactional(readOnly = true)
    fun findAllUsers(): List<UserDto> {
        return Users.selectAll().map { row ->
            UserDto(
                id = row[Users.id].value,
                name = row[Users.name],
                email = row[Users.email]
            )
        }
    }
    
    @Transactional
    fun updateUser(id: Int, name: String) {
        Users.update({ Users.id eq id }) {
            it[Users.name] = name
        }
    }
}
```

## 자동 구성 커스터마이징

```kotlin
@Configuration
class ExposedConfig {
    
    @Bean
    fun databaseConfig(): DatabaseConfig {
        return DatabaseConfig {
            // 엔티티 캐시 크기
            maxEntitiesToStoreInCachePerEntity = 1000
            
            // 중첩 트랜잭션 사용
            useNestedTransactions = true
            
            // 기본 격리 수준
            defaultIsolationLevel = TransactionIsolation.READ_COMMITTED
        }
    }
    
    @Bean
    fun database(dataSource: DataSource): Database {
        return Database.connect(
            datasource = dataSource,
            databaseConfig = databaseConfig()
        )
    }
}
```

## 실행

```bash
# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew :09-spring:01-springboot-autoconfigure:test
```

## 더 읽어보기

- [Spring Boot AutoConfiguration](https://debop.notion.site/Spring-Boot-AutoConfiguration-1c32744526b080079af9eb44b62466d0)
- [Spring Boot 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/index.html)
