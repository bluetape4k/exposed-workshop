# JaVers + Exposed 감사 이력 예제 설계

## 문서 상태

- 대상 이슈: [#239](https://github.com/bluetape4k/exposed-workshop/issues/239)
- 대상 모듈: `13-ecosystem-integrations/12-javers-exposed-audit`
- 변경 유형: Type A Full Feature
- 저장소 범위: Exposed JDBC + H2 결정론적 예제
- 제외 범위: R2DBC 저장소 구현, 원장(source of truth) 교체, Spring Boot 통합

## 목표와 성공 기준

Exposed DAO의 생성·수정·삭제 이벤트를 JaVers 감사 기준 데이터로 기록하고, 동일한
JDBC 트랜잭션 안에서 비즈니스 데이터와 감사 데이터가 함께 커밋되거나 함께
롤백되는 흐름을 학습 가능한 코드로 제공한다.

다음 동작을 코드와 테스트로 증명한다.

1. JaVers Exposed 저장소의 스키마를 반복 실행해도 안전하게 초기화한다.
2. 고객을 생성하고 수정하면 작성자(actor), 요청 식별자(requestId), 변경 유형,
   변경 프로퍼티, 감사 이력과 diff를 조회할 수 있다.
3. 하나의 트랜잭션에서 같은 고객을 여러 번 수정해도 EntityHook이 최종 flush
   상태를 한 번만 커밋한다.
4. 업무 트랜잭션이 롤백되면 고객 행과 JaVers `javers_commit`, `javers_snapshot`
   행이 모두 남지 않는다.
5. 변경되지 않은 값을 다시 커밋해도 새로운 감사 기준 데이터나 성공 이력이 중복
   생성되지 않는다.
6. 영속 엔티티의 민감한 `secret` 필드는 감사 DTO에 포함하지 않으며, 감사 기준 데이터의
   변경 프로퍼티와 저장 상태에 노출되지 않는다.
7. 구독을 닫으면 후속 DAO 변경이 감사 기준 데이터를 만들지 않는다.

## 선택한 설계

### DAO EntityHook 경로

`bluetape4k-javers` 0.3.0의 `ExposedJaversEntityHookSubscription`을 사용한다.
이 provider는 Exposed `EntityHook` 이벤트를 관찰하고 트랜잭션이 flush한 최종
엔티티를 `javers.commit` 또는 삭제 커밋으로 기록한다. 따라서 애플리케이션
서비스가 별도로 `javers.commit`을 호출하지 않아도 직접 DAO를 변경하는 경로가
동일한 감사 정책을 따른다.

구성 순서는 다음과 같다.

1. `ExposedCdoSnapshotRepository(database).ensureSchema()`로 JaVers 테이블을
   만든다.
2. `JaversBuilder.javers().registerJaversRepository(repository).build()`로
   JaVers 인스턴스를 만든다.
3. `ExposedJaversEntityHookMapping.of(CustomerEntity)`로 감사 대상과 변환기를
   등록한다.
4. `ExposedJaversEntityHookSubscription.subscribe(...)`로 전역 hook을 연결하고,
   예제 생명주기가 끝날 때 `close()`로 해제한다.

직접 커밋을 호출하는 서비스 경로는 EntityHook을 우회할 수 있고 중복 감사 기준 데이터를
만들 수 있으므로 선택하지 않는다. EntityHook과 직접 커밋을 섞는 hybrid도 같은
변경을 두 번 기록할 위험이 있어 제외한다.

### 업무 모델과 감사 경계

업무 테이블은 다음 Exposed DAO로 최소화한다.

```text
Customers (IntIdTable)
  name   : varchar
  email  : varchar
  secret : varchar       // 업무 데이터에만 존재

CustomerEntity (IntEntity)
  toAuditObject() -> AuditedCustomer

AuditedCustomer
  @Id id : Int
  name  : String
  email : String
```

`AuditedCustomer`는 영속 엔티티와 분리된 immutable data class다. `secret`을
변환기에서 의도적으로 생략하여 감사 원장에 저장하지 않는 정책을 코드의 경계로
고정한다. 감사 DTO는 JaVers의 식별자 인식을 위해 `@Id`를 사용하고, Kotlin
기본 직렬화 규칙에 맞춰 `Serializable`을 구현한다.

### 요청 문맥과 메타데이터

`AuditContext(actor, requestId)`는 빈 값을 거부한다. `AuditContextHolder`는
현재 JDBC 스레드에 문맥을 제한적으로 보관하는 `ThreadLocal` 도우미이며,
`with(context) { transaction { ... } }` 형태로 이전 값을 복원한다.

- `authorProvider`: 현재 문맥의 `actor`
- `commitPropertiesProvider`: 현재 문맥의 `requestId`와 provider가 전달하는
  변경 유형(`Created`, `Updated`, `Removed`)
- 문맥이 없는 DAO 변경: 명시적인 오류로 실패하여 익명 감사 기록을 허용하지
  않는다.

`ThreadLocal`은 예제 범위의 동기 JDBC 트랜잭션 경계에만 사용한다. 코루틴
컨텍스트 전파나 WebFlux/R2DBC 연동은 이 이슈의 범위가 아니며, 해당 구현은
`exposed-r2dbc-workshop`에서 별도 이슈로 다룬다.

문맥 도우미는 중첩 호출에서 바깥 문맥을 복원하고, 최상위 호출이 끝나면
스레드에 값을 남기지 않는다. 모든 subscription 사용 예는 `try/finally` 또는
`use`로 닫으며, provider의 전역 hook과 동시 `close()` 조정은 이 교육 예제의
지원 범위 밖이다.

### 조회 API

워크숍이 반복해서 JaVers JQL을 작성하지 않도록 `JaversAuditHistory` 파사드를
둔다.

- `snapshots(customerId)`: `QueryBuilder.byInstanceId(customerId,
  AuditedCustomer::class.java)`로 최신순 감사 기준 데이터를 조회한다.
- `changes(customerId)`: 같은 query로 diff를 조회한다.
- `history(customerId)`: 감사 기준 데이터와 diff를 한 결과 객체로 묶어 현재 상태, 변경,
  커밋 메타데이터를 함께 확인한다.

파사드는 원장 데이터를 수정하거나 되돌리지 않는다. `rollback`은 JaVers의
과거 상태를 읽어 표시하는 예제 용어로만 사용하며, 원본 `Customers` 행에 대한
자동 복원 기능은 제공하지 않는다. 이 교육용 조회는 이력 전체를 읽는 무제한
형태이며, 운영 환경의 페이지네이션·보존 정책을 대체하지 않는다.

## 트랜잭션 및 이벤트 흐름

```text
AuditContext.with(actor, requestId)
  -> transaction {
       CustomerEntity.new / update / delete
       -> Exposed EntityHook
       -> subscription maps to AuditedCustomer
       -> JaVers commit + Exposed snapshot/commit INSERT
     }
  -> commit: business row + audit rows visible
  -> exception: both business row and audit rows rolled back
```

같은 트랜잭션의 여러 update는 provider hook의 최종 flush semantics에 맡긴다.
예제 테스트는 최종 값과 감사 기준 데이터 개수를 함께 확인하여 이 경계를 회귀로 고정한다.

## 테스트 설계

모든 테스트는 `TestDB` 공용 인프라 대신 모듈 내부 H2 JDBC 데이터베이스를
사용해 외부 Docker와 자격 증명 없이 결정론적으로 실행한다. 각 테스트는 고유한
H2 데이터베이스와 새 `ExposedCdoSnapshotRepository`/JaVers 인스턴스를 만들고
업무 테이블과 JaVers 테이블을 초기화하여 provider의 head 캐시가 테스트 사이에
재사용되지 않게 한다. hook subscription은 `try/finally`에서 `close()`한다.

| 테스트 | 검증 내용 |
| --- | --- |
| schema initialization | `ensureSchema()`와 업무 테이블 생성이 반복 실행 가능함 |
| create/update audit | actor, requestId, 변경 유형, 감사 이력, diff |
| final flush | 한 트랜잭션의 다중 수정이 최종 상태의 한 감사 기준 데이터로 기록됨 |
| rollback | 예외 이후 업무 행·commit·감사 기준 데이터가 모두 0건임 |
| duplicate commit | 값이 변하지 않은 재커밋이 새 감사 기준 데이터와 성공 이력을 만들지 않음 |
| sensitive exclusion | `secret`이 변경 프로퍼티·encoded state·저장 행에 없음 |
| subscription lifecycle | `close()` 이후 DAO 변경은 감사 기준 데이터를 추가하지 않음 |
| context lifecycle | 중첩 문맥 복원, 최상위 종료 후 제거, 예외 종료를 확인함 |

JaVers API는 provider 저장소의 `findSnapshots`, `findChanges`와
`CdoSnapshot.getPropertyValue`를 사용한다. 민감 필드 검증은 여기에 더해
`CdoSnapshotTable.state`와 `changedProperties`를 직접 읽어 저장 payload에도
`secret`이 없는지 확인한다. 테스트는 문자열 출력 전체에 의존하지 않고, 감사
기준 데이터 수·메타데이터·변경 프로퍼티·관찰 가능한 값만 단언한다.

## 의존성 및 빌드 통합

- 중앙 BOM `bluetape4k-dependencies:1.4.0`이 관리하는
  `bluetape4k-javers-bom:0.3.0`을 사용한다.
- 로컬 `gradle/libs.versions.toml`에는 BOM 관리용
  `bluetape4k-javers-exposed` alias만 추가하고 provider 버전을 중복 선언하지
  않는다.
- 모듈은 `bluetape4k-javers-exposed`, Exposed `core`/`dao`/`jdbc`, H2 runtime,
  `bluetape4k-junit5` test 의존성만 사용한다. Spring, Redis, Testcontainers는
  추가하지 않는다.
- `settings.gradle.kts`의 chapter 13 동적 include로
  `:12-javers-exposed-audit`가 등록되는지 `./gradlew projects`로 증명한다.
- `.github/workflows/examples.yml`의 변경 예제 task 목록에
  `:12-javers-exposed-audit:build`를 추가한다. H2 단일 JVM 예제이므로 Nightly
  container matrix에는 별도 행을 추가하지 않는다. 다만 Nightly의 H2 전체
  `test`와 root CI 전체 테스트에 새 project가 자동 포함되는지 검증한다.

## 문서와 시각 자료

모듈 README는 영어 `README.md`와 source-equivalent 한국어 `README.ko.md`를
제공한다. 두 문서에는 저장소 루트의 `docs/images/readme-diagrams/` 아래에
다음 자료를 포함한다.

- 아키텍처 다이어그램: `13-javers-exposed-architecture-01.svg`/`.png`와
  한국어 source-equivalent `.ko.svg`/`.ko.png`
- 트랜잭션/이벤트 순서 다이어그램: `13-javers-exposed-sequence-01.svg`/`.png`와
  한국어 source-equivalent `.ko.svg`/`.ko.png`
- `javers_commit`·`javers_snapshot`·`customers` 관계를 보여주는 ERD:
  `13-javers-exposed-erd-01.svg`/`.png`와 한국어 source-equivalent
  `.ko.svg`/`.ko.png`

SVG가 편집 가능한 source of truth이며 CairoSVG로 PNG를 렌더링한다. 다이어그램의
독자-facing 문구는 영문·한국어 asset을 source-equivalent로 제공하고, README에는
raw Mermaid를 남기지 않는다. README에는 H2 결정론적 범위, 민감 필드 제외,
R2DBC 분리 경계, 테스트 실행 명령을 명시한다. `ensureSchema()`는 교육용
편의 기능이며 실제 운영에서는 외부 migration을 소유하고
`createSchemaOnEnsure=false`와 같은 provider 옵션을 검토한다.

## 범위 밖 항목과 후속 이슈

- R2DBC repository와 coroutine context 전파: `exposed-r2dbc-workshop`의 #235
  후속 흐름에서 구현한다.
- Lettuce/Redis 영속화: 이 저장소의 JDBC 예제에 넣지 않으며 별도 #240 범위를
  유지한다.
- 실제 서비스의 권한 기반 field redaction, 복원 명령, retention/archival:
  워크숍 예제가 다루지 않는다.

## 위험과 완화

| 위험 | 완화 |
| --- | --- |
| 전역 EntityHook이 테스트 사이에 남음 | 테스트별 subscription과 `close()` 보장 |
| 문맥 없는 변경이 익명으로 기록됨 | `AuditContextHolder.requireCurrent()` 실패 |
| 영속 엔티티를 직접 감사해 `secret`이 노출됨 | detached `AuditedCustomer` 변환기 |
| JaVers와 업무 데이터의 트랜잭션 불일치 | 같은 Exposed `transaction` 안에서 hook 실행 |
| provider API가 바뀌어 예제가 깨짐 | 0.3.0 BOM alias와 API 호출을 빌드·테스트로 검증 |

## 승인된 결정

- DAO EntityHook 기반 감사 흐름을 채택한다.
- JDBC/H2 결정론적 단일 모듈로 제한한다.
- 감사 DTO에서 `secret`을 제거하는 allow-list 정책을 사용한다.
- JaVers는 조회 전용이며 source of truth 교체나 자동 rollback은 제공하지
  않는다.
