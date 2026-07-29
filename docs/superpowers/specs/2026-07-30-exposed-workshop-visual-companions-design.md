# exposed-workshop 대표 예제 Visual Companion 설계

## 목적

`exposed-workshop`의 캐시 전략과 DDD 예제는 여러 저장소, 트랜잭션, 캐시 계층,
모듈 경계를 함께 이해해야 한다. README와 소스만으로도 내용을 확인할 수 있지만,
처리 순서와 상태 변화를 한 화면에서 비교하기는 어렵다.

이 작업은 다음 두 Visual Companion을 제공한다.

1. 캐시 조회·갱신·비동기 저장 방식에 따라 Redis와 DB의 데이터 반영 시점이 달라진다.
2. 주문은 자기 저장소에 저장하고 배송은 공개된 이벤트만 받아 자기 저장소에 배송 예약을 만든다.

두 자료는 예제의 제작 배경, 설계 방향, 구현 구조, 실행 방법, 검증 결과, 적용 효과와
주의사항을 실제 README, 설계 문서, 소스, 테스트, 관련 기술 블로그에 연결한다.

## 공통 원칙

- 한국어와 영어 문서를 함께 제공한다.
- 한국어는 국내 소프트웨어 기술문서에서 통용되는 용어를 사용한다.
- 설명은 예제의 문제, 구현 근거, 실행 결과, 적용 조건 순서로 전개한다.
- 추상적인 선언보다 어떤 구성 요소가 어떤 상태를 변경하는지 구체적으로 적는다.
- 클래스명, 메서드명, 설정 키, Gradle 명령은 원문을 유지한다.
- `light`, `dark`, `auto` 테마를 지원하고 `starlight-theme` 선택을 저장한다.
- 각 문서는 독립 실행 가능한 HTML이며 외부 CSS, JavaScript, 이미지 파일에 의존하지 않는다.
- 소스 기준 커밋은 40자리 Git commit으로 고정한다.
- 게시본은 원본 저장소의 매니페스트와 사이트 스냅숏 검증을 모두 통과해야 한다.

## 자료 1: Exposed + Redis 캐시 전략

### 제목

한국어:

> 캐시 조회·갱신·비동기 저장 방식에 따라 Redis와 DB의 데이터 반영 시점이 달라진다

영어:

> Cache Read, Update, and Deferred Persistence Strategies Change When Redis and the Database Converge

### 대표 예제로 선택한 이유

`11-high-performance/01-cache-strategies`는 하나의 실행 가능한 Spring MVC 예제에서
다음 전략을 모두 비교한다.

- `UserCacheRepository`: Read-Through + Write-Through
- `UserCredentialsCacheRepository`: Read-Only + 명시적 무효화
- `UserEventCacheRepository`: Write-Behind

각 전략은 `AbstractJdbcRedissonRepository`를 공통 기반으로 사용한다. 따라서 업무 데이터의
변경 빈도와 DB 반영 시점에 따라 설정과 테스트가 어떻게 달라지는지 같은 구조 안에서 비교할 수
있다. `11-high-performance/02-cache-strategies-coroutines`는 같은 전략을 WebFlux,
코루틴, `newSuspendedTransaction` 환경에서 구현하므로 런타임 차이를 설명하는 비교 근거로
사용한다.

### 설명할 문제

캐시를 추가하면 조회 지연 시간을 줄일 수 있지만, 다음 문제가 함께 생긴다.

- 캐시 미스가 발생했을 때 DB 조회와 캐시 적재를 누가 수행하는가
- 변경 데이터를 Redis와 DB에 어느 순서로 반영하는가
- Write-Behind가 요청 수락과 DB 반영 완료를 분리할 때 무엇을 성공으로 판단하는가
- 여러 애플리케이션 인스턴스의 로컬 캐시를 어떻게 무효화하는가
- Redis 장애나 재연결 뒤 오래된 로컬 값이 남지 않도록 어떤 정책을 적용하는가
- 캐시 무효화가 DB 행 삭제로 이어지지 않도록 어떤 경계를 두는가

### 구조와 필요성

화면은 다음 계층을 한 줄의 처리 경로로 표시한다.

`HTTP 요청 → JdbcCacheRepository → RLocalCachedMap → Redis → EntityMapLoader/Writer → Exposed Table`

