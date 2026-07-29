# Issue #137 Ecosystem integrations chapter 구현 계획

> **Agentic worker용:** REQUIRED SUB-SKILL: 이 계획은 task-by-task로 구현하기 위해 superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans를 사용한다. 단계 추적에는 checkbox(`- [ ]`) syntax를 사용한다.

**목표:** Child example이 구현됐다고 주장하지 않으면서 issue #137을 위한
`13-ecosystem-integrations` chapter foundation을 추가한다.

**아키텍처:** PR은 README-only chapter overview를 만들고 chapter scan hook을 등록하며 root docs와
Examples workflow path trigger를 연결한다. Child issue #138-#145는 나중에 runnable Gradle module을
만들며, 각자 workflow build task를 추가해야 한다.

**기술 스택:** Gradle Kotlin DSL, GitHub Actions, Markdown README pair, committed SVG/PNG README
diagram asset.

---

## 파일 구조

- 생성: `13-ecosystem-integrations/README.md`
- 생성: `13-ecosystem-integrations/README.ko.md`
- 생성: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`
- 생성: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`
- 수정: `settings.gradle.kts`
- 수정: `README.md`
- 수정: `README.ko.md`
- 수정: `AGENTS.md`
- 수정: `.github/workflows/examples.yml`
- 생성: `docs/lessons/2026-06-28-issue-137-ecosystem-integrations.md`

## 작업 0: Design artifact commit

**complexity:** low

**파일:**
- 생성: `docs/superpowers/specs/2026-06-28-issue-137-ecosystem-integrations-design.md`
- 생성: `docs/superpowers/plans/2026-06-28-issue-137-ecosystem-integrations-plan.md`

- [ ] 확인: Step 2-R 및 Step 3-R review gate가 P0=0, P1=0이다.
- [ ] commit: Implementation edit 전에 spec과 plan을 commit한다.
- [ ] 검토 evidence를 기록하는 Lore-style commit message를 사용한다.

## 작업 1: Chapter boundary 등록

**complexity:** low

**파일:**
- 수정: `settings.gradle.kts`
- 수정: `AGENTS.md`

- [ ] 추가: `settings.gradle.kts`의 chapter 12 바로 뒤에
  `includeModules("13-ecosystem-integrations", false, false)`.
- [ ] 갱신: `AGENTS.md` layout table에 `12-production-integration`과
  `13-ecosystem-integrations`를 모두 포함한다.
- [ ] Task 2가 base directory를 만든 뒤까지 Gradle project verification을 미룬다.

## 작업 2: Chapter README pair 추가

**complexity:** low

**파일:**
- 생성: `13-ecosystem-integrations/README.md`
- 생성: `13-ecosystem-integrations/README.ko.md`

- [ ] 추가: `English | [한국어](README.ko.md)`가 있는 English README.
- [ ] 추가: `[English](README.md) | 한국어`가 있는 Korean README.
- [ ] 포함: Shared architecture PNG reference:
  `![Chapter 13 ecosystem integrations architecture](../docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png)`.
- [ ] 포함: #138-#145의 planned issue table. Status `Planned`, full planned directory, Gradle
  task, README title, lane을 포함한다.
- [ ] 두 README table에서 예시처럼 explicit GitHub issue link를 사용한다:
  `[#138](https://github.com/bluetape4k/exposed-workshop/issues/138)`.
- [ ] 포함: 다음 다섯 explicit bullet이 있는 external service 및 credential policy:
  no checked-in credentials/tokens/service-account files/project IDs/endpoint secrets;
  no default ADC or local credential file use;
  fake/local/Testcontainers/emulator-style defaults;
  real-service execution is explicit opt-in and skipped by default in CI;
  README warnings for cost, network, and credentials before any real-service command.
- [ ] 회피: 존재하지 않는 child README file link.
- [ ] 추가: Future child implementation order note. Module과 `build.gradle.kts`를 만들고, Gradle
  project discovery를 검증하며, file이 존재한 뒤에만 chapter/root README link를 추가하고,
  적절한 Examples 또는 Nightly module build task를 추가하며, credential/coverage lane decision을
  기록한다.
- [ ] 추가: Future child workflow-lane checklist. Default local/fake path, real-service opt-in
  property/tag, selected workflow lane(weekly Examples, full Nightly, manual opt-in)을 분리한다.

## 작업 3: Chapter diagram asset 추가

**complexity:** medium

