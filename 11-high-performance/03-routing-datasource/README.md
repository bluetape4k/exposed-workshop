# 고성능을 위한 유연한 RoutingDataSource 구성

Spring Boot + Exposed 환경에서 **Multi-Tenant** 또는 **Read Replica** 구조를 구현할 때, 안전하고 유연한 방식으로
`DataSource` 라우팅을 구성하는 방법을 설명합니다.

이 모듈은 설계 가이드 문서로, 실제 소스 코드 없이 **아키텍처와 구현 패턴**을 제시합니다.

## 배경

대규모 서비스에서는 단일 DataSource로는 성능과 확장성의 한계가 있습니다. 일반적으로 다음과 같은 구조가 필요합니다:

- **Read Replica**: 읽기 트래픽을 복제 노드로 분산
- **Multi-Tenant**: 테넌트별 독립 DB로 데이터 격리
- **Sharding**: 데이터 규모에 따른 수평 분할

Spring의 `AbstractRoutingDataSource`를 활용하면 이러한 구조를 구현할 수 있지만, 기존의 정적 등록 방식에는 여러 문제점이 있습니다.

## 기존 문제점 (정적 등록 방식)

`setTargetDataSources(mapOf(...))` 방식의 위험성:

| 문제      | 설명                             |
|---------|--------------------------------|
| 키 값 실수  | 문자열 키 오타 시 런타임 NPE 발생          |
| 리팩토링 취약 | 문자열 키 변경 시 연관 코드 누락 위험         |
| 확장성 부족  | 신규 테넌트/복제 노드 추가 시 코드 수정 필요     |
| 테스트 어려움 | 정적 Map 구조로 인해 모의(Mock) 테스트 어려움 |

## 개선 전략: DataSourceRegistry 기반 동적 등록 구조

### 핵심 아이디어

```
┌──────────────┐     key     ┌──────────────────┐    DataSource   ┌────────┐
│   Client     │ ──────────→ │ RoutingDataSource│ ──────────────→ │   DB   │
└──────────────┘             └──────────────────┘                 └────────┘
                                    │
                                    │ resolveRoutingKey()
                                    ▼
                             ┌───────────────────┐
                             │ DataSourceRegistry│  ← 동적 등록/조회
                             └───────────────────┘
```

- `RoutingDataSource`는 **라우팅 키만 결정** (역할 분리)
- 실제 `DataSource` 인스턴스는 `DataSourceRegistry`에서 **동적으로 조회**
- `setTargetDataSources()`는 최소한으로만 설정 (또는 사용하지 않음)

## 구성 요소 설계

### 1. DataSourceRegistry 인터페이스 및 구현

DataSource를 키 기반으로 등록/조회하는 레지스트리입니다.
`ConcurrentHashMap`을 사용하여 스레드 안전성을 보장합니다.

```kotlin
interface DataSourceRegistry {
    fun get(key: String): DataSource?
    fun register(key: String, dataSource: DataSource)
}

@Component
class InMemoryDataSourceRegistry: DataSourceRegistry {
    private val dataSources = ConcurrentHashMap<String, DataSource>()

    override fun get(key: String): DataSource? = dataSources[key]
    override fun register(key: String, dataSource: DataSource) {
        dataSources[key] = dataSource
    }
}
```

### 2. DynamicRoutingDataSource 추상 클래스

Spring의 `AbstractRoutingDataSource`를 확장하되, 레지스트리를 통해 DataSource를 동적으로 조회합니다.
`determineTargetDataSource()`를 오버라이드하여 정적 Map 대신 레지스트리를 사용합니다.

```kotlin
abstract class DynamicRoutingDataSource(
    private val registry: DataSourceRegistry
): AbstractRoutingDataSource() {

    override fun determineTargetDataSource(): DataSource {
        val key = determineCurrentLookupKey()?.toString()
            ?: throw IllegalStateException("No routing key")

        return registry.get(key)
            ?: throw IllegalStateException("No DataSource for key: $key")
    }

    override fun determineCurrentLookupKey(): Any? {
        return resolveRoutingKey()
    }

    protected abstract fun resolveRoutingKey(): String?
}
```

### 3. ContextAwareRoutingDataSource 구현

테넌트 컨텍스트와 트랜잭션 읽기 전용 여부를 기반으로 라우팅 키를 결정합니다.

```kotlin
class ContextAwareRoutingDataSource(
    registry: DataSourceRegistry,
    private val tenantContext: TenantContextHolder
): DynamicRoutingDataSource(registry) {

    override fun resolveRoutingKey(): String? {
        val tenantId = tenantContext.currentTenantId()
        val isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()

        return if (tenantId != null) {
            if (isReadOnly) "${tenantId}_replica" else "${tenantId}_master"
        } else {
            if (isReadOnly) "default_replica" else "default_master"
        }
    }
}
```

