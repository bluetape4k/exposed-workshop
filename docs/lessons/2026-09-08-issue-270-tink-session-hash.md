# 이슈 #270 Tink 세션 토큰 해시 통합 교훈

## 배경

Spring과 Ktor auth-session repository가 각각 JDK `MessageDigest`와 private
`SessionTokenHasher`를 유지하고 있었다. 두 schema의 `token_hash`는
`varchar(64).uniqueIndex()`이므로 provider를 교체해도 UTF-8 SHA-256 lowercase
hex 저장 계약을 그대로 보존해야 했다.

## 결정

- 두 repository의 저장·조회 경로는 `TinkDigesters.SHA256.digestHex(token)`을
  직접 호출하고 private hasher를 제거한다.
- `bluetape4k-tink`와 전이되는 `bluetape4k-core`를 배포된
  `2.1.0-20260907.141940-7`로 두 auth-session 모듈에 직접 pin한다. 안정 BOM
  `2.0.0`이 provider의 `2.1.0-SNAPSHOT` core 전이를 `2.0.0`으로 낮추지 않게
  resolved graph에서 확인한다.
- hash 계약은 empty string, Unicode `한글🔐`, 실제 SHA-256 digest 첫 byte가
  `0x00`인 token `"286"`으로 고정한다. `matchesHex`는 올바른 값만 허용하고
  다른 digest, uppercase, malformed, wrong-length 입력을 거부하는지 양쪽
  consumer test에서 확인한다.

## 검증 교훈

- 처음 사용한 `leading-zero-14`의 결과는 hex 문자열이 `0`으로 시작하지만 첫
  byte가 `0x02`였다. leading-zero 의미를 검증하려면 hex 표기 첫 문자만 보지
  말고 두 문자가 `00`인지 확인해야 한다.
- provider의 timestamped build를 직접 연결하면 BOM의 안정 버전 규칙이 전이 dependency를
  낮출 수 있다. Tink만 graph에 보이는지 확인하지 말고 provider가 요구하는
  core 버전도 `dependencyInsight`로 확인하고 필요할 때 같은 배포 build를
  모듈 범위에 pin한다.
- consumer 경계에서 verifier를 직접 호출하는 경우에도 provider hex contract의
  `matchesHex` malformed·대소문자·길이 실패를 한 번 고정해 API 의미를 잃지
  않게 한다.

## 최종 근거

- RED: provider dependency가 없을 때 Spring `compileTestKotlin`에서
  `TinkDigesters` unresolved를 확인했다.
- GREEN: Spring 7개와 Ktor 10개의 auth-session 테스트, known-vector,
  persistence, `matchesHex`, endpoint/repository 검증과 두 모듈의 clean Kotlin
  compile을 모두 통과했다.
- 두 모듈의 `runtimeClasspath`와 `dependencyInsight`에서 Tink/core가
  `2.1.0-20260907.141940-7`로 선택됐고, `2.0.0 -> 2.1.0-20260907.141940-7`
  승격만 존재하며 `2.1.0-SNAPSHOT -> 2.0.0` downgrade는 없었다.
- `MessageDigest`·`SessionTokenHasher` 잔여 검색, raw token logging 검색,
  `git diff --check`, Korean terminology audit를 통과했다. Ktor detekt의 기존
  `KtorPlugins.kt` magic number와 `AuthRoutes.kt` line length 보고 외 변경 파일
  관련 진단은 없었다.

## 다음 적용 원칙

중앙 catalog가 동일 provider train을 안정 버전으로 발행하면 local timestamped
aliases와 직접 core pin을 제거하고 중앙 alias로 되돌린다. 그 전에는 consumer의
resolved graph에서 provider train 전체가 같은 build인지 확인한다.
