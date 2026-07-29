# 2026-05-20 — README overview visual 배치

## 배경

README diagram과 chart는 decorative generated asset이 아니라 source-backed
documentation으로 취급해야 한다. 이번 pass는 2026 reference document와 shared README
diagram style guide를 사용했지만, module name과 grouping의 권위는 source code와 build
layout에 두었다.

## 결정

Root README용 English-only SVG+PNG README overview visual을 추가하고, overview diagram을
installation, usage, build instruction보다 앞에 둔다. 기존 Architecture/Diagram section이
usage example 뒤에 추가돼 있었다면 위로 옮긴다.

## 결과

`exposed-workshop`에는 이제 root README overview diagram과 module composition chart가
있고, README visual placement는 overview-first rule을 따른다. Generated label은 image
안에서 localized text를 피한다.

## 검증

- Generated SVG file은 `xmllint --noout`으로 parse했다.
- Generated PNG file은 `rsvg-convert`로 rendering했다.
- Workspace README image-link scan은 missing local image 0건을 보고했다.
- Workspace Architecture/Diagram ordering scan은 Installation, Usage, Examples,
  Build heading 뒤에 남은 section 0건을 보고했다.
- Generated root overview SVG text에는 non-ASCII character가 없었다.

## 향후 참고

Architecture diagram을 README 파일 끝에 붙이지 않는다. Overview 또는 architecture
diagram은 상단 근처에 두고, class, sequence, ERD, flow diagram은 설명하는 section
옆에 둔다.

Root overview diagram과 composition chart는 BOM이 있으면 먼저 배치하고, Examples 또는
Additional examples가 있으면 마지막에 둔다. 가운데 group은 repo-specific README가
alphabetic grouping을 요구하지 않는 한 source-backed orientation order를 유지한다.
