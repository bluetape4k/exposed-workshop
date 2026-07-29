# Issue 50 Ktor chapter example wiring

## 배경

Issue #50은 새 Ktor chapter example을 documentation과 verification guidance에 연결하라고
요구했다.

## 결정

11장 Ktor module link, test task, CI/nightly coverage decision을 English/Korean README file에
모두 기록한다. Ktor module은 container-backed nightly coverage가 필요 없는 H2-only 예제로
취급한다.

## 결과

`11-high-performance/README.md`와 `README.ko.md`에 Ktor cache, coroutine cache, routing
datasource module 및 verification command를 추가했다.

## 검증

통과: `git diff --check`.

## 향후 지침

Feature PR에 의존하는 docs PR은 아직 merge되지 않은 module directory를 참조할 수 있지만,
PR body에 prerequisite module PR을 명확히 나열해야 한다.
