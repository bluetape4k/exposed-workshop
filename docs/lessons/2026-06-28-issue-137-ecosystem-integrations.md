# Issue #137 Ecosystem integrations scaffold

## 배경

Issue #137은 Exposed 1.11 database platform, Ktor, Spring Modulith, DDD 예제를 위한
epic이다. 12장은 이미 production-service integration pattern을 담당하므로, 그 장을 확장하면
chapter purpose가 흐려진다.

## 결정

`13-ecosystem-integrations`를 README-only chapter foundation으로 만든다. Child issue
#138-#145가 나중에 runnable module을 추가한다. 해당 module이 생기기 전까지 root README는
chapter overview만 link한다.

## 보호 장치

- `settings.gradle.kts` scans the chapter directory, but no Gradle child project
  is expected until a child module has its own `build.gradle.kts`.
- Examples workflow path filter는 chapter를 포함하지만, 이 foundation PR은 13장 Gradle task를
  추가하지 않는다. Child module PR은 module을 만들 때 자체 runnable task를 추가해야 한다.
- External-service example은 기본값을 local, fake, Testcontainers, emulator 또는
  documentation-only path로 둬야 한다. Real-service execution은 명시적 opt-in이어야 하며
  CI 기본값에서는 skip해야 한다.
- 13장에 runnable leaf module이 아직 없으므로 이 PR에서는 root overview와
  module-composition visual을 바꾸지 않는다. 첫 runnable child module이 들어올 때 갱신한다.

## 검증 참고

향후 child PR은 Gradle project discovery를 증명하고, file이 존재한 뒤에만 실제 README link를
추가하며, coverage가 weekly Examples, full Nightly, manual opt-in lane 중 어디에 속하는지
기록해야 한다.
