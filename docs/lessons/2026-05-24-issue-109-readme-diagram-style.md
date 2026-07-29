# Issue 109 README diagram style

## 배경

README diagram asset에는 Mermaid가 직접 rendering한 SVG/PNG pair가 많았고, 일부 newer
diagram은 workspace typography contract를 지키지 않았다.

## 결정

`docs/assets/readme-diagrams/`와 `docs/images/readme-diagrams/` 아래 모든 README diagram
asset을 recovered label과 deterministic pastel SVG template로 다시 생성한다. README file의
PNG link와 PNG 옆 matching SVG source를 유지한다.

## 결과

Mermaid-rendered asset을 custom architecture, class, sequence, ERD diagram으로 교체했다.
Large label은 Architects Daughter를 사용하고, detail text는 Comic-style fallback stack을
사용하며, connector routing은 box 사이 gap에 머문다.

## 검증

- README-linked diagram assets: `missing=0`.
- README local SVG diagram links: `0`.
- README-linked PNG diagrams without sibling SVG: `0`.
- README diagram SVGs with Mermaid renderer signatures: `0`.
- README diagram SVGs missing Architects Daughter: `0`.
- PNG render files missing or tiny: `0`.

## 향후 지침

README diagram regeneration pass에서는 chart asset을 제외한다. Task가 chart redraw를
명시적으로 요구하지 않는 한 target set은 `readme-diagrams/`로 제한한다.
