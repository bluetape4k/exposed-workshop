# Dependencies 1.1.3 동기화

## 배경

`exposed-workshop`은 아직 `bluetape4k-dependencies = "1.1.1"`을 소비하고 있었다.
Published release tag `bluetape4k-dependencies` `1.1.3`은 downstream sync의 catalog
baseline이며, local post-release branch는 이미 다음 development version을 담고 있을 수 있다.

## 결정

`bluetape4k-dependencies`를 유일한 bluetape4k BOM source로 유지하고 catalog version을
`1.1.3`으로 갱신한다. Direct `bluetape4k-bom`, `bluetape4k-exposed-bom`, per-example
JetBrains Exposed BOM import는 추가하지 않는다. Example module은 root dependency management
setup이 이미 관리하는 version을 소비해야 한다.

## 결과

Local catalog는 이제 `io.github.bluetape4k:bluetape4k-dependencies:1.1.3`을 통해
bluetape4k와 bluetape4k-exposed version을 resolve하고, example module은 더 이상
`libs.jetbrains.exposed.bom`을 직접 import하지 않는다.

- `git show 1.1.3:gradle/libs.versions.toml`로 tag catalog가
  `bluetape4k-dependencies = "1.1.3"`을 선언함을 확인했다.
- Gradle file에 대한 `rg`는 direct `libs.jetbrains.exposed.bom`,
  `libs.bluetape4k.dependencies`, `bluetape4k-bom`, `bluetape4k-exposed-bom` 사용이
  없음을 확인했다.
- `./gradlew -q :exposed-shared-tests:dependencyInsight --configuration compileClasspath --dependency org.jetbrains.exposed:exposed-core`
  resolved `org.jetbrains.exposed:exposed-core:1.3.0`.
- `./gradlew compileTestKotlin --no-daemon` passed.
