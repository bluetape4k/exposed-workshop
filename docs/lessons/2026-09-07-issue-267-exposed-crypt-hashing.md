# 이슈 #267 Exposed 1.5.0 단방향 hashing lesson

## 배경

기존 `bluetape4k-dependencies:2.0.0` BOM은 JetBrains Exposed `1.4.0`을
관리하므로 `exposed-crypt`의 `BCryptHasher`, `Hashed`, `Column.hashed` API를
컴파일 클래스패스에서 제공하지 않는다. 중앙 catalog의 develop commit
`9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`는 `exposed = 1.5.0`으로 승격했지만,
현재 공개된 `2.1.0-SNAPSHOT` metadata/POM은 아직 Exposed `1.4.0`을 관리한다.

## 결정

- version catalog의 기존 `exposed = 1.4.0` plugin/BOM 권한은 유지하고,
  `exposed-hashing = 1.5.0`과 `org.jetbrains.exposed:exposed-bom` bridge alias를
  추가한다.
- `01-exposed-crypt` 모듈에만 Exposed `1.5.0` BOM을 import해 다른 workshop
  모듈의 안정 `2.0.0` dependency contract를 바꾸지 않는다.
- 다음 중앙 catalog publication에서 Exposed `1.5.0`이 관리되면 이 모듈 범위의
  import를 재검토한다. 임의의 Bluetape artifact version pin은 추가하지 않는다.

## 구현 범위

- `Ex03_HashedColumn.kt`: DSL `hashed()`/`Column.hash()` 사용, 올바른·틀린 후보
  검증, 저장 평문 부재, nullable `null` 보존, 이미 저장한 `Hashed` 재할당 시
  재해싱 금지를 검증한다.
- `Ex04_HashedColumnWithEntity.kt`: DAO Entity 경로에서 같은 계약을 검증한다.
- EN/KO 모듈·챕터·최상위 README에 가역 encryption과 단방향 hashing의 차이,
  `Hashed.matches()` 검증 규칙, 로그에 secret을 남기지 않는 원칙을 기록한다.

## 검증

- RED: BOM `2.0.0`의 Exposed `1.4.0` 클래스패스에서는 `BCryptHasher`와
  `hashed` 심볼이 해석되지 않아 `compileTestKotlin`이 실패했다.
- GREEN: Exposed `1.5.0` BOM import 후 `:01-exposed-crypt:compileTestKotlin`이
  성공했다.
- `USE_FAST_DB=true`와 함께 모듈 테스트를 실행해 H2·PostgreSQL·MySQL 대상
  기존·신규 테스트 총 24개가 통과했다.
- Maven Central에서 `exposed-bom:1.5.0`, `exposed-crypt:1.5.0` POM/JAR/
  module metadata 응답이 모두 HTTP `200`임을 확인했다.

## 다음 적용 원칙

중앙 catalog commit과 실제 publication artifact가 일치하지 않으면 소비자에서
맹목적으로 SNAPSHOT을 올리지 않는다. 먼저 resolved graph에서 Exposed 버전을
확인하고, 필요한 경우 영향 범위를 한 모듈로 제한한 뒤 중앙 publication 이후
해당 예외를 제거한다.
