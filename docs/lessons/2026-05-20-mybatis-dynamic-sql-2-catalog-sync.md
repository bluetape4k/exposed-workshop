# MyBatis Dynamic SQL 2 Catalog Sync

## Context

`bluetape4k-dependencies` promoted MyBatis Dynamic SQL to 2.0.0 and Fory Kotlin
to 0.17.0 as shared catalog versions.

## Decision

Materialize the shared catalog change in the Exposed workshop repository and
verify the build still compiles.

## Outcome

`gradle/libs.versions.toml` now carries MyBatis Dynamic SQL 2.0.0 and Fory
Kotlin 0.17.0.

## Verification

- `./gradlew build -x test --no-daemon`

The build completed with existing unrelated warnings.
