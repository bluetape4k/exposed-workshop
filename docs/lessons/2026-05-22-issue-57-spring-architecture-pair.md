# Issue 57 Spring architecture pair

## 배경

Issue #57은 12장 epic이다. WIP limit 때문에 child lane을 한 번에 하나씩 완료해야 했고,
기존 상태에는 이미 issue #58의 Ktor 쪽 구현이 있었다.

## 결정

다른 topic을 시작하지 않고 Spring Boot 4 application architecture pair를
`12-production-integration/02-spring-application-architecture`로 추가한다. HTTP layer,
service validation, Exposed repository, H2/Hikari persistence, focused test 구성을 Ktor
모듈과 나란히 유지한다.

## 결과

12장은 이제 paired Spring/Ktor architecture topic과 이후 production-integration topic을
planned work로 표시하는 chapter-level README file을 갖는다.

## 검증

- `./gradlew projects --no-daemon` registered
  `:02-spring-application-architecture`.
- `./gradlew :02-spring-application-architecture:compileKotlin --no-daemon`
  passed.
- `./gradlew :02-spring-application-architecture:test --no-daemon` passed with
  8 tests.
- `./gradlew :01-ktor-application-architecture:test --no-daemon` passed with
  13 tests.
- `./gradlew :01-ktor-application-architecture:test
  :02-spring-application-architecture:test --no-daemon` passed after Claude
  advisor fixes.
- `.github/workflows/examples.yml` added to run the paired chapter 12
  architecture examples in the `Examples` workflow.
- `./gradlew :01-ktor-application-architecture:build
  :02-spring-application-architecture:build --no-daemon --continue` passed.
- `actionlint .github/workflows/examples.yml` passed.
- `git diff --check` passed.
- `:02-spring-application-architecture:detekt` is not registered.
- Claude CLI P0/P1 advisor review는 P0 없이 `ErrorAdvice`에서 P1 두 건을 반환했다:
  `Throwable` catch 회피, unexpected 5xx cause logging. 둘 다 `Exception` 처리와
  sanitized response 반환 전 exception logging으로 수정했다.

## 향후 보호 장치

#59-#62에서는 Spring/Ktor coverage를 pair로 추가하고 같은 변경에서 chapter README table을
갱신한다.
