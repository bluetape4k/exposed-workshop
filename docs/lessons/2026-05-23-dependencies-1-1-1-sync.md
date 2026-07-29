# Dependencies 1.1.1 동기화

## 배경

Artifact availability audit가 publish되지 않은 mock web application module의 generated
alias를 찾은 뒤 `bluetape4k-dependencies` 1.1.0은 1.1.1로 대체됐다. 이 workshop은 shared
catalog를 소비하므로 release train과 정렬돼 있어야 한다.

## 결정

Standard shared-version sync path를 통해 `bluetape4k-dependencies = "1.1.1"`을 소비한다.
Central catalog가 이미 제거한 artifact에 대해 workshop-local override를 추가하지 않는다.

## 결과

PR #96은 이 repository를 1.1.1 catalog와 정렬했고 CI 통과 후 merge됐다.

## 검증

- GitHub PR #96 status check는 merge 전에 통과했다.
- Workspace-level `scripts/sync-shared-versions.py --workspace .. --check --summary`
  passed after the downstream PRs were merged.

## 향후 지침

Shared catalog patch가 publication availability를 수정할 때는 Maven Central `repo1`이 새
version을 resolve할 때까지 기다린 뒤 downstream CI를 다시 실행한다. 여러 bluetape4k
repository에 영향을 주는 workshop dependency drift fix는 catalog에 유지한다.
