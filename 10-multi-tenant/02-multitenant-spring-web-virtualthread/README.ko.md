# Exposed + Spring Web + Virtual Threads + Multi-Tenant (02)

[English](./README.md) | 한국어

`01` 모듈의 멀티테넌시 구조를 Java 25 Virtual Threads 환경으로 확장한 예제입니다. 블로킹 I/O 스타일을 유지하면서 동시 처리량을 높이는 구성에 초점을 맞춥니다. 모듈 내부 구현 대신 공통
`bluetape4k-tenant`의 `ScopedValueTenantContext`를 사용해 Virtual Thread 친화적인 컨텍스트 전파를 구현합니다.

## 학습 목표

- Virtual Thread 기반 요청 처리 모델을 이해한다.
- `ThreadLocal` vs `ScopedValue` 컨텍스트 전파 방식의 차이를 비교한다.
- `TransactionSchemaAspect`로 스키마 생성과 전환을 동시에 처리하는 방식을 익힌다.
- 동시성 증가 상황에서 격리/안정성을 검증한다.

## 선수 지식

- [`../01-multitenant-spring-web/README.md`](../01-multitenant-spring-web/README.md)
- Java 25 Virtual Threads 기초

## 의존성

이 모듈은 `2.0.0-SNAPSHOT` 의존성 계열의 공통 tenant carrier를 사용합니다.
catalog alias에는 버전을 직접 쓰지 않으며 `bluetape4k-dependencies`와 BOM이
버전의 기준입니다.

```kotlin
implementation(libs.bluetape4k.tenant)
```

---

## 도메인 모델

![02 multitenant spring web virtualthread Entity Relationship diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-erd-01.png)

---

## 01 모듈과의 핵심 차이

| 항목         | 01 (Spring MVC)                   | 02 (Virtual Threads)                                 |
|------------|-----------------------------------|------------------------------------------------------|
| 스레드 모델     | OS 스레드 풀 (Tomcat 기본)              | Virtual Thread per request                           |
| 컨텍스트 저장    | `ThreadLocal`                     | `ScopedValue`                                        |
| 스키마 Aspect | `TenantSchemaAspect` (setSchema만) | `TransactionSchemaAspect` (createSchema + setSchema) |
| Tomcat 설정  | 기본                                | `TomcatVirtualThreadConfig`                          |

---

## 아키텍처

![02 multitenant spring web virtualthread Class Structure 2 diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-class-02.png)

### ScopedValue 기반 컨텍스트 전파

Virtual Thread는 수백만 개가 동시에 생성될 수 있어 `ThreadLocal`의 메모리 오버헤드가 문제가 됩니다. Java 25의
`ScopedValue`는 불변 바인딩으로 동작해 Virtual Thread 환경에 적합합니다.

```
ThreadLocal  → 변경 가능, 반드시 수동 clear() 필요
ScopedValue  → 불변 바인딩, 스코프 벗어나면 자동 소멸
```

### Platform Thread vs Virtual Thread 비교

![Platform Thread vs Virtual Thread diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-architecture-03.png)

---

## 요청 흐름

![02 multitenant spring web virtualthread Sequence Flow 4 diagram](../../docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-sequence-04.png)

---

## 핵심 구현

### TomcatVirtualThreadConfig

`spring.threads.virtual.enabled=true`(기본값) 조건에서 Tomcat의 `ProtocolHandler` executor를
`Executors.newVirtualThreadPerTaskExecutor()`로 교체합니다. 기존 코드 변경 없이 Virtual Thread를 활성화하는 최소 설정입니다.

```kotlin
@Bean
fun protocolHandlerVirtualThreadExecutorCustomizer(): TomcatProtocolHandlerCustomizer<*> {
    return TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
        protocolHandler.executor = Executors.newVirtualThreadPerTaskExecutor()
    }
}
```

### TenantContexts (공통 ScopedValue carrier)

`TenantContexts`는 `bluetape4k-tenant`의 공통 `ScopedValueTenantContext`를 감싸는
얇은 애플리케이션 경계입니다. header parsing과 `Tenants` 조회는 애플리케이션이
소유하고, lexical binding과 기본 tenant를 두지 않는 조회 의미는 공통 carrier에
위임합니다. 값은 carrier scope 안에서만 유효하며 블록 종료 시 자동으로 소멸됩니다.

```kotlin
object TenantContexts {
    private val delegate = ScopedValueTenantContext()

    fun currentOrNull(): Tenant? = delegate.currentOrNull()?.let { Tenants.getById(it.value) }
    fun current(): Tenant = Tenants.getById(delegate.requireCurrent().value)
    fun <T> withTenant(tenant: Tenant, block: () -> T): T =
        delegate.withTenant(BluetapeTenantId(tenant.id), block)
}
```

