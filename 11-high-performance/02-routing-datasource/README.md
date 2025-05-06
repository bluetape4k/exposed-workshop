# 고성능을 위한 유연한 RoutingDataSource 구성

이 문서는 Spring Boot + Exposed 환경에서 Multi-Tenant 또는 Read Replica 구조를 구현할 때, 안전하고 유연한 방식으로 `DataSource` 라우팅을 구성하는 방법을 설명합니다.

---

## 🔥 기존 문제점 (정적 등록 방식)

- `setTargetDataSources(mapOf(...))` 방식은 다음과 같은 위험이 있음:
    - 키 값 실수 → 런타임 NPE
    - 문자열 키 리팩토링 시 누락 위험
    - 신규 테넌트/복제 노드 추가 시 코드 수정 필요

---

## ✅ 개선 전략: DataSourceRegistry 기반의 동적 등록 구조

### 🎯 핵심 아이디어

- `RoutingDataSource`는 단지 key만 반환
- 실제 `DataSource` 인스턴스는 `DataSourceRegistry`에서 동적으로 조회
- `setTargetDataSources()`는 최소한으로만 설정

---

## 🧱 구성 요소 설계

### 1. DataSourceRegistry 인터페이스 및 구현

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

---

### 2. DynamicRoutingDataSource 추상 클래스

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

---

### 3. ContextAwareRoutingDataSource 구현

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

---

## ⚙️ Spring Boot Configuration 자동 설정 구성

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

---

### 설정 클래스

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

---

### 자동 등록 구성

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

---

## ✨ 이점 요약

| 항목        | 기존 방식                    | 개선 방식                 |
|-----------|--------------------------|-----------------------|
| Key 정적 등록 | `setTargetDataSources()` | 동적 `Registry` 등록      |
| Key 관리    | 문자열 하드코딩                 | 설정 기반 바인딩             |
| 신규 추가     | 코드 변경 필요                 | `YAML` 등록만 하면 OK      |
| 리팩토링 안정성  | 낮음                       | 높음 (enum or 타입 래핑 가능) |

---

## 📘 활용 팁

- 이 방식은 **Separate DB per Tenant**, **Read Replica**, **Sharding** 등 다양한 DB 구성에 재사용 가능
- `resolveRoutingKey()`만 변경하면 다른 전략에 쉽게 적용됨
