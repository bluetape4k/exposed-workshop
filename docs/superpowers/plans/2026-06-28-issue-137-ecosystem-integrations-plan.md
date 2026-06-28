# Issue #137 Ecosystem Integrations Chapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `13-ecosystem-integrations` chapter foundation for issue #137 without claiming the child examples are implemented.

**Architecture:** The PR creates a README-only chapter overview, registers the chapter scan hook, and wires root docs plus Examples workflow path triggers. Child issues #138-#145 will create runnable Gradle modules later and must add their own workflow build tasks.

**Tech Stack:** Gradle Kotlin DSL, GitHub Actions, Markdown README pairs, committed SVG/PNG README diagram assets.

---

## File Structure

- Create: `13-ecosystem-integrations/README.md`
- Create: `13-ecosystem-integrations/README.ko.md`
- Create: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`
- Create: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`
- Modify: `settings.gradle.kts`
- Modify: `README.md`
- Modify: `README.ko.md`
- Modify: `AGENTS.md`
- Modify: `.github/workflows/examples.yml`
- Create: `docs/lessons/2026-06-28-issue-137-ecosystem-integrations.md`

## Task 0: Commit Design Artifacts

**complexity:** low

**Files:**
- Create: `docs/superpowers/specs/2026-06-28-issue-137-ecosystem-integrations-design.md`
- Create: `docs/superpowers/plans/2026-06-28-issue-137-ecosystem-integrations-plan.md`

- [ ] Confirm Step 2-R and Step 3-R review gates have P0=0 and P1=0.
- [ ] Commit the spec and plan before implementation edits.
- [ ] Use a Lore-style commit message that records review evidence.

## Task 1: Register Chapter Boundary

**complexity:** low

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `AGENTS.md`

- [ ] Add `includeModules("13-ecosystem-integrations", false, false)` immediately after chapter 12 in `settings.gradle.kts`.
- [ ] Update the `AGENTS.md` layout table to include both `12-production-integration` and `13-ecosystem-integrations`.
- [ ] Defer Gradle project verification until after Task 2 creates the base directory.

## Task 2: Add Chapter README Pair

**complexity:** low

**Files:**
- Create: `13-ecosystem-integrations/README.md`
- Create: `13-ecosystem-integrations/README.ko.md`

- [ ] Add the English README with `English | [한국어](README.ko.md)`.
- [ ] Add the Korean README with `[English](README.md) | 한국어`.
- [ ] Include the shared architecture PNG reference:
  `![Chapter 13 ecosystem integrations architecture](../docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png)`.
- [ ] Include the planned issue table for #138-#145 with status `Planned`, full planned directory, Gradle task, README title, and lane.
- [ ] Use explicit GitHub issue links in both README tables, for example `[#138](https://github.com/bluetape4k/exposed-workshop/issues/138)`.
- [ ] Include the external service and credential policy with these five explicit bullets:
  no checked-in credentials/tokens/service-account files/project IDs/endpoint secrets;
  no default ADC or local credential file use;
  fake/local/Testcontainers/emulator-style defaults;
  real-service execution is explicit opt-in and skipped by default in CI;
  README warnings for cost, network, and credentials before any real-service command.
- [ ] Avoid links to non-existent child README files.
- [ ] Add a short future child implementation order note: create the module and `build.gradle.kts`, verify Gradle project discovery, add chapter/root README links only after files exist, add the module build task to Examples or Nightly as appropriate, and record credential/coverage lane decisions.
- [ ] Add a future child workflow-lane checklist with separate fields for default local/fake path, real-service opt-in property or tag, and selected workflow lane: weekly Examples, full Nightly, or manual opt-in.

## Task 3: Add Chapter Diagram Assets

**complexity:** medium

**Files:**
- Create: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`
- Create: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`

- [ ] Create a source-backed SVG with three lanes: Database platform adapters, Runtime/framework integration, Domain architecture.
- [ ] Use `Architects Daughter` and `Comic Mono` font families.
- [ ] Keep final diagram text reader-facing only.
- [ ] Validate XML with `xmllint --noout docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`.
- [ ] Check the SVG has a non-empty `viewBox`, explicit `width`, and explicit `height`.
- [ ] Render PNG with `~/.local/bin/cairosvg docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg -o docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png -s 2`.
- [ ] Inspect the rendered PNG with `view_image`.
- [ ] During PNG inspection, verify no text overflow, no card/connector overlap, no excessive whitespace, readable three-lane grouping, and all eight planned child examples represented.

