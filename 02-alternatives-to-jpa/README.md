# Alternatives to JPA for Async/Non-Blocking

JPA의 블로킹 한계를 보완하기 위해 주로 비교되는 3가지 대안은 Hibernate Reactive, R2DBC, Vert.x SQL Client 입니다. 각 접근은 추상화 수준과 런타임 모델이 다르므로, 팀의 개발 방식과 운영 환경에 맞춰 선택하는 것이 중요합니다.

## 요약 비교

| 구분       | Hibernate Reactive  | R2DBC (Spring Data R2DBC)    | Vert.x SQL Client   |
|----------|---------------------|------------------------------|---------------------|
| 추상화 수준   | ORM (JPA 유사)        | Reactive 데이터 접근 계층           | 저수준 SQL 클라이언트       |
| 프로그래밍 모델 | Mutiny/Stage 기반 비동기 | Reactive Streams (Mono/Flux) | Future/Coroutine 지원 |
| SQL 제어   | ORM 기반 (제한적 제어)     | SQL/DSL 혼합                   | SQL 직접 작성           |
| 학습 난이도   | 낮음 (JPA 경험 활용)      | 중간 (리액티브 개념 필요)              | 중간~높음 (SQL 직접 제어)   |
| 성능/유연성   | 보통                  | 좋음                           | 가장 높음               |
| 주요 사용처   | ORM 기반 도메인 모델       | Spring Reactive 스택           | 고성능/고제어 서비스         |

## 선택 가이드

- **Hibernate Reactive**: JPA 스타일을 유지하면서 비동기로 전환하고 싶을 때. 도메인 모델 중심, ORM 편의성 필요.
- **R2DBC**: Spring WebFlux 등 리액티브 스택과 잘 맞는 선택. SQL 제어와 편의성의 균형.
- **Vert.x SQL Client**: 성능과 제어가 최우선일 때. SQL 직접 작성과 비동기 실행을 선호하는 팀에 적합.

## 사용 시나리오별 추천

- **기존 JPA 코드가 많고 마이그레이션 비용을 최소화해야 한다** → Hibernate Reactive
- **Spring WebFlux + Reactor 기반 서비스** → R2DBC (Spring Data R2DBC)
- **고성능/저지연 서비스, SQL 튜닝이 핵심** → Vert.x SQL Client
- **복잡한 도메인 모델과 연관관계 매핑이 중요** → Hibernate Reactive
- **CQRS/Read 모델에서 직접 SQL을 다루고 싶다** → Vert.x SQL Client
- **DB 벤더별 특수 기능(쿼리/힌트)을 적극 활용** → Vert.x SQL Client 또는 R2DBC

## 장단점 및 주의사항

### Hibernate Reactive

**장점**

- JPA 경험을 그대로 활용 가능 (모델/매핑 중심)
- 도메인 모델 중심 설계에 적합

**단점/주의**

- 완전한 JPA 기능 커버리지가 아니다 (일부 기능 제약)
- ORM 특유의 추상화로 인해 SQL 튜닝이 제한적
- 리액티브 특성에 맞춘 트랜잭션/세션 관리가 필요

### R2DBC (Spring Data R2DBC)

**장점**

- Spring WebFlux와 자연스럽게 통합
- Repository 기반 개발과 커스텀 SQL 병행 가능
- 비교적 가벼운 추상화로 성능/유연성 균형

**단점/주의**

- JPA와 유사한 연관관계 매핑이 제한적
- 트랜잭션/세션 관리 패턴이 JPA와 다름
- 드라이버 품질과 DB별 기능 차이를 고려해야 함

### Vert.x SQL Client

**장점**

- 가장 높은 성능과 SQL 제어권
- 비동기 IO 모델이 단순하고 직관적
- 벤더별 SQL 기능을 자유롭게 사용 가능

**단점/주의**

- ORM 편의 기능이 없어서 보일러플레이트 증가
- 도메인 모델과 DB 모델 간 매핑을 직접 관리해야 함
- 팀에 SQL 숙련도가 필요

## 발표 자료

* [Alternatives to JPA 2025](https://drive.google.com/file/d/1-ISsBzdfxDufRlYpGOAW-Abed1sTqWJj/view)

## 블로그 글

* [Async/Non-Blocking](https://debop.notion.site/1ad2744526b080708385fce6531752c7?v=1ad2744526b081ae899a000c87c870a1)
    * [Async/Non-Blocking 지원 DB 라이브러리](https://debop.notion.site/Async-Non-Blocking-DB-1ad2744526b080608767e69344793e60)
    * [Hibernate Reacitve 소개](https://debop.notion.site/Hibernate-Reactive-1b92744526b080eb8d1dfd93654a16b3)
    * [Vert.x SQL Client 소개](https://debop.notion.site/Vert-x-SQL-Client-1ad2744526b08072b431f5b00e0874d9)
    * [Spring Data R2DBC 소개](https://debop.notion.site/R2DBC-Spring-Data-R2DBC-1ad2744526b080adadc7c737672f32a1)
