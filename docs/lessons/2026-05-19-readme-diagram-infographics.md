# README 다이어그램 infographic

## 배경

README 파일들은 architecture, class, sequence, ERD 등 여러 다이어그램에 Mermaid
code block을 사용했다. Workspace-wide 시각 방향은 재사용 가능한 SVG source asset을
함께 보관하는 reviewed pastel infographic PNG로 바뀌었다.

## 결정

README Mermaid block을 generated PNG image link로 바꾸고, 대응되는 SVG source를 PNG
파일 옆에 저장한다. Diagram text는 English-only로 유지하고, 큰 label에는
Architects Daughter, detail text에는 Comic Mono를 사용하며, architecture, class,
sequence, ERD diagram마다 전용 layout을 적용한다.

## 결과

bluetape4k.github.io/docs/readme-diagram-samples의 shared 2026-05-19 style guide로
README diagram을 rendering했다. Root README asset은 존재할 때 repo-local asset
placement rule을 따른다.

## 검증

Cross-repository conversion pass에서 rsvg-convert로 PNG/SVG asset을 생성하고 README
link를 확인했다.

## 향후 지침

README diagram은 편집용 SVG source와 함께 PNG embed로 유지한다. 시각 일관성이
중요할 때 raw Mermaid나 단순 Mermaid theme recoloring으로 되돌리지 않는다.
