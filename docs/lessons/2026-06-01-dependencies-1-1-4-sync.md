# Dependencies 1.1.4 동기화

## 배경

`bluetape4k-dependencies` 1.2.0 release preparation에서는 BOM release CI가 통과하기 전에
downstream workshop이 central shared-version source of truth와 일치해야 한다.

## 결정

Workshop catalog를 latest published `bluetape4k-dependencies:1.1.4` baseline 및 central
shared runtime version과 정렬한다. `1.2.0`은 publish되기 전까지 소비하지 않는다.

## 결과

Workshop은 더 이상 central release preflight에서 shared-version drift를 보고하지 않는다.

## 검증

`bluetape4k-dependencies`에서 `sync-shared-versions.py --workspace
/Users/debop/work/bluetape4k --write --check --summary`로 검증했다.
