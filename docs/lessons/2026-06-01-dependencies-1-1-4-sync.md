# Dependencies 1.1.4 Sync

## Context

`bluetape4k-dependencies` 1.2.0 release preparation requires downstream
workshops to match the central shared-version source of truth before the BOM
release CI can pass.

## Decision

Align the workshop catalog to the latest published
`bluetape4k-dependencies:1.1.4` baseline and central shared runtime versions.
Do not consume `1.2.0` until it is published.

## Outcome

The workshop no longer reports shared-version drift in the central release
preflight.

## Verification

Validated from `bluetape4k-dependencies` with `sync-shared-versions.py
--workspace /Users/debop/work/bluetape4k --write --check --summary`.
