# Exposed + Spring Webflux + Coroutines + Multitenant

이 모듈은 반응형(Reactive) Spring WebFlux Application 환경에서 Kotlin Coroutines와 Exposed 프레임워크를 이용하여 멀티테넌시(Multi-tenancy)를 구현하는 방법을 자세히 설명합니다.
`Schema-based Multi-tenancy` 방식을 채택하여 각 테넌트(tenant)별로 데이터베이스 스키마를 분리함으로써 데이터의 독립성과 보안을 보장하며, 여기에 WebFlux와 Coroutines의 비동기/논블로킹 처리 능력을 결합합니다.

## 주요 기술 스택

* **Spring WebFlux:** 논블로킹(Non-blocking) 및 반응형 웹 애플리케이션 개발을 위한 프레임워크입니다.
* **Kotlin Coroutines:** 비동기 프로그래밍을 간소화하고, 효율적인 동시성 처리를 가능하게 합니다.
* **Kotlin Exposed:** SQL 중심의 경량 ORM 프레임워크로, 유연한 데이터베이스 접근을 제공합니다. Coroutines를 통한 비동기 트랜잭션 처리를 지원합니다.
* **H2 Database:** 예제 실행을 위한 인메모리 데이터베이스로 사용됩니다. (실제 환경에서는 PostgreSQL, MySQL 등으로 대체 가능)
* **Gradle Kotlin DSL:** 빌드 자동화 및 의존성 관리에 사용됩니다.

## 멀티테넌시 구현 방식 및 WebFlux/Coroutines 활용

이 모듈에서는
`Schema-based Multi-tenancy` 방식을 사용하여 각 테넌트의 데이터를 물리적으로 분리하며, Spring WebFlux와 Kotlin Coroutines를 활용하여 비동기적이고 효율적인 방식으로 요청을 처리합니다.

1. **테넌트 식별 (Tenant Identification):** HTTP 요청 헤더(`X-Tenant-Id`)를 통해 현재 요청의 테넌트를 식별합니다. WebFlux의
   `WebFilter`를 통해 비동기적으로 테넌트 정보를 추출하고 관리합니다.
2. **동적 데이터소스 라우팅 (Dynamic Datasource Routing):** 식별된 테넌트 ID에 따라 적절한 데이터베이스 스키마로 라우팅되도록 Exposed 트랜잭션을 설정합니다. Coroutines의
   `newSuspendedTransaction`과 같은 기능을 활용하여 비동기 컨텍스트 내에서 테넌트별 트랜잭션을 안전하게 처리합니다.
3. **데이터 격리 (Data Isolation):** 각 테넌트는 자신만의 전용 데이터베이스 스키마를 가지므로, 다른 테넌트의 데이터에 접근할 수 없습니다.
4. **비동기/논블로킹 처리:** WebFlux의 반응형 스트림과 Coroutines의 `suspend` 함수를 통해 데이터베이스 I/O 작업을 논블로킹 방식으로 처리하여, 높은 동시성과 확장성을 제공합니다.

## 주요 기능 및 예제

* **테넌트 컨텍스트 관리:** 반응형 컨텍스트(`Reactor Context`)를 통해 테넌트 정보를 전달하고 관리하여, Coroutines 내에서 현재 테넌트 정보에 접근할 수 있도록 합니다.
* **`TenantFilter`:** HTTP 요청에서 테넌트 ID를 추출하고 `Reactor Context`에 저장하여, 다운스트림 서비스에서 이 정보를 활용할 수 있도록 합니다.
* **`TenantAwareDataSource`:** 동적으로 현재 테넌트에 맞는 데이터베이스 스키마를 연결하도록 Exposed에 알리는 데이터소스 구현.
* **테넌트별 데이터 초기화:** 각 테넌트 스키마 생성 및 초기 데이터를 삽입하는 비동기 예제.
* **반응형 RESTful API 예제:** 테넌트별로 데이터를 조회하고 조작하는 비동기 Actor API (`/api/{tenantId}/actors`)를 제공합니다.

## 프로젝트 실행 방법

1. 프로젝트를 클론합니다.
2. `03-multitenant-spring-webflux` 디렉토리로 이동합니다.
3. Gradle을 사용하여 애플리케이션을 실행합니다: `./gradlew bootRun`
4. 애플리케이션이 실행되면, Postman 또는 cURL과 같은 도구를 사용하여 API를 테스트할 수 있습니다.
   1. 테넌트 ID는 헤더로 전달합니다.
      1. 예: `GET /actors` with header `X-Tenant-Id: tenant1`
   2. path variable로 전달하는 방식을 사용하려면 ActorController의 매핑을 `/api/{tenantId}/actors`로 변경합니다.
      1. 예: `GET /api/tenant1/actors`

이 모듈을 통해 Spring WebFlux, Kotlin Coroutines, Exposed를 활용하여 고성능, 반응형 멀티테넌트 웹 애플리케이션을 구축하는 데 필요한 지식과 실용적인 예제를 얻을 수 있습니다.
