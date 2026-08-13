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

## 유지 대상

`jackson-annotations = 2.22`, `r2dbc-pool = 1.0.2.RELEASE`,
`r2dbc-postgresql = 1.1.1.RELEASE`는 release catalog의 managed value와
일치한다. `fory` core는 local direct key가 없고 provider graph에서 1.5.0을
선택하므로 별도 key를 만들지 않는다.

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

