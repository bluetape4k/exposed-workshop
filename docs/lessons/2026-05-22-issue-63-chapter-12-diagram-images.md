# Issue 63 Chapter 12 diagram image

## 배경

12장의 Spring application architecture 및 HTTP outbox/idempotency README pair에는 아직
Mermaid diagram block이 남아 있었다. 현재 README rule은 예제 architecture diagram을 PNG
image로 commit할 것을 요구한다.

## 결정

남은 Mermaid block을 `docs/images/readme-diagrams/` 아래 committed SVG+PNG asset으로
변환하고, README embed는 PNG file만 가리키게 유지한다. Mermaid source는 generated SVG
source asset으로 대표한다. 최종 PNG는 foreignObject-heavy Mermaid SVG를 `rsvg-convert`로
변환하지 않고 Mermaid CLI/Chromium으로 직접 rendering한다. 해당 변환은 box는 보이게 두면서
text를 누락할 수 있기 때문이다.

## 결과

02/03/04 English/Korean README file을 PNG diagram link가 있는 Architecture section으로
갱신했다. 변환된 각 diagram에 대응되는 SVG+PNG asset을 추가했다.

## 검증

- Chapter 12 example README scan: `readmes=8`, `missingPng=0`,
  `missingArchitecture=0`, `mermaidResidue=0`, `missingFiles=0`.
- `git diff --check` passed.
- 새 image asset 세 개의 PNG dimension을 `file`/`sips`로 검증했다.
- Follow-up correction: regenerated 02/03/04 PNG를 직접 열어 light diagram box 위 dark
  text가 보이는지 확인했다.

## 향후 참고

12장 예제를 추가할 때 README link를 넣기 전에 diagram image asset을 먼저 commit한다.
Mermaid는 final README rendering surface가 아니라 intermediate source로만 사용한다. PNG file
존재나 image dimension만으로 충분한 검증으로 보지 않는다. Rendering path마다 generated PNG를
최소 하나 열어 label이 보이는지 확인한다.