Near Cache를 적용하면 각 애플리케이션 인스턴스의 메모리가 L1, Redis가 L2 역할을 한다.
동일 키를 반복 조회할 때 네트워크 왕복을 줄일 수 있지만, 로컬 값의 무효화와 재연결 정책을
설계해야 한다. 그래서 단순한 인메모리 맵과 Redis 조합이 아니라 Redisson
`RLocalCachedMap`의 동기화 기능을 사용한다.

### Exposed + Redis 구현 경로

Visual Companion은 다음 구현 관계를 소스 링크와 함께 표시한다.

1. 예제 저장소가 `AbstractJdbcRedissonRepository`를 상속한다.
2. `RedissonCacheConfig.isNearCacheEnabled`가 `true`이면 `RLocalCachedMap`을 생성한다.
3. `EntityMapLoader`는 캐시 미스 시 Exposed `transaction {}` 안에서 DB를 조회한다.
4. `EntityMapWriter`는 Write-Through 또는 Write-Behind 설정에 따라 Exposed
   `transaction {}` 안에서 DB 쓰기를 수행한다.
5. Read-Only 설정은 writer를 만들지 않는다.
6. `invalidate`, `invalidateAll`, `clear`는 기본 설정에서 writer가 없는
   `cacheOnlyMap`을 사용해 캐시만 제거하고 DB 행은 유지한다.
7. Near Cache를 사용하면 원격 키 제거 뒤 현재 인스턴스의 로컬 캐시도 정리한다.

구현 설명에는 다음 설정을 포함한다.

- `READ_WRITE_THROUGH_WITH_NEAR_CACHE`
- `READ_ONLY_WITH_NEAR_CACHE`
- `WRITE_BEHIND_WITH_NEAR_CACHE`
- `nearCacheSyncStrategy`
- `ttl`
- `nearCacheMaxIdleTime`
- `writeRetryAttempts`
- `writeRetryInterval`
- `deleteFromDBOnInvalidate`

### 전략별 상호작용

사용자는 전략과 상황을 선택할 수 있다.

| 전략 | 선택 가능한 상황 | 화면에서 보여 줄 결과 |
| --- | --- | --- |
| Read-Through | L1 적중, Redis 적중, 전체 미스 | 조회가 끝나는 계층과 DB 조회·캐시 적재 여부 |
| Write-Through | 사용자 정보 변경 | Redis와 DB 반영 완료 시점 |
| Read-Only | 인증 정보 조회·무효화 | writer 부재, 무효화 뒤 DB 재조회 |
| Write-Behind | 단일 이벤트, 10,000건 일괄 저장 | 요청 수락, Redis 적재, DB 반영 완료를 분리한 시간축 |

각 상태 변화는 Near Cache, Redis, DB 칸에 같은 키의 현재 값을 표시한다. Write-Behind는
DB 반영이 지연되는 구간을 별도 상태로 표시하며, Redis 적재만 끝난 상태를 DB 반영 완료로
표현하지 않는다.

### 필요성 및 효과

- Read-Through는 캐시 미스 처리와 DB 조회 코드를 저장소 경계로 모은다.
- Write-Through는 변경 요청이 끝날 때 Redis와 DB가 같은 값을 갖도록 처리한다.
- Read-Only는 변경되지 않는 데이터에 불필요한 DB 쓰기 경로를 만들지 않는다.
- Write-Behind는 대량 이벤트의 요청 처리와 DB 일괄 저장을 분리한다.
- Near Cache는 동일 인스턴스의 반복 조회에서 Redis 네트워크 왕복을 줄인다.
- 공통 `JdbcCacheRepository` 계약으로 직접 DB 조회, 캐시 조회, 저장, 무효화를 구분한다.

성능 효과는 테스트의 상대 비교와 기존 블로그의 검증 범위 안에서만 설명한다. 현재 예제에
재현 가능한 벤치마크 결과가 없으면 구체적인 처리량이나 지연 시간 수치를 만들지 않는다.

### 주의할 점

- Cache-aside와 Redisson loader/writer 기반 Read/Write-Through를 같은 전략으로 설명하지 않는다.
- Near Cache는 Redis 장애를 제거하지 않는다. 로컬 값의 신선도와 재연결 뒤 동기화 정책을
  함께 검증해야 한다.
- Write-Behind는 요청 수락과 DB 반영 완료가 다르다. 유실을 허용할 수 없는 업무 데이터에는
  복구 기록 없이 적용하지 않는다.
