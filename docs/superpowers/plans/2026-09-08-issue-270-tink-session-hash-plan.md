# Issue 270 Tink 세션 토큰 해시 통합 Implementation Plan

> **For agentic workers:** 승인된 설계를 기준으로 테스트를 먼저 작성하고, 두 auth-session 모듈을 같은 provider contract로 갱신한다.

**Goal:** Spring·Ktor auth-session 예제가 published `bluetape4k-tink` hex API를 사용해 중복 JDK hasher를 제거한다.

**Architecture:** repository의 token 저장/조회 지점이 `TinkDigesters.SHA256.digestHex`를 직접 호출한다. catalog timestamped provider aliases와 module-scoped implementation dependency만 추가하고 session schema/API는 유지한다.

**Tech Stack:** Kotlin 2.4, Spring Boot 4.1, Ktor 3.5, Exposed 1.4, H2, Gradle version catalog, `io.github.bluetape4k:bluetape4k-tink:2.1.0-20260907.141940-7`, `io.github.bluetape4k:bluetape4k-core:2.1.0-20260907.141940-7`.

---

### Task 1: Tink/core timestamped provider aliases와 module dependency를 RED 상태로 추가

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `12-production-integration/05-spring-auth-session/build.gradle.kts`
- Modify: `12-production-integration/06-ktor-auth-session/build.gradle.kts`

- [x] `[versions]`에 배포된 timestamped provider build `bluetape4k-provider-timestamp = "2.1.0-20260907.141940-7"`을 추가하고 `[libraries]`에 `bluetape4k-tink-snapshot`과 matching `bluetape4k-core-snapshot` alias를 추가한다.
- [x] 두 module의 `dependencies`에 `implementation(libs.bluetape4k.core.snapshot)`과 `implementation(libs.bluetape4k.tink.snapshot)`을 추가한다.
- [x] provider dependency를 받기 전 baseline에서 `TinkDigesters` import가 unresolved인 RED compile을 관찰한다.

### Task 2: RED — known vector와 persistence 계약을 고정

**Files:**
- Modify: `12-production-integration/05-spring-auth-session/src/test/kotlin/exposed/examples/spring/auth/SpringAuthSessionApplicationTest.kt`
- Modify: `12-production-integration/06-ktor-auth-session/src/test/kotlin/exposed/examples/ktor/auth/repository/ExposedAuthRepositoryTest.kt`

- [x] 두 테스트에 `TinkDigesters.SHA256.digestHex` known vector를 추가한다. `""`, Unicode `"한글🔐"`, 실제 `0x00` 선행 byte 결과를 만드는 token `"286"`을 각각 확인하고 expected는 현재 provider output에서 고정한다.
- [x] 두 테스트에서 `matchesHex`의 일치, 다른 digest, uppercase, malformed, wrong-length 결과를 확인한다.
- [x] Ktor repository test는 session 생성 후 `AuthSessions`의 저장 hash를 읽어 64자 lowercase hex인지 확인하고, Spring integration test는 repository/database query 또는 생성 결과에 대한 동일한 저장 계약을 확인한다.
- [x] session endpoint/repository 기존 테스트를 먼저 실행해 새 assertion이 provider API 없이 실패하는지 확인한다.

### Task 3: GREEN — private hasher를 provider API로 치환

**Files:**
- Modify: 두 `ExposedAuthRepository.kt`

- [x] `java.security.MessageDigest` import와 `SessionTokenHasher` object를 삭제한다.
- [x] 저장/조회 hash 지점을 다음 단일 호출로 바꾼다.

```kotlin
TinkDigesters.SHA256.digestHex(token)
```

- [x] 기존 `varchar("token_hash", 64).uniqueIndex()`, expiration predicate, transaction/IO dispatcher, authentication flow는 그대로 둔다.
- [x] Task 2의 RED 테스트와 전체 targeted module tests를 GREEN으로 만든다.

### Task 4: 검증·정리·commit

- [x] 순서대로 실행한다.

```bash
./gradlew :05-spring-auth-session:test --tests '*SpringAuthSessionApplicationTest' --no-build-cache
./gradlew :06-ktor-auth-session:test --tests '*ExposedAuthRepositoryTest' --tests '*AuthRoutesTest' --no-build-cache
./gradlew :05-spring-auth-session:dependencies --configuration runtimeClasspath
./gradlew :06-ktor-auth-session:dependencies --configuration runtimeClasspath
./gradlew :05-spring-auth-session:compileKotlin :06-ktor-auth-session:compileKotlin
git diff --check
```

- [x] dependency output에 `bluetape4k-tink:2.1.0-20260907.141940-7`과 `bluetape4k-core:2.1.0-20260907.141940-7`이 있고, `dependencyInsight`에 `2.1.0-SNAPSHOT -> 2.0.0` downgrade가 없는지 확인한다.
- [x] raw token이 로그/예외/fixture 출력에 추가되지 않았는지 `rg`로 확인한다.
- [x] 설계/계획 SPW-01..05, Kotlin KT-FIN-01..11, workflow CG-01..10 증거를 기록하고 Lore commit protocol로 한국어 commit을 만든다.
