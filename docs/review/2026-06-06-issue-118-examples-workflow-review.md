# Issue 118 Examples workflow review

## 범위

- `.github/workflows/examples.yml`
- `docs/lessons/2026-06-06-issue-118-examples-weekly.md`

## 리뷰 결과

- P0: 0
- P1: 0
- P2: 0

## 발견 사항

Blocking finding 없음.

## 근거

- Schedule은 weekly다: `0 21 * * 0`.
- `workflow_dispatch`, push path filter, pull request path filter가 유지된다.
- Selected downstream example module은 하나의 Gradle invocation에 남아 sequential
  Testcontainers behavior를 보존한다.
- Failed test report는 `actions/upload-artifact@v5`와 함께 upload된다.
  `if: failure()`.
- `actionlint .github/workflows/examples.yml` 통과.
- `git diff --check` 통과.

## 잔여 위험

PR 생성 전에 GitHub Actions dispatch를 수동 실행하지 않았다. 이 workflow-only 변경의 required
live validation surface는 PR check다.
