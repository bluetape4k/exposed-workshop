# Chapter 13 diagram connector QA

## 배경

Ktor Exposed integration README diagram은 SVG syntax와 geometry check를 통과했지만,
rendered PNG에서는 resource connector가 local-resource lane title/note text를 가로질렀고,
나중에도 resource lane 전체를 감싸는 큰 connector frame처럼 보였다. 이후 review는 더 심각한
누락도 잡았다. `testApplication`에서 `/healthz + /readyz`로 가는 connector가 `/api/notes`
card를 직접 통과했다. 또 다른 review는 같은 path 안에서 둥근 첫 bend와 날카로운 두 번째
bend를 섞은 helper connector를 발견했다.

## 결정

Connector-heavy diagram은 더 긴 outer detour를 추가하는 방식으로 고치지 않는다. 먼저 routing
pressure를 줄인다. Source-backed distinction이 같은 reader-facing card 안에 있을 수 있으면
card를 합치고, 실제 relationship 주변으로 card를 재배치하며, lane title/note band를 no-flow
zone으로 유지한다. Static audit은 README diagram에 필요하지만 충분하지는 않다.

## 결과

`05-ktor-exposed-integration-architecture-01.svg`는 이제 JDBC datasource와 dispatcher를 하나의
caller-owned resource card로 합치고, resource connector를 짧고 구분 가능하게 유지하며 lane
text에서 떨어뜨린다. Health-route HTTP connector는 더 이상 `/api/notes` card를 가로지르지
않고 upper bypass path를 사용한다. Helper connector path는 rounded-then-sharp mix 대신
matching two-corner rounded S-bend를 사용한다.

## 검증

- `xmllint --noout` for the six Chapter 13 README SVGs.
- `diagram-geometry-audit.py` for the six Chapter 13 README SVGs.
- `diagram-endpoint-audit.py` for the Chapter 13 architecture SVGs.
- `diagram-sequence-style-audit.py` for the sequence SVGs.
- Marker color audit, segment-crossing audit, and diagonal-connector audit for
  the touched SVG.
- Card-intrusion sampling audit for connector paths against rendered card
  rectangles in the touched SVG.
- Mixed-corner audit that rejects connector paths where a `Q` bend is followed
  by an immediate orthogonal `L` turn without a second rounded corner.
- Full-size rendered PNG inspection plus a six-diagram contact sheet.

## 향후 작업

Connector-heavy README diagram에서는 diagram checklist 통과를 보고하기 전에 full-size PNG를
검사한다. Contact sheet는 coverage 확인에는 유용하지만 dense connector area에 대한 targeted
full-size inspection을 대체하지 않는다. Connector가 lane border나 frame처럼 보이기 시작하면
다른 path-only patch를 시도하기 전에 card grouping과 port를 다시 설계한다. Connector가
시각적으로 card body를 가로지르면 endpoint나 corner audit에만 기대지 않는다. Push 전에
path-vs-card interior sampling check를 실행하고 full-size PNG를 검사한다. Path에 rounded
corner가 하나라도 있으면 같은 path의 나머지와 same-class sibling을 감사한다. 한 bend의 `Q`
command가 인접 `L` turn을 허용하지는 않는다.
