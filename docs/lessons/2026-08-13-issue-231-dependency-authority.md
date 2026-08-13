# 의존성 버전 권위와 로컬 pin의 충돌을 분리한다

## 맥락

Workshop은 `bluetape4k-dependencies:1.4.0` BOM을 import하고 있었지만,
version catalog의 직접 pin 일부가 릴리스 catalog보다 낮았다. Gradle은
provider가 요청한 버전과 local catalog가 요청한 버전을 함께 보여 주므로,
`dependencyInsight`에서 `3.5.2 -> 3.5.0` 같은 downgrade edge가 드러났다.

## 결정

공식 `bluetape4k-dependencies 1.4.0` release catalog를 shared dependency의
중앙 권위로 삼고, 대표 사례와 전수 대조에서 local 선택 또는 downgrade가
확인된 23개 direct pin을 일치시키고, release 값과 같지만 BOM이 관리하는
MySQL Connector/J와 Guava 두 pin도 검사 목록에 포함했다. 여기에는 Ktor, HikariCP, PostgreSQL
JDBC, Redisson, Netty, Jackson 2/3, Caffeine, Fory Kotlin, Vert.x, Hibernate
계열, Micrometer, Springdoc, Kover, cache/test/benchmark 라이브러리가 포함된다. Boot 관리값,
legacy H2, 공용 annotation processor, Spring 호환성 경계는 예외 목록으로
남겼고, local direct alias가 없는 Fory core에는 새 pin을 추가하지 않았다.

## 검증

`gradle/dependency-governance.sh`를 catalog 변경 전 실행해 stale key의 RED
출력을 확인한 뒤, 값을 조정하고 25개 key가 모두 `status=ok`가 되는지
확인했다. script는 BOM version도 함께 `1.4.0`인지 확인하므로, 다른 BOM을
가리킨 채 1.4.0 기대값을 통과시키지 않는다. `./gradlew projects --no-daemon --no-configuration-cache`도
`BUILD SUCCESSFUL`로 통과했다. 대표 소비 모듈의 `dependencyInsight`와
compatibility test는 catalog 변경 후 새 결과를 수집해 PR 검증에 포함한다.

## 예상 밖의 점

Redisson은 local pin이 낮았지만 provider graph가 이미 4.7.0을 선택하고
있었다. 그러나 선언과 release authority의 drift는 계속 남으므로, resolved
결과만으로 catalog 정합성을 판단하면 안 된다. 반대로 Netty는 Ktor 소비
configuration에 나타나지 않아 root의 직접 dependency-management 경계를
별도로 확인해야 했다.

## 다음 방어선

catalog 변경은 `gradle/dependency-governance.sh`를 먼저 실행하고, release
catalog와의 차이를 확인한 뒤 dependencyInsight에서 선언 경로와 resolved
version을 함께 기록한다. BOM이 관리하는 artifact에 ad hoc pin을 추가할
때는 반드시 compatibility 사유를 lesson 또는 catalog 주석으로 남긴다. BOM
release가 바뀌면 이 1.4.0 전용 guard와 release 비교 자료를 같은 변경에서
갱신한다.