### TransactionSchemaAspect

`01` 모듈의 `TenantSchemaAspect`와 동일한 역할을 하지만,
`SchemaUtils.createSchema()`를 추가로 호출해 스키마가 없을 경우 자동 생성합니다. Virtual Thread 환경에서 동시 요청이 몰릴 때 스키마 초기화 경합을 방지합니다.

```kotlin
@Before("@within(...Transactional) || @annotation(...Transactional)")
fun setSchemaForTransaction() {
    transaction {
        val schema = getSchemaDefinition(TenantContexts.current())
        SchemaUtils.createSchema(schema)  // 01 모듈 대비 추가
        SchemaUtils.setSchema(schema)
        commit()
    }
}
```

### TenantFilter

`01` 모듈과 동일한 서블릿 필터 인터페이스를 사용하지만, 내부적으로
`TenantContexts.withTenant()`가 공통 `ScopedValueTenantContext`에 위임됩니다.
header가 없거나 알 수 없는 경우의 명시적인 애플리케이션 정책은 필터가 소유하며,
공통 carrier 자체는 tenant를 대체하지 않습니다.

---

## 주요 구성 요소 요약

| 파일                                    | 역할                                  |
|---------------------------------------|-------------------------------------|
| `config/TomcatVirtualThreadConfig.kt` | Tomcat executor를 Virtual Thread로 교체 |
| `tenant/TenantFilter.kt`              | 헤더에서 테넌트 추출, 공통 ScopedValue carrier 바인딩 |
| `tenant/TenantContexts.kt`            | 애플리케이션 tenant를 `ScopedValueTenantContext`에 매핑 |
| `tenant/Tenants.kt`                   | 테넌트 열거형 + 스키마 매핑                    |
| `tenant/SchemaSupport.kt`             | `Schema` 객체 생성 헬퍼                   |
| `tenant/TransactionSchemaAspect.kt`   | AOP로 트랜잭션 전 스키마 생성/전환               |
| `tenant/TenantAwareDataSource.kt`     | 테넌트 기반 DataSource 라우팅               |
| `tenant/TenantInitializer.kt`         | 앱 기동 시 스키마/데이터 초기화                  |
| `tenant/DataInitializer.kt`           | 스키마 생성 + 샘플 데이터 삽입                  |
| `config/ExposedMultitenantConfig.kt`  | DataSource/Database 빈 설정            |
| `controller/ActorController.kt`       | 배우 조회 REST API                      |

---

## 테스트 방법

```bash
# 모듈 테스트 실행
./gradlew :02-multitenant-spring-web-virtualthread:test

# 애플리케이션 기동
./gradlew :02-multitenant-spring-web-virtualthread:bootRun
```

### API 실습

```bash
# 한국어 테넌트 배우 목록
curl -H 'X-TENANT-ID: korean' http://localhost:8080/actors

# 영어 테넌트 배우 목록
curl -H 'X-TENANT-ID: english' http://localhost:8080/actors

# 특정 배우 조회
curl -H 'X-TENANT-ID: english' http://localhost:8080/actors/1
```

---

## 실습 체크리스트

- `X-TENANT-ID: korean`과 `X-TENANT-ID: english` 응답 데이터가 다른지 확인
- 동시 요청 수를 늘려도 테넌트 데이터가 교차되지 않는지 검증
- `ScopedValue` 스코프 밖에서 `currentOrNull()`이 `null`을 반환하고 `current()`가 `MissingTenantContextException`을 던지는지 확인
- 스레드 풀/커넥션 풀 설정값 변경 시 지연시간 변화 측정

## 운영 체크포인트

- Virtual Thread 증가만으로 DB 병목이 해결되지 않으므로 HikariCP `maximumPoolSize` 함께 튜닝
- `ScopedValue` 바인딩은 현재 스코프 안에서 불변이지만, 중첩 스코프에서는 다른 테넌트를 임시로 재바인딩하고 종료 시 바깥 바인딩을 복원 — 설계 단계에서 흐름 확정 필요
- 장시간 블로킹 작업을 요청 경로에 두지 않도록 점검
- tenant 누수 탐지를 위한 통합 테스트를 CI에 고정

---

## 다음 모듈

- [
  `../03-multitenant-spring-webflux/README.md`](../03-multitenant-spring-webflux/README.md): WebFlux + Coroutines 기반 논블로킹 멀티테넌트
