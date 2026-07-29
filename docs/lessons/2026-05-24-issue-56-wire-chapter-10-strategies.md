# Issue 56 Chapter 10 strategy wiring

## 배경

Issue #56은 10장 Spring Boot strategy example을 documentation과 verification에 연결하라고
요구했다.

## 결정

Tenant onboarding module을 module link, test/build task, CI/nightly coverage decision과 함께
10장 English/Korean README file에 추가한다.

## 결과

`08-tenant-onboarding-spring-web`를 위해 `10-multi-tenant/README.md`와 `README.ko.md`를
갱신했다.

## 검증

통과: `git diff --check`.

## 향후 지침

Strategy wiring docs PR은 dependent feature PR을 나열하고, 새 module이 nightly container
coverage를 요구하는지 기록해야 한다.
