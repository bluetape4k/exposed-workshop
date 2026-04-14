# 09 Spring: AutoConfiguration (01)

[English](./README.md) | 한국어

Spring Boot 자동 설정으로 Exposed를 최소 구성으로 통합하는 모듈입니다.
`spring-boot-autoconfigure` 가 제공하는 `SpringTransactionManager`, `DatabaseInitializer` 빈을 활용해
`application.yml` 한 파일로 DataSource와 트랜잭션을 연결하는 패턴을 학습합니다.

## 학습 목표

- Spring Boot 자동 구성이 Exposed용 `SpringTransactionManager`와 `DatabaseInitializer`를 어떻게 등록하는지 이해한다.
- `application.yml` 프로퍼티(`spring.exposed.*`)로 DDL 자동 생성과 SQL 로깅을 제어하는 방법을 익힌다.
- `DatabaseConfig` 빈을 재정의해 엔티티 캐시 크기 등 Exposed 동작을 커스터마이즈한다.
- `@SpringBootApplication(exclude = [...])` 로 충돌하는 Auto-Configuration을 제외하는 방법을 확인한다.

## 선수 지식

- Spring Boot 자동 구성 원리
- [`../04-exposed-ddl/01-connection/README.ko.md`](../../04-exposed-ddl/01-connection/README.ko.md)

## 아키텍처

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    class SpringBootApplication {
        +exclude: DataSourceTransactionManagerAutoConfiguration
    }
    class ExposedAutoConfiguration {
        <<AutoConfiguration>>
        +springTransactionManager() SpringTransactionManager
        +databaseInitializer() DatabaseInitializer
    }
    class SpringTransactionManager {
        +dataSource: DataSource
        +databaseConfig: DatabaseConfig
    }
    class DatabaseInitializer {
        +tables: List~Table~
        +generateDdl: Boolean
    }
    class DatabaseConfig {
        +maxEntitiesToStoreInCachePerEntity: Int
        +useNestedTransactions: Boolean
    }

    SpringBootApplication --> ExposedAutoConfiguration : auto-configure
    ExposedAutoConfiguration --> SpringTransactionManager : creates
    ExposedAutoConfiguration --> DatabaseInitializer : creates (when generate-ddl=true)
    SpringTransactionManager --> DatabaseConfig : uses

    style SpringBootApplication fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style ExposedAutoConfiguration fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style SpringTransactionManager fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style DatabaseInitializer fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style DatabaseConfig fill:#FFFDE7,stroke:#FFF176,color:#F57F17
```

## 핵심 개념

### application.yml 설정

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:test
    driver-class-name: org.h2.Driver
  exposed:
    generate-ddl: false   # true 시 DatabaseInitializer가 SchemaUtils.create() 실행
    show-sql: true        # Exposed SQL 로그 출력
```

### DatabaseConfig 재정의

```kotlin
@TestConfiguration
class CustomDatabaseConfigConfiguration {

    @Bean
    fun customDatabaseConfig(): DatabaseConfig = DatabaseConfig {
        maxEntitiesToStoreInCachePerEntity = 100
        useNestedTransactions = true
    }
}
```

### Auto-Configuration 제외

```kotlin
@SpringBootApplication(
    exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
class Application
```

`DataSourceTransactionManagerAutoConfiguration`이 등록한 `DataSourceTransactionManager`는 Exposed의
`SpringTransactionManager`와 충돌하므로 반드시 제외해야 합니다.

## 자동 등록 빈 흐름

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'", "actorBkg": "#E3F2FD", "actorBorder": "#90CAF9", "actorTextColor": "#1565C0", "actorLineColor": "#90CAF9", "activationBkgColor": "#E8F5E9", "activationBorderColor": "#A5D6A7", "labelBoxBkgColor": "#FFF3E0", "labelBoxBorderColor": "#FFCC80", "labelTextColor": "#E65100", "loopTextColor": "#6A1B9A", "noteBkgColor": "#F3E5F5", "noteBorderColor": "#CE93D8", "noteTextColor": "#6A1B9A", "signalColor": "#1565C0", "signalTextColor": "#1565C0"}}}%%
sequenceDiagram
    participant Boot as Spring Boot
    participant AC as ExposedAutoConfiguration
    participant STM as SpringTransactionManager
    participant DI as DatabaseInitializer

    Boot->>AC: auto-configure 실행
    AC->>STM: SpringTransactionManager 빈 등록
    Note over AC: spring.exposed.generate-ddl=true 일 때만
    AC->>DI: DatabaseInitializer 빈 등록
    DI->>DI: SchemaUtils.create(tables...)
    Boot->>STM: @Transactional 요청 위임
```

## 테이블 정의 예시

```kotlin
object TestTable: IntIdTable("test_table") {
    val name = varchar("name", 100)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class TestEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TestEntity>(TestTable)

    var name by TestTable.name
    var createdAt by TestTable.createdAt
}
```

`spring.exposed.generate-ddl=true`로 설정하면 `DatabaseInitializer`가 스캔한 `Table` 객체를 대상으로 `SchemaUtils.create()`를 자동 실행합니다.

## 실행 방법

```bash
# 전체 모듈 테스트
./gradlew :09-spring:01-springboot-autoconfigure:test

# 테스트 로그 요약
./bin/repo-test-summary -- ./gradlew :09-spring:01-springboot-autoconfigure:test
```

## 실습 체크리스트

- `spring.exposed.generate-ddl=true/false` 전환 시 `DatabaseInitializer` 빈 유무 차이 확인
- `DatabaseConfig` 빈을 재정의했을 때 `maxEntitiesToStoreInCachePerEntity` 값이 반영되는지 검증
- `DataSourceTransactionManagerAutoConfiguration` 제외 없이 기동 시 트랜잭션 충돌 오류 재현

## 성능·안정성 체크포인트

- 자동 구성 기본값(`show-sql=true`)은 운영 환경에서 반드시 `false`로 변경
- `generate-ddl=true`는 개발/테스트 전용, 운영에서는 마이그레이션 도구(Flyway/Liquibase) 사용

## 다음 모듈

- [`../02-transactiontemplate/README.ko.md`](../02-transactiontemplate/README.ko.md)