- `timeForCache <= timeForDB` 테스트는 해당 실행에서의 상대 비교이며 운영 성능 보장이 아니다.
- 무효화는 기본적으로 캐시만 제거한다. `deleteFromDBOnInvalidate`를 변경하면 삭제 의미도
  달라지므로 별도 검증이 필요하다.
- 로컬 H2와 Testcontainers Redis 검증 결과를 실제 PostgreSQL 운영 환경의 결과로 확대 해석하지
  않는다.

### 실제 실행 설명

```bash
./gradlew :01-cache-strategies:test
./gradlew :01-cache-strategies:bootRun
./gradlew :02-cache-strategies-coroutines:test
```

문서에는 Redis가 Testcontainers로 시작되고, 테스트가 캐시 적중·미스, DB 직접 조회,
명시적 무효화, 10,000건 Write-Behind의 최종 DB 반영을 확인한다는 점을 적는다.
애플리케이션 실행 시 사용할 HTTP 경로도 README와 일치하게 제공한다.

### 근거 자료

- `11-high-performance/README.ko.md`
- `11-high-performance/01-cache-strategies/README.ko.md`
- `11-high-performance/02-cache-strategies-coroutines/README.ko.md`
- `UserCacheRepository.kt`
- `UserCredentialsCacheRepository.kt`
- `UserEventCacheRepository.kt`
- 각 저장소의 통합 테스트
- `bluetape4k-exposed`의 `JdbcCacheRepository.kt`
- `bluetape4k-exposed`의 `AbstractJdbcRedissonRepository.kt`
- `bluetape4k-exposed`의 `EntityMapLoader.kt`, `EntityMapWriter.kt`
- `bluetape4k.github.io`의 캐시 시리즈 Part 3, Part 4
- `bluetape4k.github.io`의 Exposed 시리즈 Part 5

## 자료 2: DDD와 Modulith 경계 검증

### 제목

한국어:

> 주문은 자기 저장소에 저장하고 배송은 공개된 이벤트만 받아 자기 저장소에 배송 예약을 만든다

영어:

> Orders Persist Their Own State and Shipping Creates Reservations Only from the Published Event

### 대표 예제로 선택한 이유

`13-ecosystem-integrations/08-ddd-modulith-boundaries`는 DDD 개념을 패키지 이름으로만
표현하지 않는다. 다음 내용을 실행 가능한 테스트로 검증한다.

- `orders`와 `shipping`이 각자 Exposed 테이블과 저장소를 소유한다.
- `orders.events`만 공개 인터페이스가 된다.
- `shipping`은 `orders :: events`만 참조할 수 있다.
- 주문 접수 이벤트가 배송 예약을 생성한다.
- `shipping → orders.internal` 직접 참조는 `ApplicationModules.verify()`에서 실패한다.

즉, 정상적인 이벤트 흐름과 금지된 컴파일 시점 참조를 같은 테스트 모듈에서 비교할 수 있다.
H2만 사용하므로 외부 인증 정보 없이 실행할 수 있다는 점도 워크숍 대표 자료로 적합하다.

### DDD가 필요한 상황

테이블 중심 코드에서 여러 업무가 같은 저장소와 엔티티를 직접 변경하기 시작하면 다음 문제가
발생한다.

- 어떤 모듈이 상태 변경을 책임지는지 불분명해진다.
- 다른 업무 모듈이 내부 테이블과 저장소를 직접 참조한다.
- 한 모듈의 스키마 변경이 관련 없는 모듈까지 수정하게 만든다.
- 후속 작업을 원래 트랜잭션 안에 계속 추가해 처리 범위가 커진다.
- 문서에 적은 모듈 경계와 실제 코드 참조가 달라져도 빌드가 이를 검출하지 못한다.

DDD는 업무 규칙과 상태 변경 책임을 bounded context와 애그리거트에 배치한다. 이 예제는
Spring Modulith 검증을 추가해 그 설계를 실행 가능한 코드 규칙으로 만든다.

### 구조와 처리 흐름

정상 흐름은 다음 순서로 표시한다.

1. `AcceptOrderCommand`가 `OrderApplicationService`에 전달된다.
2. `orders` 트랜잭션이 `ExposedOrderRepository`로 주문을 저장한다.
3. `OrderAcceptedEvent`를 `orders.events` 공개 인터페이스로 발행한다.
4. `ShippingReservationHandler`가 이벤트를 수신한다.
5. `shipping` 트랜잭션이 `ExposedShippingReservationRepository`로 배송 예약을 저장한다.

