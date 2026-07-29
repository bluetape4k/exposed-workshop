# Dependencies 1.2.0 동기화

## 배경

Final upstream BOM matrix가 Maven Central-visible 상태가 된 뒤
`bluetape4k-dependencies:1.2.0`이 publish됐다.

## 결정

Exposed workshop shared catalog를 `1.1.4`에서 `1.2.0`으로 이동한다.

## 결과

Workshop example은 이제 published 1.2.0 dependency-governance baseline을 소비한다.

## 검증

- `sync-shared-versions.py --workspace .. --write --check --summary` updated
  the catalog line.
- Maven Central은 `io.github.bluetape4k:bluetape4k-dependencies:1.2.0`에 대해 HTTP 200을
  반환했다.
