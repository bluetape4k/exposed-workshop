# bluetape4k-dependencies BOM 전환

## Context

`exposed-workshop`가 `bluetape4k-bom`과 `bluetape4k-exposed-*` 버전을 `1.8.0-SNAPSHOT` catalog 값으로 직접 고정하고 있었다. `bluetape4k-dependencies:1.0.0`가 release BOM들을 가져오므로, workshop은 central BOM 하나를 기준으로 버전을 받아야 했다.

## Decision

- 루트 `dependencyManagement`는 `bluetape4k-bom` 대신 `io.github.bluetape4k:bluetape4k-dependencies:1.0.0`을 import한다.
- bluetape4k 및 `io.github.bluetape4k.exposed` alias는 versionless로 둔다.
- 기존 `spring-boot4-*` alias는 published artifact 이름인 `spring-boot-*`로 바꾼다.
- 사용하지 않는 `bluetape4k-bom` alias는 제거해서 직접 import로 되돌아갈 여지를 줄인다.

## Outcome

`bluetape4k-dependencies`가 `bluetape4k-bom:1.8.0`과 `bluetape4k-exposed-bom:1.8.0`을 import하고, Gradle이 exposed/spring/bluetape4k 아티팩트 버전을 central BOM에서 해석하도록 정리했다.
추가로 중앙 shared version 검증에 맞춰 JetBrains Exposed와 Dokka catalog alias도 각각 `1.3.0`, `2.2.0`으로 정렬했다.

## Verification

- `./gradlew -q projects`
- `./gradlew compileKotlin compileTestKotlin`
- `./gradlew :01-ktor-application-architecture:test :11-exposed-jackson3:test :06-spring-cache:test`
- `./gradlew -q :11-exposed-jackson3:dependencyInsight --configuration testRuntimeClasspath --dependency io.github.bluetape4k.exposed:bluetape4k-exposed-jackson3`
- `./gradlew -q :06-spring-cache:dependencyInsight --configuration compileClasspath --dependency io.github.bluetape4k:bluetape4k-spring-boot-core`
- `git diff --check`
- `bluetape4k-dependencies/scripts/sync-shared-versions.py --workspace <symlink-workspace> --repo exposed-workshop --check --summary`
- After aligning Exposed/Dokka aliases: `./gradlew compileKotlin compileTestKotlin --no-daemon`
- `./gradlew build --no-daemon` failed during `:03-functions:test` after MySQL Testcontainers connection refusal; dependency resolution and compilation had already passed.

## Future Guard

External advisor results about BOM coverage can be stale or based on artifact-name assumptions. Verify against the published BOM POM and `dependencyInsight` before changing artifact coordinates.
