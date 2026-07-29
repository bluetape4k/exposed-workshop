# Code scanning alert

## 배경

GitHub CodeQL은 CI, Nightly, Examples의 workflow token permission alert와 README diagram
tooling 및 checked-in Gatling report의 JavaScript alert를 보고했다.

## 결정

먼저 workflow-level `contents: read` permission을 명시하고, 이후 alert가 난 JavaScript
helper를 고치며, generated report artifact가 source documentation이 아니면 제거한다.

## 결과

Checkout 기반 job의 workflow token default는 이제 least-privilege다. Static resource fix는
alert가 난 file로 범위를 제한한다.

## 검증

- `actionlint .github/workflows/ci.yml .github/workflows/nightly.yml .github/workflows/examples.yml`
- `yq` inspection of workflow permissions
- `git diff --check`

## 향후 보호 장치

Generated benchmark/report asset이 documentation으로 적극 유지되고 static web content로
CodeQL을 통과하지 않는 한 source control에 넣지 않는다.
