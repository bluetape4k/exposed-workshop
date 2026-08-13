# Issue #231 `bluetape4k-dependencies 1.4.0` 중앙 버전 권위 설계

## 문제와 목표

이 저장소는 `bluetape4k-dependencies = "1.4.0"` BOM을 import하지만,
`gradle/libs.versions.toml`의 일부 직접 pin이 1.4.0 release catalog보다 낮다.
그 결과 provider BOM이 요청한 버전이 local catalog의 낮은 직접 요청에 의해
다시 선택된다.

기준선에서 확인한 예시는 다음과 같다.

| 좌표 | local pin | 1.4.0 release authority | dependencyInsight 결과 |
|---|---:|---:|---:|
| Ktor | 3.5.0 | 3.5.2 | `3.5.2 -> 3.5.0` |
| Caffeine | 3.2.3 | 3.2.4 | `3.2.4 -> 3.2.3` |
| Fory Kotlin | 1.3.0 | 1.5.0 | `1.5.0 -> 1.3.0` |
| Jackson 2 | 2.22.0 | 2.22.1 | `2.22.1 -> 2.22.0` |
| Jackson 3 | 3.2.0 | 3.2.1 | `3.2.1 -> 3.2.0` |
| HikariCP | 7.0.2 | 7.1.0 | local 7.0.2 선택 |
| PostgreSQL JDBC | 42.7.11 | 42.7.13 | local 42.7.11 선택 |
| Netty 4.2 | 4.2.15.Final | 4.2.17.Final | root dependencyManagement가 local pin 고정 |
| Redisson | 4.6.1 | 4.7.0 | provider 요청은 4.7.0으로 선택되지만 선언은 drift |
| Agroal | 3.2 | 3.2.1 | `:vertx-sqlclient-example`에서 local 3.2 선택 |
| Vert.x | 5.1.3 | 5.1.6 | `5.1.5 -> 5.1.3` |
| Hibernate ORM | 7.4.2.Final | 7.4.5.Final | Spring/Exposed 소비 모듈에서 local pin 선택 |
| Hibernate Reactive | 4.5.0.Final | 4.5.2.Final | `4.5.2.Final -> 4.5.0.Final` |
| Spring Modulith | 2.0.6 | 2.1.0 | `2.1.0 -> 2.0.6` |
| MockK | 1.14.9 | 1.14.11 | `1.14.11 -> 1.14.9` |
| Logback | 1.5.32 | 1.5.34 | `1.5.34 -> 1.5.32` |
| Zstd JNI | 1.5.7-11 | 1.5.7-12 | local pin 선택 |
| DataFaker | 2.5.4 | 2.7.0 | local pin 선택 |
| Kotlinx Benchmark | 0.4.15 | 0.4.17 | plugin/runtime local pin 선택 |
| LZ4 Java | 1.11.0 | 1.11.1 | local pin 선택 |
| Micrometer BOM | 1.16.1 | 1.17.0 | local BOM pin 선택 |
| Springdoc OpenAPI | 3.0.3 | 3.1.0 | Spring MVC/WebFlux consumers select local pin |
| Kover plugin | 0.9.8 | 0.9.9 | root coverage plugin pin |
| MySQL Connector/J | 9.7.0 | 9.7.0 | unchanged direct pin, included in governance check |
| Guava | 33.6.0-jre | 33.6.0-jre | unchanged direct pin, included in governance check |

목표는 release tag의 중앙 버전 권위를 local catalog에 반영하고, 실제 소비
모듈의 `dependencyInsight`에서 downgrade edge가 사라지는지 증명하는 것이다.
생산 코드의 동작이나 dependency coordinate를 새로 추가하지 않는다.

## 권위와 조사 근거

- 공식 release: `bluetape4k-dependencies` `1.4.0` tag와
  `gradle/libs.versions.toml`.
- 현재 root `build.gradle.kts:197-204`는 central BOM과 Netty/Jackson BOM을
  import한다.
- 현재 catalog와 release catalog의 version key/alias를 직접 비교했다.
- 대표 소비 모듈에서 `dependencyInsight`를 실행해 선언 경로와 resolved
  version을 분리했다.

