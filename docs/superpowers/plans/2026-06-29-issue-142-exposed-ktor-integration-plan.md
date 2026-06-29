# Issue #142 Plan - Explicit Ktor Exposed Integration

## Step 1 - Source and Requirement Grounding

- Action: inspect issue #142, chapter 13 README, existing Ktor examples, and
  `bluetape4k-exposed-ktor` source/demo code.
- DoD: record the chosen module path, helper APIs, and acceptance evidence in
  the design artifact.

## Step 2 - TDD Contract

- Action: add the new module skeleton and tests before production code.
- DoD: `./gradlew :05-ktor-exposed-integration:test --no-daemon --no-configuration-cache`
  fails at compile time because the production API is intentionally missing.

## Step 3 - Implementation

- Action: implement the local H2 Ktor example, caller-owned resources, CRUD
  routes, readiness routes, and sanitized error route.
- DoD: targeted module tests pass locally.

## Step 4 - Documentation and Diagram

- Action: add bilingual module README files and a `$bluetape4k-diagram`
  compliant SVG/PNG architecture diagram.
- DoD: SVG geometry and endpoint audits pass, PNG renders successfully, and the
  README links point to the rendered asset.

## Step 5 - Registration and Automation

- Action: update chapter/root README links, version catalog aliases, and
  `.github/workflows/examples.yml`.
- DoD: Gradle project discovery includes the module and the Examples workflow
  includes `:05-ktor-exposed-integration:build`.

## Step 6 - Verification and PR

- Action: run targeted tests, `git diff --check`, review local diff, write a
  review note and lesson, then open a PR linked to issue #142.
- DoD: PR metadata mirrors issue #142 assignee, labels, and milestone; PR body
  ends with `## DoD Status`.

