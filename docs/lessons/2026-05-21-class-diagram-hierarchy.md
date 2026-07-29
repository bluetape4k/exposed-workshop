# Class diagram hierarchy 감사

## 배경

Mermaid-to-SVG 변환이 inheritance 또는 implementation arrow가 parent contract를 향해
아래로 향하는 layout을 보존하면 README class diagram이 오해를 줄 수 있다.

## 결정

Inheritance 또는 implementation edge가 있으면 interface, abstract, base contract node를
implementor나 subclass보다 위에 둔다. Open-triangle marker가 parent node에 닿도록 해당
edge를 orthogonal path로 다시 routing한다.

## 결과

Exposed workshop README class diagram asset은 top-down으로 다시 배치했고, 수정된 SVG
source에서 PNG를 다시 생성했다.

## 검증

- 모든 workspace class SVG에서 downward `inheritLine`, `implLine` endpoint를 검사했다: `COUNT 0`.
- 변경된 PNG asset을 `rsvg-convert`로 다시 rendering했다.
- 변경된 SVG file을 `xmllint --noout`으로 검증했다.

## 향후 지침

README class diagram을 publish하기 전에 `docs/images/readme-diagrams/*class*.svg`에 대해
inheritance-direction scan을 실행하고, 변경된 diagram family마다 rendered PNG를 최소
하나씩 시각적으로 확인한다.
