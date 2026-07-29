# README diagram policy 갱신

## 배경

README diagram policy는 editable SVG source를 동반한 committed PNG embed를 요구한다.
README file은 raw Mermaid block, ASCII tree diagram, Mermaid renderer type name을
user-facing diagram label로 유지해서는 안 된다.

## 결정

현재 repository structure에서 root README diagram을 다시 생성하고, Ktor 12장 architecture
example에는 dedicated package-layout diagram을 추가하며, README link는 PNG file만 가리키게
유지한다.

## 결과

Root feature map, API structure, overview, learning path, module structure,
module composition chart는 이제 refreshed SVG/PNG asset을 사용한다. Ktor package tree는 text
tree 대신 diagram asset으로 표현되고, `classDiagram`, `flowchart` 같은 Mermaid type label은
README heading과 alt text에서 제거됐다.

## 검증

- README scan으로 raw Mermaid block이 없음을 확인했다.
- README scan으로 ASCII tree diagram code block이 없음을 확인했다.
- README scan으로 local image link가 resolve되고 SVG file을 가리키지 않음을 확인했다.
- 새로 생성되거나 다시 생성된 모든 asset의 PNG dimension을 확인했다.

## 다음 작업

Mermaid 또는 structured source는 intermediate input으로만 사용한다. README에 link하기 전에
final PNG와 matching SVG source를 commit한다.
