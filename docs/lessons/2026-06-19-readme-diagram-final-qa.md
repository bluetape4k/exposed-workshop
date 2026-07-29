# README diagram final QA

## 배경

README refresh는 root workshop과 모든 module의 source-derived diagram을 다시 생성했다.
Final pass에서는 모든 SVG를 PNG로 rendering하고, connector placement, balanced margin, text
overflow를 보기 위해 grouped contact sheet를 검토했다.

## 결정

Diagram verification에는 render success와 visual layout check가 모두 포함돼야 한다. XML
validity와 successful PNG generation은 필요하지만, oversized canvas, uneven whitespace,
README rendering에서 너무 작아지는 card를 잡아내지는 못한다.

## 결과

Final QA는 content가 canvas의 첫 1/4 지점에서 끝나는데 SVG viewBox는 오래된 height를
유지하던 반복 JPA basic ERD asset 네 개를 찾았다. 수정은 canvas/frame height를 줄이고
paired PNG file을 다시 생성했다.

## 검증

- 175개 SVG diagram 전체를 CairoSVG로 rendering했다.
- 175개 SVG file 전체의 SVG XML validity를 확인했다.
- 152개 README file 전반의 README image reference를 검증했다.
- Sequence `alt` region에 near-transparent fill이 있는지 감사했다.
- Architecture, class, sequence, ERD, miscellaneous contact sheet에서 connector,
  margin, text fit issue를 검사했다.

## 다음 작업

광범위한 README diagram 작업에서는 visual pass 전에 image dimension ratio sweep을 추가한다.
비정상적으로 높거나 넓은 diagram을 표시한 뒤, commit 전에 rendered PNG를 검사한다.
