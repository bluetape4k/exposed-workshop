# Issue #140 CockroachDB Serializable Retry Workshop Plan

Date: 2026-06-29
Issue: https://github.com/bluetape4k/exposed-workshop/issues/140

## Step 1 - Red Tests

Action: Add the module skeleton plus tests that reference the desired workshop
API.

DoD: `./gradlew :03-cockroachdb-retry:test` fails only because the workshop
implementation or catalog wiring is missing.

## Step 2 - Implementation

Action: Implement inventory reservation helpers using public
`bluetape4k-exposed-cockroachdb` APIs.

DoD: Tests prove successful commit, retryable failure replay, non-retryable
failure boundary, and schema bootstrap.

## Step 3 - Documentation And Diagram

Action: Add README.md, README.ko.md, chapter/root README links, and a validated
SVG+PNG sequence diagram.

DoD: README locale pair embeds the same PNG and explains the local Testcontainers
path without raw Mermaid.

## Step 4 - Workflow And Verification

Action: Add the module to the Examples workflow, run targeted local checks,
review the diff, commit, open PR, monitor CI, merge after green, and sync local
develop.

DoD: Local verification passes; PR mirrors issue metadata; CI passes; PR is
rebased into develop; issue #140 closes.