**파일:**
- 생성: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`
- 생성: `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`

- [ ] 생성: Database platform adapters, Runtime/framework integration, Domain architecture 세 lane을
  보여 주는 source-backed SVG.
- [ ] `Architects Daughter`와 `Comic Mono` font family를 사용한다.
- [ ] 유지: 최종 diagram text는 reader-facing text만 둔다.
- [ ] validate: `xmllint --noout docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`로 XML을 검증한다.
- [ ] SVG가 non-empty `viewBox`, explicit `width`, explicit `height`를 갖는지 확인한다.
- [ ] `~/.local/bin/cairosvg docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg -o docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png -s 2`로 PNG를 render한다.
- [ ] `view_image`로 rendered PNG를 검사한다.
- [ ] PNG inspection 중 text overflow, card/connector overlap, excessive whitespace가 없고,
  three-lane grouping이 readable하며 planned child example 여덟 개가 모두 표현됐는지 검증한다.

## 작업 4: Root README pair 연결

**complexity:** low

**파일:**
- 수정: `README.md`
- 수정: `README.ko.md`

- [ ] 추가: Chapter overview만 가리키는 root module-list entry.
- [ ] English root README는 `13-ecosystem-integrations/README.md`로 link한다.
- [ ] Korean root README는 `13-ecosystem-integrations/README.ko.md`로 link한다.
- [ ] Child module directory가 존재하기 전에는 child module README link를 추가하지 않는다.
- [ ] Child example이 `[#137](https://github.com/bluetape4k/exposed-workshop/issues/137)` 아래 planned 상태임을 언급한다.
- [ ] 기존 root visual(`root-readme-overview-01`, `root-readme-module-chart-01`)을 review하고
  update하거나, README-only zero-leaf chapter를 이 foundation PR에서 의도적으로 제외한 이유를
  기록한다.

## 작업 5: Examples workflow trigger 연결

**complexity:** low

**파일:**
- 수정: `.github/workflows/examples.yml`

- [ ] 추가: `push.paths`와 `pull_request.paths` 모두에 `13-ecosystem-integrations/**`.
- [ ] 이 foundation PR에서는 아직 없는 chapter 13 Gradle task를 selected build list에 추가하지 않는다.
- [ ] 유지: Child module PR은 module이 존재할 때 runnable Gradle task를 추가해야 한다는 inline workflow
  comment를 명확하게 둔다.
- [ ] 기록: 이 foundation PR은 기존 selected Examples job을 trigger할 수 있지만 runnable child module이
  존재하기 전까지 chapter 13 task를 의도적으로 추가하지 않는다는 점을 DoD에 기록한다.
- [ ] 기록: 이 foundation PR은 runnable Testcontainers 또는 external-service module을 만들지 않으므로
  Nightly가 변경되지 않는다는 점.
- [ ] 실행: `actionlint .github/workflows/examples.yml`.
- [ ] 실행: `rg "\\\\'" .github/workflows`.

## 작업 6: 검증과 문서 검사

**complexity:** low

**파일:**
- 변경된 모든 file을 검사한다.

- [ ] 실행: `git diff --check`.
- [ ] 실행: `./gradlew projects --quiet`.
- [ ] 확인: Foundation PR에는 아직 child `build.gradle.kts`가 없으므로 `./gradlew projects --quiet`
  output에 `:01-bigquery-dry-run` 같은 planned chapter 13 child project가 없는지 확인한다.
- [ ] 실행: `actionlint .github/workflows/examples.yml`.
- [ ] 실행: README reference check:
  `rg -n "13-ecosystem-integrations|13-ecosystem-integrations-architecture-01" README.md README.ko.md 13-ecosystem-integrations/README.md 13-ecosystem-integrations/README.ko.md`.
- [ ] 검증: Root 및 chapter README pair의 real Markdown link target.
- [ ] 검증: Chapter README file 존재 여부:
  `test -f 13-ecosystem-integrations/README.md && test -f 13-ecosystem-integrations/README.ko.md`.
- [ ] 검증: 두 chapter README file이 8개의 explicit child issue link를 포함하는지:
  `rg -c "https://github.com/bluetape4k/exposed-workshop/issues/13[8-9]|https://github.com/bluetape4k/exposed-workshop/issues/14[0-5]" 13-ecosystem-integrations/README.md 13-ecosystem-integrations/README.ko.md`.
- [ ] 검증: 두 chapter README file이 같은 PNG path를 reference하고 동일한 다섯 credential policy bullet을
  포함하는지.
- [ ] 실행: Documentation secret/credential drift scan:
  `rg -in "GOOGLE_APPLICATION_CREDENTIALS|application-default|Application Default Credentials|\bADC\b|service[-_ ]account|client_secret|password|token|api[_-]?key|project[-_ ]?ids?|projectId|endpoint secret" README.md README.ko.md 13-ecosystem-integrations docs/superpowers docs/lessons docs/images/readme-diagrams .github/workflows/examples.yml`.
- [ ] 읽기: Drift scan match를 확인하고 policy text only로 분류하거나 PR 전에 제거한다.
- [ ] 검증: Diagram sibling 존재 여부:
  `test -f docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg && test -f docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.png`.

## 작업 7: Lesson 및 PR 준비

**complexity:** low

**파일:**
- 생성: `docs/lessons/2026-06-28-issue-137-ecosystem-integrations.md`

- [ ] 추가: Chapter boundary decision, workflow false-green guard, credential-free default guard,
  future child implementation order를 기록하는 lesson.
- [ ] commit: Implementation과 lesson을 Lore trailer로 commit한다.
- [ ] 생성: `Closes #137` 없이 #137을 reference하는 PR.
- [ ] PR assignee는 `debop`, milestone은 `exposed-1.11.0`, label은 `enhancement`, `examples`로
  설정한다.
- [ ] 검증: Live PR body는 `## DoD Status`로 끝난다.
