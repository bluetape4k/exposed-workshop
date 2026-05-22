# Database-per-Tenant Spring Web (05)

[English](./README.md) | 한국어

이 Spring MVC 예제는 각 tenant를 별도 datasource와 물리 H2 데이터베이스로
라우팅합니다. schema-per-tenant 예제와 달리 하나의 connection pool에서
schema를 바꾸지 않고, 허용된 tenant마다 전용 Hikari pool과 Exposed
`Database`를 둡니다.

이 전략은 tenant별 백업, 복구, 보존 정책, 마이그레이션, 운영 소유권을 강하게
분리해야 할 때 적합합니다. 이 워크숍은 datasource routing 경계를 명확히
보여주기 위해 `X-Tenant-ID`를 단순화해 사용합니다. 운영 환경에서는 tenant
identity를 인증 claim 또는 서버 측 session 상태와 반드시 연결해야 합니다.

## Architecture Diagram

![Database-per-Tenant Spring Web Architecture diagram](../../docs/images/readme-diagrams/10-multi-tenant-05-database-per-tenant-spring-web-architecture-01.png)

## Request Flow

![Database-per-Tenant Spring Web Sequence diagram](../../docs/images/readme-diagrams/10-multi-tenant-05-database-per-tenant-spring-web-sequence-02.png)

## 전략

| 관심사 | Database-per-tenant 동작 |
|---|---|
| Tenant resolution | `TenantFilter`가 하나의 `X-Tenant-ID` header를 읽고 `acme`, `globex`만 허용합니다 |
| Fallback | 기본 datasource가 없습니다. tenant 누락은 400, 알 수 없는 tenant는 404입니다 |
| Routing boundary | `TenantTransaction`이 현재 tenant를 해석하고 Exposed `transaction(database)`를 호출합니다 |
| Isolation | 각 tenant는 서로 다른 H2 JDBC URL과 Hikari pool을 가집니다 |
| Lifecycle | `TenantDatabaseRegistry`가 모든 tenant datasource를 소유하고 닫습니다 |
| Bootstrap | `InventorySeeder`가 tenant database마다 `inventory_items`를 만들고 서로 다른 seed row를 넣습니다 |

## 실행

```bash
./gradlew :05-database-per-tenant-spring-web:bootRun
```

```bash
curl -H 'X-Tenant-ID: acme' \
  http://localhost:8080/inventory/ACME-ROUTER-001

curl -H 'X-Tenant-ID: globex' \
  http://localhost:8080/inventory/GLOBEX-DRONE-001
```

## 테스트

```bash
./gradlew :05-database-per-tenant-spring-web:test
```

테스트는 격리된 read/write, tenant 누락/미등록 오류, fallback 금지,
병렬 request의 `ThreadLocal` 정리, rollback, tenant별 DDL bootstrap,
datasource close 동작을 검증합니다.

## CI Coverage

이 모듈은 H2-only tenant database를 사용하므로 selected examples CI에
포함합니다. 별도 Testcontainers shard는 필요하지 않습니다.
