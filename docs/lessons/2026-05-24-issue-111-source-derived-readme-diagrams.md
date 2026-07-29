# Issue 111 Source-derived README diagram

## 배경

첫 README diagram refresh는 여전히 기존 SVG label에 과도하게 의존했다. 그 결과 stale
Mermaid-like structure가 보존되고 non-guide asset이 남을 수 있었다.

## 결정

README context와 현재 Kotlin source에서 README diagram asset을 다시 생성한다. Parent
directory에 direct source set이 없으면 parent README diagram은 child Gradle module source를
포함해야 한다. Localized README는 image text를 결정하지 않는다. Image title과 label에는
`README.md`를 preferred source로 사용한다.

## 결과

175개 README diagram SVG/PNG pair를 모두 다시 생성했다. 결과물은 source-derived architecture
panel, concrete implementation 위에 supertype을 두는 UML-like class section, sequence
interaction band, FK relationship을 갖춘 ERD table compartment를 포함한다.

## 검증

- `node scripts/regenerate-readme-diagrams.js`
- README diagram audit: `missing=0`, `svgRefs=0`, `fontMissing=0`, `rawMermaid=0`, `nonEnglish=0`
- `git diff --check`

## 향후 지침

SVG text를 제자리에서 편집해 diagram asset을 수리하지 않는다. Source model 또는 generator를
먼저 수정한 뒤 SVG와 PNG를 함께 다시 생성한다.
