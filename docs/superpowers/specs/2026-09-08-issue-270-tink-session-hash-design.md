# Issue 270 세션 토큰 해시 API 통합 설계

## 문제와 근거

Spring과 Ktor auth-session repository가 각각 `MessageDigest.getInstance("SHA-256")`와 기본 `toByteArray()`를 사용해 private `SessionTokenHasher`를 복제한다. 두 schema 모두 `token_hash varchar(64).uniqueIndex()`를 사용한다.

- Spring: `12-production-integration/05-spring-auth-session/src/main/kotlin/exposed/examples/spring/auth/repository/ExposedAuthRepository.kt`
- Ktor: `12-production-integration/06-ktor-auth-session/src/main/kotlin/exposed/examples/ktor/auth/repository/ExposedAuthRepository.kt`
- upstream `bluetape4k-projects#1649`의 병합 PR `#1678`
- provider snapshot `io.github.bluetape4k:bluetape4k-tink:2.1.0-SNAPSHOT`

현재 provider의 `TinkDigesters.SHA256.digestHex(String)`은 UTF-8 입력을 lowercase hex로 변환하고 SHA-256 결과를 항상 64자로 만든다. `matchesHex`는 canonical lowercase 형식과 길이를 먼저 확인한 뒤 constant-time 비교를 수행한다.

## 경계와 선택

두 repository는 private hasher를 유지하지 않고 저장/조회 지점에서 `TinkDigesters.SHA256.digestHex(token)`을 호출한다. catalog에는 workshop이 직접 소비하는 snapshot alias를 추가하고 두 모듈에 `implementation`으로 선언한다. 안정 BOM 2.0.0을 전역 변경하지 않으며 snapshot override는 이 workshop 범위에 한정한다.

검토한 대안은 (1) 기존 JDK 구현을 한 shared utility로 이동하는 방식, (2) provider의 Base64 `digest`를 사용하는 방식, (3) Tink hex API 직접 호출이다. (1)은 provider contract 중복을 남기고, (2)는 `varchar(64)` 저장 계약을 깨뜨리므로 제외한다. (3)은 기존 DB 값과 encoding을 보존하면서 private duplication을 제거하므로 선택한다.

## 호환성·보안 계약

- 기존 token의 UTF-8 SHA-256 lowercase 64자 hex 값은 동일해야 한다. Unicode, empty string, leading-zero digest byte를 known vector로 고정한다.
- `token_hash` column 길이와 unique index를 변경하지 않는다.
- Ktor의 token lookup은 기존처럼 hash equality와 expiration 조건을 함께 적용한다. Spring/Ktor authentication, TTL, transaction 경계는 변경하지 않는다.
- raw token은 DB 외부에 기록하지 않으며 hash API 예외나 테스트 출력에도 token 값을 포함하지 않는다.
- malformed/uppercase/길이 오류는 provider `matchesHex` 계약에서 `false`로 처리되지만, 현재 consumer에는 expected-hash verifier가 없으므로 범위 밖이다. provider contract는 별도 API test로 확인한다.

## 검증과 문서

두 모듈의 session creation/list/lookup endpoint 및 repository 테스트를 유지하고, 각 repository 테스트에 known-vector와 저장된 hash length/format 검사를 추가한다. 변경 후 `MessageDigest`와 `SessionTokenHasher`가 consumer에 남지 않는지 검색하고 dependency graph/POM에서 Tink artifact가 runtime에만 필요한 모듈에 올바르게 포함되는지 확인한다.

완료 조건은 두 모듈 compile/test, provider snapshot resolution, UTF-8/lowercase/64-character contract, raw-token 비노출, `git diff --check`, Kotlin final checklist PASS다.
