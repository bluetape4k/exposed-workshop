# README Class/ERD routing

## 배경

README class와 ERD image는 documentation, blog post, presentation 재사용을 위해
bluetape4k workspace 전반에서 다시 생성됐다.

## 결정

Class와 ERD diagram에는 blocker-aware lane selection을 갖춘 orthogonal connector
routing을 사용한다. Pastel color와 기존 typography는 유지하되, cubic curve와 component
내부를 가로지르는 connector path는 피한다.

## 결과

다시 생성된 class/ERD SVG는 relation-aware component placement, straight
horizontal/vertical lane, 더 작은 arrow marker, vertical first/final segment를 갖는
top/bottom port, component edge 대신 row midline 근처에 배치된 horizontal lane을
사용한다.

## 검증

- `node --check .omx/scripts/refine-readme-diagrams.mjs`
- 변경된 class/ERD SVG: cubic connector count `0`
- 변경된 class/ERD SVG: card-interior crossing candidate `0`

## 향후 지침

Diagram을 다시 생성할 때는 blocker-aware route scoring을 유지하고, 광범위한 image churn을
받아들이기 전에 contact sheet를 검토한다.
