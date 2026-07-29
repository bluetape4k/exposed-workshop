# Issue 118 Examples weekly gate

## 배경

`exposed-workshop`에는 이미 `Examples` workflow가 있었지만, issue #118은 manual dispatch와
PR/push path filter를 보존하면서 downstream example gate를 daily가 아니라 weekly로 실행하라고
요구했다.

## 결정

- Selected downstream example module에는 weekly scheduled run 하나를 사용한다.
- Testcontainers-backed path가 workflow job 사이에서 경쟁하지 않도록 selected example을
  하나의 Gradle invocation에 유지한다.
- Passing run을 가볍게 유지하기 위해 failure일 때만 test report를 upload한다.

## 결과

Workflow는 CI 및 Nightly와 분리된 상태를 유지하고, `workflow_dispatch`와 path filter를
보존하며, selected gate scope를 inline으로 문서화하고, 실패한 example report를 artifact로
publish한다.

## 검증

- `actionlint .github/workflows/examples.yml`
- `git diff --check`

## 향후 지침

새 downstream example을 추가할 때는 module을 CI나 Nightly로 옮기기보다 selected Examples
workflow scope 확장을 우선한다.
