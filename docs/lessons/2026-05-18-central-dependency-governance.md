# 중앙 dependency governance 동기화

## 배경

Downstream Dependabot PR들이 shared dependency version을 repository별로 갱신하면서
bluetape4k 조직 전체에 version drift를 만들고 있었다.

## 결정

Shared dependency version은 먼저 `bluetape4k-dependencies`에서 변경한 뒤,
`sync-shared-versions.py`로 이 repository에 반영한다. 이 repository의 Dependabot도
centrally governed dependency 이름을 ignore하여 이후 PR이 중앙 source of truth를
통해 흐르게 한다.

## 결과

local version catalog와 `.github/dependabot.yml`이 central dependency-governance
정책을 따른다.

## 검증

- `sync-shared-versions.py --write --check --summary` for this repository
- `sync-dependabot-ignores.py --write --check --summary` for this repository
- `git diff --check`

## 향후 보호 장치

Centrally governed dependency에 대한 repo-local Dependabot PR은 merge하지 않는다.
`bluetape4k-dependencies`를 먼저 갱신한 뒤 이 repository를 동기화한다.