두 모듈의 테이블과 트랜잭션은 별도 영역으로 표시한다. 이벤트 전달은 허용된 연결선으로,
`shipping → orders.internal` 직접 참조는 차단된 연결선으로 표시한다.

### DDD 적용 효과

- 주문 상태 변경 책임과 배송 예약 책임을 분리한다.
- 각 bounded context가 자기 테이블과 저장소를 변경한다.
- 다른 모듈은 공개 이벤트 계약만 참조하므로 내부 구현을 변경하기 쉬워진다.
- 이벤트 발행으로 후속 작업을 원래 명령 처리 코드에서 분리한다.
- `ApplicationModules.verify()`가 허용되지 않은 참조를 테스트에서 검출한다.
- 정상 흐름과 위반 픽스처를 함께 두어 설계 규칙이 실제로 작동함을 증명한다.

### 주의할 점

- 패키지를 나누는 것만으로 DDD가 적용되지는 않는다. 상태 변경 책임과 공개 계약이 함께
  분리되어야 한다.
- 이 예제의 `@EventListener`는 같은 애플리케이션 프로세스 안에서 동작한다. 외부 서비스나
  브로커 전달의 내구성을 보장하지 않는다.
- 주문 저장 뒤 이벤트 발행 사이의 실패 복구, 트랜잭셔널 아웃박스, 이벤트 중복 처리,
  멱등성은 이 예제의 범위가 아니다.
- bounded context마다 테이블과 트랜잭션을 분리하면 매핑과 이벤트 계약 코드가 늘어난다.
  업무 규칙이 단순한 CRUD라면 이 구조가 불필요할 수 있다.
- `ApplicationModules.verify()`는 코드 참조 규칙을 검증하지만 업무 규칙 자체의 정확성을
  대신 검증하지 않는다.

### 상호작용

사용자는 다음 두 시나리오를 전환할 수 있다.

1. 정상 처리
   - 주문 행 생성
   - 이벤트 발행
   - 배송 예약 행 생성
   - `ApplicationModules.verify()` 통과
2. 경계 위반
   - `shipping`이 `orders.internal.LeakyOrderRepository`를 직접 참조
   - 허용된 `orders :: events` 계약을 우회
   - `Violations` 발생

화면은 현재 단계, 실행 중인 트랜잭션, 생성된 DB 행, 검증 결과를 함께 표시한다.

### 실제 실행 설명

```bash
./gradlew :08-ddd-modulith-boundaries:test
```

문서에는 다음 테스트 결과를 설명한다.

- 정상 애플리케이션 모듈 구성이 검증을 통과한다.
- 잘못된 내부 저장소 참조가 `Violations`로 검출된다.
- 주문 접수 뒤 `ddd_modulith_orders`에 행이 저장된다.
- 이벤트 처리 뒤 `ddd_modulith_shipping_reservations`에 행이 저장된다.
- 테스트는 로컬 H2에서 실행되며 외부 서비스에 연결하지 않는다.

### 근거 자료

- `docs/superpowers/specs/2026-06-30-issue-145-ddd-modulith-boundaries-design.md`
- `docs/superpowers/plans/2026-06-30-issue-145-ddd-modulith-boundaries-plan.md`
- `docs/lessons/2026-06-30-issue-145-ddd-modulith-boundaries.md`
- `13-ecosystem-integrations/08-ddd-modulith-boundaries/README.ko.md`
- `OrderApplicationService.kt`
- `orders/events/OrderAcceptedEvent.kt`
- `shipping/ShippingReservationHandler.kt`
- `BoundaryVerificationApplicationTest.kt`
- `bluetape4k.github.io`의 DDD 워크숍 및 Spring Modulith 관련 글

## 화면 구성

두 문서는 같은 화면 구조를 사용한다.

1. 제목과 현재 소스 기준 커밋
2. 예제 시나리오와 선택 근거
3. 구조 및 필요성
4. 상호작용 가능한 처리 흐름
5. 구현 클래스와 설정
6. 적용 효과
7. 주의할 점과 예제가 검증하지 않는 범위
8. 실제 실행 명령과 예상 결과
9. 원본 README, 설계 문서, 소스, 테스트 링크

