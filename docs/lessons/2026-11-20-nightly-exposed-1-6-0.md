# Nightly 실패를 Exposed 1.6.0과 명시적 BOM으로 복구한 lesson

## 문맥

`develop`의 동일한 HEAD `7a5f55bc3e0651c8dc21b853826f5e155c67e7d0`에서 Nightly가 반복해서 실패했다. 최신 확인 run은 [#44433422975](https://github.com/bluetape4k/exposed-workshop/actions/runs/44433422975)이며 다음 실패가 함께 나타났다.

- `Build & Detekt`: `:01-bigquery-dry-run:compileKotlin`의 versionless dependency가 빈 버전으로 해석됨
- `Test / H2 / full`, PostgreSQL, MySQL: `Ex04_LongIdTableEntity`가 FK `RESTRICT` 위반
- `Test / H2 / full`: Spring Modulith 테스트가 `kotlin.uuid.Uuid`와 `String`을 Exposed 컬럼 타입에 대입

JetBrains 공식 release 목록에서 현재 최신 patch는 1.6.1이지만, 이번 변경의 목표는 사용자 요청 버전인 Exposed 1.6.0이다. 적용 전후 API는 이미 `org.jetbrains.exposed.v1.*` 패키지를 사용하고 있었다.

## 원인

1. 루트 `bluetape4k-dependencies:2.1.0-SNAPSHOT` BOM이 `bluetape4k-bom`과 `bluetape4k-exposed-bom`을 중첩 import했지만, GitHub runner의 dependency-management 해석에서는 consumer의 versionless dependency에 빈 버전이 전달됐다. 실패 로그에는 `bluetape4k-logging:.`, `bluetape4k-exposed-dao:.`, Jackson 3 모듈과 `org.jetbrains.exposed:exposed-core:.`가 그대로 남았다.
2. Exposed DAO의 새 엔티티는 insert가 지연될 수 있다. `flush()` 없이 `delete()`하면 삭제 대상 행이 아직 영속되지 않은 상태로 cache에서만 제거되고, 이어지는 부모 도시 삭제 시 FK 행이 남는다.
3. Spring Modulith publication table의 `id` 컬럼은 `java.util.UUID`, `status` 컬럼은 `EventPublication.Status?`인데 테스트가 각각 Kotlin `Uuid`와 `Status.FAILED.name`을 사용했다.

## 결정

- version catalog의 `exposed`와 `exposed-hashing`을 모두 `1.6.0`으로 올리고, 루트 `dependencyManagement`에 `org.jetbrains.exposed:exposed-bom:1.6.0`을 명시한다.
- `bluetape4k-bom`과 `bluetape4k-exposed-bom`도 루트 dependency-management에 직접 import한다. 중첩 BOM 해석에 의존하지 않아 versionless consumer의 빈 버전을 방지한다.
- `Ex04_LongIdTableEntity`에서 `sunghoon.flush()` 후 `delete()`를 호출해 실제 FK 행을 먼저 제거한다. FK 정책을 완화하거나 cascade로 바꾸지 않는다.
- Spring Modulith 테스트는 `UUID`와 `Status.FAILED`를 컬럼에 직접 대입한다.
- `exposed-crypt`의 EN/KO README도 요청 버전 1.6.0으로 맞춘다.

## 결과

수정 후 dependency graph는 `org.jetbrains.exposed:exposed-core:1.6.0`과 `org.jetbrains.exposed:exposed-bom:1.6.0`을 선택했고, Bluetape 모듈은 timestamped `2.1.0-SNAPSHOT` artifact로 해석됐다. versionless dependency가 빈 버전으로 남지 않았다.

## 검증

- RED: `./gradlew :06-spring-modulith-publications:compileTestKotlin`은 두 컬럼 대입의 type mismatch로 실패했다.
- RED: `USE_FAST_DB=true ./gradlew :05-entities:test --tests 'exposed.examples.entities.Ex04_LongIdTableEntity'`는 H2/PostgreSQL/MySQL에서 FK `RESTRICT`로 실패했다.
- GREEN: `./gradlew :01-bigquery-dry-run:compileKotlin --no-daemon --console=plain` 성공
- GREEN: `./gradlew :06-spring-modulith-publications:test --no-daemon --console=plain` 3개 테스트 성공
- GREEN: `USE_FAST_DB=true ./gradlew :05-entities:test --no-daemon --console=plain` 전체 테스트 성공
- GREEN: `./gradlew :05-entities:detekt :06-spring-modulith-publications:detekt --no-daemon --console=plain` 성공
- GREEN: `./gradle/dependency-governance.sh`의 모든 항목 `status=ok`
- GREEN: `dependencyInsight`에서 Exposed core/BOM이 1.6.0으로 선택됨

## 놓친 점과 다음 guard

- Nightly는 실패한 시도와 retry-green 시도를 분리해 읽어야 한다. 이번 수정은 로컬 검증만 완료했으며, 수정 HEAD를 원격에 올리거나 workflow를 dispatch하지 않았으므로 GitHub의 새 Nightly 결과는 아직 없다.
- Snapshot root BOM을 추가하거나 갱신할 때는 중첩 BOM을 직접 import하는지와 versionless consumer의 `dependencyInsight`를 함께 확인한다.
- Exposed 1.6.1로의 추가 patch upgrade는 이번 요청 범위를 벗어나므로 별도 결정으로 다룬다.

## DoD Status

- 상태: 로컬 수정·검증 완료, 원격 Nightly 재실행 대기
- 범위: `fix/nightly-exposed-1.6.0` 격리 브랜치
- 남은 항목: 원격 branch/PR 반영 후 exact-head GitHub Nightly 확인
