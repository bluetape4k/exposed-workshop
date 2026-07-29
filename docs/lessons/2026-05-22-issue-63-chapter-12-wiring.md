# Issue 63 Chapter 12 wiring

## 배경

Issue #63은 12장 production integration 예제를 root documentation에서 찾을 수 있게 하고
verification coverage를 명시하라고 요구했다.

## 결정

Chapter 12 module registration은 `includeModules("12-production-integration", false,
false)`를 통해 automatic으로 유지한다. Root English/Korean README module map으로
discoverability를 wiring하고, detailed paired Spring/Ktor verification command는 chapter
README에 둔다. Daily Examples workflow와 chapter-change PR이 완료된 예제를 다루도록
`.github/workflows/examples.yml`에 12장 모듈 05-10을 추가한다.

## 결과

Root README production integration section은 이제 완료된 12장 예제 01-10을 모두 나열한다.
Chapter README는 committed PNG chapter architecture diagram을 embed하고, local
verification, Examples workflow coverage, CI DB matrix coverage, 현재 self-contained
예제에 별도 nightly override가 필요 없는 이유를 기록한다.

## 검증

Documentation edit 후 root README link scan과 chapter 12 README diagram scan을 실행한다.
PR을 열기 전에 `./gradlew projects` 또는 settings scan으로 Gradle project discovery를
확인한다.

## 향후 agent 지침

새 12장 예제를 추가할 때 chapter README pair와 root README pair를 모두 갱신한다.
Architecture Diagram PNG link는 committed 상태로 유지하고, 새 예제가 external
infrastructure나 non-default CI coverage를 요구할 때만 workflow-specific entry를 추가한다.
Chapter-level module map이 바뀌면
`docs/images/readme-diagrams/12-production-integration-architecture-01.svg`와 그 PNG를
다시 생성한다.