첫 화면에는 제목과 핵심 처리 흐름을 배치하고, 다음 내용의 일부가 보이도록 구성한다.
페이지 섹션을 카드처럼 띄우지 않으며 반복 항목과 실제 도구 영역에만 제한적으로 테두리를 사용한다.

## 내비게이션과 테마

- 저장소 안의 두 Visual Companion을 이전/다음 링크로 연결한다.
- 한국어와 영어 문서는 같은 문서 ID와 순서를 사용한다.
- 사이트에서 열었을 때 카탈로그, 저장소 소개, 다른 Visual Companion으로 이동할 수 있다.
- 직접 HTML을 열어도 언어 전환과 두 문서 사이 이동이 동작한다.
- 테마 버튼은 `light`, `dark`, `auto`를 제공하고 접근성 이름을 포함한다.
- `auto`는 운영체제 테마를 추종하며 선택값은 `starlight-theme`에 저장한다.

## 원본 저장소 계약

`docs/visual-companions/manifest.json`에 두 문서를 등록한다.

- `exposed-redis-cache-strategies`
- `ddd-modulith-boundaries`

검증 스크립트는 다음 항목을 검사한다.

- 매니페스트 스키마와 저장소 이름
- 한국어·영어 문서의 존재와 순서
- source와 40자리 baseline
- `light`, `dark`, `auto` 초기화와 저장
- 접근 가능한 테마 및 언어 전환 버튼
- 독립 실행을 방해하는 외부 리소스와 네트워크 API 사용 금지
- 실행 명령, source 링크, 테스트 링크
- 문서 간 이전/다음 내비게이션

## 사이트 게시 계약

사이트의 기존 Visual Companion 계약이 배포된 뒤 후속 변경으로 진행한다.

1. `src/data/visual-companions/repositories.json`에
   `bluetape4k/exposed-workshop`과 정확한 `sourceRef`를 등록한다.
2. 원본 매니페스트와 HTML을 수정불가한 스냅숏으로 복사한다.
3. 한영 카탈로그에 저장소 설명과 두 문서 요약을 추가한다.
4. `/visual-companions/exposed-workshop/...`와
   `/ko/visual-companions/exposed-workshop/...` 경로를 검증한다.
5. Examples 페이지의 대표 설계 영역에서 exposed-workshop 자료를 찾을 수 있게 한다.

현재 `bluetape4k.github.io` PR #302가 이 다중 저장소 계약을 도입한다. 이 PR이 병합되기
전에는 사이트 후속 변경을 별도 기반으로 만들거나 기존 PR에 섞지 않는다.

## 검증

### 원본 저장소

- 두 예제의 대상 테스트
- Visual Companion 검증 스크립트
- 한국어·영어 내용과 링크 일치
- `git diff --check`
- Playwright 데스크톱·모바일 검증
- `light`, `dark`, `auto` 전환과 저장
- 캐시 전략 선택, DDD 정상·위반 시나리오 상호작용
- 브라우저 콘솔 오류 0건

### 사이트

- `npm test`
- `npm run check:visual-companions`
- `npm run build`
- 한영 카탈로그와 Examples 페이지
- 새로 추가한 네 개 게시 경로
- 데스크톱·모바일에서 테마와 상호작용
- 배포 뒤 실제 경로와 스냅숏의 `sourceRef`

## 비목표

- 캐시 구현이나 DDD 예제의 동작 변경
- 새로운 벤치마크 수치 생성
- Redis 장애 복구 기능 또는 트랜잭셔널 아웃박스 추가
- Spring Modulith 발행 기록 예제와 DDD 경계 예제를 하나의 실행 흐름으로 결합
- Visual Companion 안에서 실제 Redis, H2, Gradle 작업 실행

## 완료 조건

- 두 Visual Companion이 한국어와 영어로 제공된다.
- 사용자가 요청한 필요성, 효과, 구현 방법, Near Cache, 주의사항, DDD 선택 근거를
  모두 실제 근거에 연결한다.
- 한국어 전체 문장이 `bluetape-writer` 기술문서 기준을 통과한다.
- 원본 저장소와 사이트 검증 스크립트가 `sourceRef`와 한영 문서 일치를 검증한다.
- 대상 테스트, 브라우저 검증, 사이트 빌드가 통과한다.
- PR 병합과 배포는 exact head 기준의 별도 승인 뒤에만 진행한다.