## 선택한 범위

release authority와 다른 직접 pin 중 실제 catalog key가 존재하고 workshop이
직접 소비하는 다음 값을 업데이트한다. 대표 사례뿐 아니라 전수 대조에서
동일한 downgrade 또는 local 선택이 확인된 항목도 포함한다.

```text
ktor = 3.5.2
caffeine = 3.2.4
fory-kotlin = 1.5.0
jackson = 2.22.1
jackson3 = 3.2.1
hikaricp = 7.1.0
postgresql-driver = 42.7.13
netty = 4.2.17.Final
redisson = 4.7.0
agroal = 3.2.1
vertx = 5.1.6
hibernate = 7.4.5.Final
hibernate-reactive = 4.5.2.Final
spring-modulith = 2.1.0
mockk = 1.14.11
logback = 1.5.34
zstd-jni = 1.5.7-12
datafaker = 2.7.0
kotlinx-benchmark = 0.4.17
lz4-java = 1.11.1
micrometer = 1.17.0
springdoc-openapi = 3.1.0
kover = 0.9.9
```

`jackson-annotations = 2.22`는 release authority와 일치한다. 다음 값은
Spring Boot 또는 processor와의 호환성 경계가 확인될 때까지 예외로 유지한다.

| key/current | 1.4.0 release | 실제 소비 모듈 | 유지 이유와 제거 조건 |
|---|---:|---|---|
| `mariadb-java-client = 3.5.8` | 3.5.10 | `00-shared/exposed-shared-tests`, `04-exposed-ddl/01-connection`, DML/Coroutine 테스트 | Spring Boot `4.1.0` managed value와 현재 catalog가 일치한다. Boot 관리값이 release와 일치하고 영향 모듈 matrix가 통과하면 local pin을 제거한다. |
| `r2dbc-postgresql = 1.1.1.RELEASE` | 1.1.2.RELEASE | `02-alternatives-to-jpa/r2dbc-example` | Boot reactive 관리 경계와 기존 R2DBC 예제 호환성을 보존한다. R2DBC 예제가 Boot release 값으로 통과하는 시점에 제거한다. |
| `h2 = 1.4.197` (v1) | release alias 없음 | `02-alternatives-to-jpa/vertx-sqlclient-example` test runtime | Vert.x 4 legacy test 전용이다. Vert.x 5 전환 또는 모듈 제거 후 H2 v2 matrix가 통과하면 제거한다. |
| `hibernate-validator = 9.1.0.Final` | runtime 9.1.3.Final / processor 9.1.0.Final | `07-jpa/01-convert-jpa-basic`, `02-alternatives-to-jpa/hibernate-reactive-example` | runtime과 annotation processor가 하나의 local key를 공유한다. 두 key를 분리하고 processor/runtime compatibility test가 통과할 때 제거한다. |
| `springmockk = 4.0.2` | 5.0.1 | Spring Boot test consumers using `springmockk` | Boot 4 compatibility 경계를 별도 검증하지 않은 상태다. SpringMockK 5.0.1 전환 후 Spring MVC/WebFlux test matrix가 통과하면 제거한다. |
| `jackson-annotations = 2.22` | 2.22 | Jackson 2 consumers | release authority와 일치하며 downstream alias compatibility 때문에 유지한다. release alias가 동일한 호환 alias를 제공하면 재검토한다. |
| `r2dbc-pool = 1.0.2.RELEASE` | 1.0.2.RELEASE | R2DBC examples and `05-ktor-exposed-integration` | release managed value와 일치한다. release가 바뀌면 R2DBC pool compatibility matrix를 먼저 갱신한다. |

```text
mariadb-java-client = 3.5.8       # Boot 4.1.0 관리값과 일치
r2dbc-postgresql = 1.1.1.RELEASE  # Boot reactive 관리값과 일치
h2 (v1) = 1.4.197                 # Vert.x 4 호환 테스트 전용
hibernate-validator = 9.1.0.Final # runtime/annotation processor 공용 key
springmockk = 4.0.2              # Spring Boot 4 호환성 검증 전용
```

