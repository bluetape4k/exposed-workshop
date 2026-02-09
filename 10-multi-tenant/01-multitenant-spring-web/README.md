# Exposed + Spring Web + Multitenant

이 모듈은 Spring Web Application 환경에서 Kotlin Exposed 프레임워크를 이용하여 멀티테넌시(Multi-tenancy)를 구현하는 방법을 자세히 설명합니다. 각 테넌트(tenant)별로 데이터베이스 스키마를 분리하여 데이터의 독립성과 보안을 보장하는
`Schema-based Multi-tenancy` 방식을 채택하고 있습니다.

## 주요 기술 스택

* **Spring Web:** 웹 애플리케이션의 컨트롤러 및 요청 처리를 담당합니다.
* **Kotlin Exposed:** SQL 중심의 경량 ORM 프레임워크로, 유연한 데이터베이스 접근을 제공합니다.
* **H2 Database:** 예제 실행을 위한 인메모리 데이터베이스로 사용됩니다. (실제 환경에서는 PostgreSQL, MySQL 등으로 대체 가능)
* **Gradle Kotlin DSL:** 빌드 자동화 및 의존성 관리에 사용됩니다.

## 멀티테넌시 구현 방식

이 모듈에서는 `Schema-based Multi-tenancy` 방식을 사용하여 각 테넌트의 데이터를 물리적으로 분리합니다.

1. **테넌트 식별 (Tenant Identification):** HTTP 요청 헤더(`X-Tenant-Id`)를 통해 현재 요청의 테넌트를 식별합니다.
2. **동적 데이터소스 라우팅 (Dynamic Datasource Routing):** 식별된 테넌트 ID에 따라 적절한 데이터베이스 스키마로 라우팅되도록 Exposed 트랜잭션을 설정합니다.
3. **데이터 격리 (Data Isolation):** 각 테넌트는 자신만의 전용 데이터베이스 스키마를 가지므로, 다른 테넌트의 데이터에 접근할 수 없습니다.

## 주요 기능 및 예제

* **테넌트 컨텍스트 관리:** `TenantContext`를 사용하여 현재 스레드에 테넌트 정보를 저장하고 관리합니다.
* **`TenantFilter`:** HTTP 요청마다 테넌트 ID를 추출하고 `TenantContext`에 설정하는 서블릿 필터 구현.
* **`TenantAwareDataSource`:** 동적으로 현재 테넌트에 맞는 데이터베이스 스키마를 연결하도록 Exposed에 알리는 데이터소스 구현.
* **테넌트별 데이터 초기화:** 각 테넌트 스키마 생성 및 초기 데이터를 삽입하는 예제.
* **RESTful API 예제:** 테넌트별로 데이터를 조회하고 조작하는 간단한 Actor API (`/api/{tenantId}/actors`)를 제공합니다.

## 프로젝트 실행 방법

1. 프로젝트를 클론합니다.
2. `01-multitenant-spring-web` 디렉토리로 이동합니다.
3. Gradle을 사용하여 애플리케이션을 실행합니다: `./gradlew bootRun`
4. 애플리케이션이 실행되면, Postman 또는 cURL과 같은 도구를 사용하여 API를 테스트할 수 있습니다.
   1. 테넌트 ID는 헤더로 전달합니다.
      1. 예: `GET /actors` with header `X-Tenant-Id: tenant1`
   2. path variable로 전달하는 방식을 사용하려면 ActorController의 매핑을 `/api/{tenantId}/actors`로 변경합니다.
      1. 예: `GET /api/tenant1/actors`

이 모듈을 통해 Spring Web 환경에서 Exposed와 함께 안전하고 확장 가능한 멀티테넌시 솔루션을 구축하는 데 필요한 기반 지식을 얻을 수 있습니다.