#### 라우팅 키 결정 로직

| 테넌트 ID    | 읽기 전용   | 라우팅 키             |
|-----------|---------|-------------------|
| `null`    | `false` | `default_master`  |
| `null`    | `true`  | `default_replica` |
| `tenant1` | `false` | `tenant1_master`  |
| `tenant1` | `true`  | `tenant1_replica` |

## Spring Boot 자동 설정 구성

### `application.yml` 예시

```yaml
multi-datasource:
    datasources:
        tenant1:
            url: jdbc:mysql://db1/tenant1
            username: user
            password: pass
        tenant2:
            url: jdbc:mysql://db2/tenant2
            username: user
            password: pass
        default_master:
            url: jdbc:mysql://db0/master
            username: user
            password: pass
        default_replica:
            url: jdbc:mysql://replica/master
            username: user
            password: pass
```

### 설정 클래스

`@ConfigurationProperties`를 사용하여 YAML 설정을 타입 안전하게 바인딩합니다.

```kotlin
@ConfigurationProperties(prefix = "multi-datasource")
data class MultiDataSourceProperties(
    val datasources: Map<String, DataSourceConfig>
)

data class DataSourceConfig(
    val url: String,
    val username: String,
    val password: String,
    val driverClassName: String = "com.mysql.cj.jdbc.Driver"
)
```

### 자동 등록 구성

애플리케이션 시작 시 YAML에 정의된 모든 DataSource를 레지스트리에 자동으로 등록합니다.

```kotlin
@Configuration
@EnableConfigurationProperties(MultiDataSourceProperties::class)
class MultiDataSourceAutoConfiguration(
    private val props: MultiDataSourceProperties
) {

    @Bean
    fun dataSourceRegistry(): DataSourceRegistry {
        val registry = InMemoryDataSourceRegistry()
        props.datasources.forEach { (key, cfg) ->
            val ds = DataSourceBuilder.create()
                .url(cfg.url)
                .username(cfg.username)
                .password(cfg.password)
                .driverClassName(cfg.driverClassName)
                .build()
            registry.register(key, ds)
        }
        return registry
    }

    @Bean
    fun dataSource(registry: DataSourceRegistry, tenantContext: TenantContextHolder): DataSource {
        return ContextAwareRoutingDataSource(registry, tenantContext)
    }
}
```

## 기존 방식 vs 개선 방식 비교

| 항목        | 기존 방식                    | 개선 방식                 |
|-----------|--------------------------|-----------------------|
| Key 정적 등록 | `setTargetDataSources()` | 동적 `Registry` 등록      |
| Key 관리    | 문자열 하드코딩                 | YAML 설정 기반 바인딩        |
| 신규 추가     | 코드 변경 필요                 | YAML 등록만 하면 OK        |
| 리팩토링 안정성  | 낮음                       | 높음 (enum or 타입 래핑 가능) |
| 테스트 용이성   | 낮음 (정적 Map)              | 높음 (Registry Mock 가능) |
| 런타임 확장    | 불가                       | 가능 (동적 register 호출)   |

## 활용 시나리오

### Separate DB per Tenant

```
Tenant A → tenant_a_master / tenant_a_replica
Tenant B → tenant_b_master / tenant_b_replica
```

- 테넌트별 완전한 데이터 격리
- `TenantContextHolder`를 통해 현재 테넌트 식별

### Read Replica 분산

```
Write 요청 → default_master
Read 요청  → default_replica
```

- `@Transactional(readOnly = true)` 설정만으로 자동 라우팅
- Spring의 `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` 활용

### Sharding

```
User ID % 4 == 0 → shard_0
User ID % 4 == 1 → shard_1
User ID % 4 == 2 → shard_2
User ID % 4 == 3 → shard_3
```

- `resolveRoutingKey()`만 변경하면 샤딩 전략에 쉽게 적용 가능

## 활용 팁

- `resolveRoutingKey()`만 변경하면 **다른 라우팅 전략에 쉽게 적용**할 수 있습니다.
- 테넌트 컨텍스트는 `ThreadLocal`, `CoroutineContext`, 또는 Spring Security의 `SecurityContextHolder`를 통해 전달할 수 있습니다.
- 운영 환경에서는 레지스트리에 **헬스체크 로직**을 추가하여 장애 DataSource를 자동 제거하는 것을 권장합니다.
- HikariCP와 함께 사용할 경우 각 DataSource별로 독립적인 Connection Pool이 생성됩니다.
