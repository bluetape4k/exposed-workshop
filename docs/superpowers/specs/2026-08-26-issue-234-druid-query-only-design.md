# Apache Druid Query-Only Exposed 예제 설계

## 목표

이슈 [#234](https://github.com/bluetape4k/exposed-workshop/issues/234)의 목표는 Apache Druid를 분석 조회 전용 데이터 소스로 사용하는 독립적인 workshop 모듈을 추가하고, 중앙 dependency catalog와 `bluetape4k-exposed:1.12.1` provider API의 실제 사용 경로를 고정하는 것이다.

기본 테스트는 외부 Druid, 네트워크, 자격 증명 없이 결정적으로 실행한다. 실제 Druid smoke test는 `EXPOSED_DRUID_SMOKE=true`일 때만 활성화한다.

## 현재 근거

- 저장소의 중앙 버전은 `bluetape4k-dependencies:1.4.0`이며 `gradle/libs.versions.toml`에 `exposed-druid` alias가 아직 없다.
- provider `bluetape4k-exposed:1.12.1`은 `DruidConnectionOptions`, `DruidJdbc.query`, `DruidJdbc.querySuspend`, `DruidJdbc.listColumns`를 제공한다.
- provider의 허용 query는 `SELECT`, `WITH`, `EXPLAIN`, `DESCRIBE`, `SHOW`로 시작하는 SQL과 `INFORMATION_SCHEMA.COLUMNS` metadata 조회다.
- provider는 DDL, DML, DAO, repository, migration, Exposed `Database`/dialect를 의도적으로 노출하지 않는다.
- 공식 provider 문서: <https://github.com/bluetape4k/bluetape4k-exposed/blob/1.12.1/exposed/druid/README.md>
- 인접 예제 `01-bigquery-dry-run`, `02-trino-session-options`, `09-duckdb-embedded-analytics`는 타입화한 profile, Korean KDoc, MockK 또는 local-first 검증, English/Korean README 패턴을 사용한다.

## 설계 선택지와 결정

### 선택지 A — `DruidJdbc` 직접 래핑과 타입화한 profile (채택)

`DruidQueryProfile`이 endpoint, datasource, schema, context property를 보존하고 `DruidConnectionOptions`로 변환한다. workshop 함수는 provider의 `DruidJdbc.query`, `querySuspend`, `listColumns`를 직접 호출한다. 테스트는 MockK object mocking으로 provider 호출을 캡처하여 URL, property, SQL, mapper 결과를 검증한다.

이 선택은 provider API 자체가 학습 대상이라는 점을 보존하고, 새로운 추상화와 실행 인프라를 추가하지 않는다. 한 명의 개발자가 유지하는 예제에 필요한 최소 경계만 남긴다.

### 선택지 B — 주입형 Druid adapter 인터페이스

provider 호출을 별도 adapter 인터페이스로 감싸면 테스트 격리는 쉬워지지만, adapter가 provider API의 사용법을 가리고 예제 코드가 실제 호출 경로와 달라진다. 이슈가 요구하지 않는 추상화이므로 채택하지 않는다.

### 선택지 C — 로컬 Avatica/Druid 서버 또는 Testcontainers

실제 wire 경로를 검증할 수 있으나 이미지, 네트워크, 기동 시간, 운영 자격 증명 경계가 필요하다. 기본 테스트의 결정성과 단일 개발자 실행 비용을 해치므로 opt-in smoke test 외에는 사용하지 않는다.

## 모듈과 공개 예제 API

새 모듈 경로는 `13-ecosystem-integrations/10-druid-query-only`다. `settings.gradle.kts`의 chapter 13 자동 module discovery를 사용하므로 별도 include를 추가하지 않는다.

예상 source 파일은 `src/main/kotlin/exposed/examples/druid/queryonly/DruidQueryOnlyWorkshop.kt`다.

```kotlin
data class DruidQueryProfile(
    val avaticaEndpoint: String = "http://localhost:8888/druid/v2/sql/avatica/",
    val datasource: String = "wikipedia",
    val schema: String = "druid",
    val contextProperties: Map<String, String> = mapOf("sqlTimeZone" to "Etc/UTC"),
) : Serializable {
    fun toConnectionOptions(): DruidConnectionOptions
}

fun queryDatasourceRowCount(profile: DruidQueryProfile): List<Long>

suspend fun queryDatasourceRowCountSuspend(profile: DruidQueryProfile): List<Long>

fun listDatasourceColumns(profile: DruidQueryProfile): List<DruidColumnMetadata>
```

구현 규칙은 다음과 같다.

- profile 생성 시 `requireNotBlank`로 endpoint 외 식별자와 map key/value를 검증한다. endpoint 형식 검증은 provider `DruidConnectionOptions`에 위임한다.
- query-only SQL은 읽기 목적의 명명된 sample query로 제한한다. datasource 이름을 SQL에 삽입할 때는 영숫자와 `_`만 허용하는 식별자 검증을 적용하여 예제 문자열을 안전하게 만든다.
- 동기 함수는 `DruidJdbc.query`의 row mapper를 사용하고, suspend 함수는 `DruidJdbc.querySuspend`를 사용한다. 두 함수는 같은 조회 의미를 유지한다.
- metadata 함수는 profile의 `datasource`, `schema`, `DruidConnectionOptions`를 `DruidJdbc.listColumns`에 전달한다.
- 연결을 직접 열거나 `Database.connect`, DDL/DML, DAO, repository, migration, batch abstraction을 추가하지 않는다.

## 의존성과 모듈 구성

`gradle/libs.versions.toml`에 다음 BOM 기반 alias를 추가한다.

```toml
exposed-druid = { module = "io.github.bluetape4k.exposed:bluetape4k-exposed-druid" }
```

모듈 `build.gradle.kts`는 `implementation(libs.exposed.druid)`와 기존 test convention의 `bluetape4k.junit5`, `mockk`를 사용한다. provider가 coroutine API를 노출하므로 suspend 테스트에 기존 `kotlinx-coroutines-test` dependency를 사용하고 새 dependency는 추가하지 않는다.

## 테스트 설계

`DruidQueryOnlyWorkshopTest.kt`는 다음을 테스트한다.

1. 기본 profile이 `DruidConnectionOptions` URL, transparent reconnection, context property를 올바르게 만든다.
2. blank datasource/schema와 잘못된 profile 값은 JDBC 호출 전에 실패한다.
3. MockK로 `DruidJdbc.query`를 캡처하여 row mapper 결과와 query-only sample SQL을 검증한다.
4. MockK의 `coEvery`로 `DruidJdbc.querySuspend`를 캡처하여 suspend 결과가 동기 의미와 일치하는지 검증한다.
5. `DruidJdbc.listColumns` 호출의 datasource/schema/options와 `DruidColumnMetadata` 결과를 검증한다.
6. provider query-only validator가 빈 SQL과 `INSERT`/`CREATE` 같은 비조회 SQL을 거부하는지 검증한다.

실서비스 smoke test는 별도 `DruidQueryOnlySmokeTest.kt`에 둔다. `@EnabledIfEnvironmentVariable(named = "EXPOSED_DRUID_SMOKE", matches = "true")`를 사용하고 endpoint, datasource, schema, user/password는 환경 변수에서만 읽는다. 기본 Gradle test에서는 실행되지 않으며 저장소에 자격 증명이나 운영 endpoint를 기록하지 않는다.

## 문서와 다이어그램

모듈에는 source-equivalent인 다음 문서를 추가한다.

- `13-ecosystem-integrations/10-druid-query-only/README.md`
- `13-ecosystem-integrations/10-druid-query-only/README.ko.md`

두 README는 profile 구성, 동기/suspend query, `listColumns`, query-only 제외 범위, 기본 테스트 명령, opt-in smoke 명령을 같은 순서와 의미로 설명한다. README는 PNG만 embed한다.

정적 architecture 다이어그램은 SVG를 source로 삼고 English/Korean 자산을 각각 렌더링한다.

- `docs/images/readme-diagrams/13-druid-query-only-architecture-01.svg`
- `docs/images/readme-diagrams/13-druid-query-only-architecture-01.png`
- `docs/images/readme-diagrams/13-druid-query-only-architecture-01.ko.svg`
- `docs/images/readme-diagrams/13-druid-query-only-architecture-01.ko.png`

다이어그램의 독자 질문은 “타입화한 profile과 query-only facade가 외부 Druid Avatica endpoint까지 어떤 책임 경계로 이어지는가?”다. 카드와 연결은 source의 `DruidQueryProfile`, `DruidConnectionOptions`, `DruidJdbc` 세 API와 local deterministic test/mock, 명시적 opt-in smoke 경계를 나타낸다. SVG는 CairoSVG scale 2로 PNG를 만들고 XML, semantic, connector, arrowhead, geometry, visual, asset-pair 감사를 통과시킨다.

## 워크플로와 범위 경계

- `.github/scripts/select-changed-examples.sh`의 고정 `ALL_TASKS`에 `:10-druid-query-only:build`를 추가한다. chapter 13 path mapping은 기존 동적 규칙을 사용한다.
- chapter 13 English/Korean README 예정 예제 표에 이슈 #234, 모듈 경로, `:10-druid-query-only:build`, query-only 제목을 추가한다.
- workflow lane은 Weekly Examples이며 외부 Druid smoke는 해당 lane에 포함하지 않는다.
- 기본 경로는 local/fake(MockK)이고, real-service 경로는 `EXPOSED_DRUID_SMOKE=true`인 수동 opt-in이다.
- provider 버전과 중앙 BOM 버전은 각각 이슈에 명시된 `1.12.1`과 `1.4.0` contract를 따른다. 저장소에서 provider 버전을 임의로 override하지 않는다.

## 수용 기준

- alias, 모듈, source, 테스트가 `bluetape4k-dependencies:1.4.0`에서 해석된다.
- 동기·suspend query와 column metadata 경로가 외부 Druid 없이 테스트된다.
- 잘못된 endpoint/profile/비조회 SQL 경계가 명시적으로 실패한다.
- smoke test는 opt-in 없이는 비활성이고 자격 증명은 환경 변수 밖에 없다.
- English/Korean README가 source-equivalent이며 architecture SVG/PNG pair가 존재한다.
- 모듈 test, detekt/static 검사, changed-examples workflow 선택 검사가 통과한다.

## 리뷰 렌즈 기록

단일 개발자 운영 제약에 따라 아래 여섯 렌즈를 같은 세션에서 순차 점검한다.

| 렌즈 | 확인 내용 |
|---|---|
| API/아키텍처 | provider API를 가리지 않는 최소 facade와 query-only 경계 |
| Kotlin 패턴 | immutable data class, null-safety, `requireNotBlank`, suspend 시그니처, Korean KDoc |
| 테스트 | 외부 서비스 없는 deterministic MockK, RED→GREEN, opt-in smoke 격리 |
| 보안/운영 | credential·운영 endpoint 미저장, identifier 검증, smoke 명시성 |
| 문서/다이어그램 | English/Korean source parity, PNG embed, SVG/PNG 감사 가능성 |
| 범위/워크플로 | chapter 13 등록, Examples task, DDL/DML 등 제외, single-owner 순차 실행 |
