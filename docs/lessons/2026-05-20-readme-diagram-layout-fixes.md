# README diagram layout 수정

## 배경

Follow-up visual QA에서 생성된 README diagram의 layout defect 두 가지를 찾았다.

- 일부 architecture connector는 arrow head만 보일 정도로 매우 짧은 line segment로 rendering됐다.
- sequence participant header label은 header box의 위쪽으로 치우쳐 있었다.

관련 sequence 문제도 함께 수정했다. self-call이 이전에는 zero-length arrow로 rendering되어
독립된 arrow head처럼 보였기 때문이다.

## 결정

기존 diagram style은 유지하고 generated SVG/PNG asset의 geometry만 갱신한다.
Architecture connector line segment는 인접 card 사이의 visible gap을 가로질러야 한다.
Sequence participant label은 architecture card와 같은 vertical-centering baseline을
사용해야 한다. Sequence self-call은 zero-length line 대신 작은 loop로 rendering해야 한다.

## 검증

- README image link check: missing=0, localSvgImageLinks=0, mermaidResidue=0
- PNG/SVG shape check: shapeCandidates=0
- architecture short connector check: shortArch=0
- sequence header alignment check: seqTop=0
- sequence zero-length arrow check: zeroSeq=0
- `git diff --check`
- exposed root architecture와 대표 sequence diagram의 visual sample을 검토했다.

## 향후 지침

SVG가 문법적으로 유효하더라도 arrow head-only connector는 rendering 실패로 취급한다.
PR 생성 전에 geometry check가 architecture connector length, sequence header baseline,
sequence self-call arrow를 다루도록 한다.
