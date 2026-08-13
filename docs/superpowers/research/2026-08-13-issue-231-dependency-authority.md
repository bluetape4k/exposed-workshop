# Issue #231 버전 권위 조사

## 기준

현재 `gradle/libs.versions.toml`과 공식
[`bluetape4k-dependencies 1.4.0 catalog`](https://github.com/bluetape4k/bluetape4k-dependencies/blob/8a738f084de98323b5651c548b9d2c354fb22329/gradle/libs.versions.toml)을 비교했다.
root `build.gradle.kts`는 `bluetape4k-dependencies`, Netty, Jackson 2/3 BOM을
import한다.

## 변경 대상

| key | 현재 | release | 근거 |
|---|---:|---:|---|
| `ktor` | 3.5.0 | 3.5.2 | `:05-ktor-exposed-integration`에서 `3.5.2 -> 3.5.0` |
| `caffeine` | 3.2.3 | 3.2.4 | `:01-cache-strategies`에서 `3.2.4 -> 3.2.3` |
| `fory-kotlin` | 1.3.0 | 1.5.0 | `:06-spring-cache`에서 `1.5.0 -> 1.3.0` |
| `jackson` | 2.22.0 | 2.22.1 | `:08-exposed-jackson`에서 `2.22.1 -> 2.22.0` |
| `jackson3` | 3.2.0 | 3.2.1 | `:11-exposed-jackson3`에서 `3.2.1 -> 3.2.0` |
| `hikaricp` | 7.0.2 | 7.1.0 | `:01-connection`에서 7.0.2 선택 |
| `postgresql-driver` | 42.7.11 | 42.7.13 | `:01-dml`에서 42.7.11 선택 |
| `netty` | 4.2.15.Final | 4.2.17.Final | root dependencyManagement direct list |
| `redisson` | 4.6.1 | 4.7.0 | provider request already resolves 4.7.0 |
| `agroal` | 3.2 | 3.2.1 | `:vertx-sqlclient-example`에서 3.2 선택 |
| `vertx` | 5.1.3 | 5.1.6 | `5.1.5 -> 5.1.3` |
| `hibernate` | 7.4.2.Final | 7.4.5.Final | benchmark/ORM 소비 모듈에서 local 선택 |
| `hibernate-reactive` | 4.5.0.Final | 4.5.2.Final | `4.5.2.Final -> 4.5.0.Final` |
| `spring-modulith` | 2.0.6 | 2.1.0 | `2.1.0 -> 2.0.6` |
| `mockk` | 1.14.9 | 1.14.11 | `1.14.11 -> 1.14.9` |
| `logback` | 1.5.32 | 1.5.34 | `1.5.34 -> 1.5.32` |
| `zstd-jni` | 1.5.7-11 | 1.5.7-12 | cache consumers select local pin |
| `datafaker` | 2.5.4 | 2.7.0 | cache consumers select local pin |
| `kotlinx-benchmark` | 0.4.15 | 0.4.17 | benchmark plugin/runtime select local pin |
| `lz4-java` | 1.11.0 | 1.11.1 | cache consumers select local pin |
| `micrometer` | 1.16.1 | 1.17.0 | Micrometer BOM selects local pin |
| `springdoc-openapi` | 3.0.3 | 3.1.0 | Spring MVC/WebFlux consumers select local pin |
| `kover` | 0.9.8 | 0.9.9 | root coverage plugin pin |
| `mysql-connector-j` | 9.7.0 | 9.7.0 | unchanged BOM-managed direct pin; checked |
| `guava` | 33.6.0-jre | 33.6.0-jre | unchanged BOM-managed direct pin; checked |

## 예외 및 유지 대상

예외의 현재값, 1.4.0 release 값, 소비 모듈, 제거 조건은 설계서의 예외 표와
동일하게 유지한다. 요약하면 `mariadb-java-client`와 `r2dbc-postgresql`은
Spring Boot 4.1.0 관리 경계, v1 H2는 `vertx-sqlclient-example` legacy test,
`hibernate-validator`는 runtime/annotation processor 공용 key,
`springmockk`는 Boot 4 호환성 미검증 경계다. `jackson-annotations`와
`r2dbc-pool`은 release managed value와 일치한다. `fory` core는 local direct
key가 없고 provider graph에서 1.5.0을 선택하므로 별도 key를 만들지 않는다.

## 재현 명령

```bash
./gradlew :05-ktor-exposed-integration:dependencyInsight --dependency io.ktor:ktor-server-core --configuration runtimeClasspath
./gradlew :01-cache-strategies:dependencyInsight --dependency com.github.ben-manes.caffeine:caffeine --configuration runtimeClasspath
./gradlew :06-spring-cache:dependencyInsight --dependency org.apache.fory:fory-kotlin --configuration runtimeClasspath
./gradlew :08-exposed-jackson:dependencyInsight --dependency com.fasterxml.jackson.core:jackson-databind --configuration testRuntimeClasspath
./gradlew :11-exposed-jackson3:dependencyInsight --dependency tools.jackson.core:jackson-databind --configuration testRuntimeClasspath
./gradlew :01-connection:dependencyInsight --dependency com.zaxxer:HikariCP --configuration testRuntimeClasspath
./gradlew :01-dml:dependencyInsight --dependency org.postgresql:postgresql --configuration testRuntimeClasspath
```

이 문서는 release/current 비교와 원인 증거를 보존한다. 최종 resolved 값은
catalog 변경 뒤 각 명령을 새로 실행한 결과로 갱신한다.

## 변경 후 검증 결과

`gradle/dependency-governance.sh`는 정렬 대상 25개 key에 대해 모두
`status=ok`를 출력했다. `./gradlew projects --no-daemon
--no-configuration-cache`도 `BUILD SUCCESSFUL`로 통과했다.

대표 `dependencyInsight` 결과는 다음과 같다.

| 소비 configuration | resolved 결과 |
|---|---|
| `:05-ktor-exposed-integration:runtimeClasspath` | Ktor `3.5.2` |
| `:01-cache-strategies:runtimeClasspath` | Caffeine `3.2.4`, Fory Kotlin `1.5.0`, Redisson `4.7.0`, Logback `1.5.34`, Zstd `1.5.7-12`, DataFaker `2.7.0`, LZ4 `1.11.1` |
| `:08-exposed-jackson:testRuntimeClasspath` | Jackson 2 `2.22.1` |
| `:11-exposed-jackson3:testRuntimeClasspath` | Jackson 3 `3.2.1` |
| `:01-connection:testRuntimeClasspath` | HikariCP `7.1.0` |
| `:01-dml:testRuntimeClasspath` | PostgreSQL JDBC `42.7.13` |
| `:vertx-sqlclient-example:testRuntimeClasspath` | Vert.x `5.1.6`, Agroal `3.2.1` |
| `:04-benchmark:runtimeClasspath` | Hibernate `7.4.5.Final`, Kotlinx Benchmark `0.4.17` |
| `:hibernate-reactive-example:runtimeClasspath` | Hibernate Reactive `4.5.2.Final` |
| `:06-spring-modulith-publications:runtimeClasspath` | Spring Modulith `2.1.0` |
| `:05-exposed-repository-coroutines:runtimeClasspath` | Micrometer `1.17.0` |

각 결과에서 이전 local pin으로 내려가는 새 downgrade edge는 나타나지
않았다. 영향 모듈 테스트도 Ktor, cache/Fory, Jackson 2/3, connection,
Vert.x, Hibernate Reactive, Spring Modulith에서 `BUILD SUCCESSFUL`이었다.