## Task 4: Wire Root README Pair

**complexity:** low

**Files:**
- Modify: `README.md`
- Modify: `README.ko.md`

- [ ] Add a root module-list entry for the chapter overview only.
- [ ] English root README links to `13-ecosystem-integrations/README.md`.
- [ ] Korean root README links to `13-ecosystem-integrations/README.ko.md`.
- [ ] Do not add child module README links until those directories exist.
- [ ] Mention that child examples are planned under `[#137](https://github.com/bluetape4k/exposed-workshop/issues/137)`.
- [ ] Review existing root visuals (`root-readme-overview-01` and `root-readme-module-chart-01`) and either update them or record why a README-only zero-leaf chapter is intentionally excluded for this foundation PR.

## Task 5: Wire Examples Workflow Trigger

**complexity:** low

**Files:**
- Modify: `.github/workflows/examples.yml`

- [ ] Add `13-ecosystem-integrations/**` to both `push.paths` and `pull_request.paths`.
- [ ] Do not add missing chapter 13 Gradle tasks to the selected build list in this foundation PR.
- [ ] Keep the inline workflow comment clear that child module PRs must add their runnable Gradle tasks when modules exist.
- [ ] Record in DoD that this foundation PR can trigger the existing selected Examples job, but it intentionally does not add a chapter 13 task until a runnable child module exists.
- [ ] Record that Nightly remains unchanged because this foundation PR creates no runnable Testcontainers or external-service module.
- [ ] Run `actionlint .github/workflows/examples.yml`.
- [ ] Run `rg "\\\\'" .github/workflows`.

## Task 6: Verification And Documentation Checks

**complexity:** low

**Files:**
- Check all changed files.

- [ ] Run `git diff --check`.
- [ ] Run `./gradlew projects --quiet`.
- [ ] Confirm the `./gradlew projects --quiet` output does not contain planned chapter 13 child projects such as `:01-bigquery-dry-run`, because the foundation PR has no child `build.gradle.kts` yet.
- [ ] Run `actionlint .github/workflows/examples.yml`.
- [ ] Run README reference checks:
  `rg -n "13-ecosystem-integrations|13-ecosystem-integrations-architecture-01" README.md README.ko.md 13-ecosystem-integrations/README.md 13-ecosystem-integrations/README.ko.md`.
- [ ] Verify real Markdown link targets for the root and chapter README pair.
- [ ] Verify the chapter README files exist:
  `test -f 13-ecosystem-integrations/README.md && test -f 13-ecosystem-integrations/README.ko.md`.
- [ ] Verify both chapter README files contain eight explicit child issue links:
  `rg -c "https://github.com/bluetape4k/exposed-workshop/issues/13[8-9]|https://github.com/bluetape4k/exposed-workshop/issues/14[0-5]" 13-ecosystem-integrations/README.md 13-ecosystem-integrations/README.ko.md`.
- [ ] Verify both chapter README files reference the same PNG path and contain the same five credential policy bullets.
- [ ] Run a documentation secret/credential drift scan:
  `rg -in "GOOGLE_APPLICATION_CREDENTIALS|application-default|Application Default Credentials|\bADC\b|service[-_ ]account|client_secret|password|token|api[_-]?key|project[-_ ]?ids?|projectId|endpoint secret" README.md README.ko.md 13-ecosystem-integrations docs/superpowers docs/lessons docs/images/readme-diagrams .github/workflows/examples.yml`.
- [ ] Read any matches from the drift scan and classify them as policy text only or remove them before PR.
- [ ] Verify diagram sibling existence:
  `test -f docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg && test -f docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`.

## Task 7: Lessons And PR Preparation

**complexity:** low

**Files:**
- Create: `docs/lessons/2026-06-28-issue-137-ecosystem-integrations.md`

- [ ] Add a lesson recording the chapter boundary decision, workflow false-green guard, credential-free default guard, and future child implementation order.
- [ ] Commit implementation and lesson with Lore trailers.
- [ ] Create a PR that references #137 without `Closes #137`.
- [ ] Set PR assignee `debop`, milestone `exposed-1.11.0`, and labels `enhancement`, `examples`.
- [ ] Verify live PR body ends with `## DoD Status`.
