# Dependencies 1.1.3 Sync

## Context

`exposed-workshop` was still consuming `bluetape4k-dependencies = "1.1.1"`.
The published release tag `bluetape4k-dependencies` `1.1.3` is the catalog
baseline for downstream sync; the local post-release branch may already carry
the next development version.

## Decision

Keep `bluetape4k-dependencies` as the only bluetape4k BOM source and update the
catalog version to `1.1.3`. Do not add direct `bluetape4k-bom`,
`bluetape4k-exposed-bom`, or per-example JetBrains Exposed BOM imports; example
modules should consume the versions already managed by the root dependency
management setup.

## Outcome

The local catalog now resolves bluetape4k and bluetape4k-exposed versions
through `io.github.bluetape4k:bluetape4k-dependencies:1.1.3`, and example
modules no longer import `libs.jetbrains.exposed.bom` directly.

- `git show 1.1.3:gradle/libs.versions.toml` confirmed the tag catalog declares
  `bluetape4k-dependencies = "1.1.3"`.
- `rg` over Gradle files found no direct `libs.jetbrains.exposed.bom`,
  `libs.bluetape4k.dependencies`, `bluetape4k-bom`, or
  `bluetape4k-exposed-bom` usage.
- `./gradlew -q :exposed-shared-tests:dependencyInsight --configuration compileClasspath --dependency org.jetbrains.exposed:exposed-core`
  resolved `org.jetbrains.exposed:exposed-core:1.3.0`.
- `./gradlew compileTestKotlin --no-daemon` passed.