`r2dbc-pool = 1.0.2.RELEASE`는 release의 managed value와 일치한다. `fory`
core는 local direct alias가 없고 BOM이 1.5.0을 이미 선택하므로 새 pin을
추가하지 않는다. MySQL Connector/J와 Guava는 release value와 일치하지만
BOM-managed direct pin이므로 governance script의 검사 대상에 포함한다.

## 호환성·검증 설계

- Ktor: `:05-ktor-exposed-integration`의 `runtimeClasspath`에서
  `3.5.2`가 선택되고 `3.5.2 -> 3.5.0` edge가 없어야 한다.
- Caffeine: `:01-cache-strategies`의 `runtimeClasspath`에서 `3.2.4`가
  선택되어야 한다.
- Fory: `:06-spring-cache`의 `runtimeClasspath`에서 `1.5.0`이 직접
  선택되고 `selected by rule`로 1.3.0으로 내려가지 않아야 한다.
- Jackson 2/3: `:08-exposed-jackson`와 `:11-exposed-jackson3`의
  `testRuntimeClasspath`에서 각각 `2.22.1`, `3.2.1`을 선택해야 한다.
- Hikari/PostgreSQL: `:01-connection:testRuntimeClasspath`와
  `:01-dml:testRuntimeClasspath`에서 각각 `7.1.0`, `42.7.13`을 확인한다.
- Netty/Redisson: 실제 소비 configuration에서 release BOM과 local
  dependencyManagement가 같은 값으로 수렴하는지 확인한다.
- Agroal/Vert.x, Hibernate 계열, Spring Modulith, MockK, Logback, Zstd,
  DataFaker, Kotlinx Benchmark, LZ4, Micrometer, Springdoc, Kover: 각 대표 소비
  configuration 또는 plugin resolution에서
  release 값이 선택되고 기존 테스트가 유지되는지 확인한다.

각 버전 변경 뒤 영향 모듈 테스트와 전체 `detekt`/compile을 순차 실행한다.
실패하면 해당 key만 되돌리고, compatibility exception을 README/lesson에
명시한다. 범위를 넓혀 unrelated dependency를 업데이트하지 않는다.

## 문서 계약

root `README.md`와 `README.ko.md`의 Tech Stack 표 아래에 다음 규칙을
source-equivalent하게 추가한다.

- shared dependency 버전은 `bluetape4k-dependencies` release catalog를
  기준으로 선언한다.
- local pin이 필요한 경우 release 값과 이유를 `gradle/libs.versions.toml`
  주석 또는 별도 lesson에 기록한다.
- BOM이 이미 관리하는 artifact에 ad hoc version을 추가하지 않는다.

## 제외 범위

- 기능 코드, API, schema, README 예제 동작 변경.
- `bluetape4k-dependencies` upstream release 생성/수정.
- 닫힌 issue #137과 이미 별도 진행 중인 #166 FastFory 작업.
- `r2dbc-example` 신규 parity 및 provider repository 변경.

## 수용 기준과 DoD

- [x] release/current version diff 표와 변경 이유가 spec/PR에 남는다.
- [x] 위 대표 dependencyInsight 결과에서 downgrade edge가 제거된다.
- [x] Hikari/PostgreSQL/Netty/Redisson은 변경 또는 보류 이유가 명시된다.
- [x] 영향 모듈 test, compile, Detekt, `git diff --check`가 통과한다.
- [x] root README 양쪽 locale의 catalog rule이 일치한다.
- [x] public API/production feature 변경이 없다.

## Writer evidence

- `SPW-01`: release tag, local catalog, root import, consumer configuration을
  source ledger로 고정했다.
- `SPW-02`: scope, alternatives, exceptions, compatibility checks, DoD를
  포함했다.
- `SPW-03`: 한국어 기술 register와 버전/좌표 보존을 naturalness checklist로
  검토한다.
- `SPW-04`: 각 table row를 current/release catalog와 insight output에
  대조한다.
- `SPW-05`: Markdown read-back과 workflow receipt evidence를 남긴다.
