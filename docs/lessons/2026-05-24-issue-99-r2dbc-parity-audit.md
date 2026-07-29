# Issue 99 R2DBC parity 감사

## 배경

Ktor, 10장, 12장 example issue가 완료된 뒤 `exposed-workshop`에는
`exposed-r2dbc-workshop` 대비 최종 roadmap parity pass가 필요했다.

## 결정

Parity는 정확한 module name이 아니라 concept 기준으로 추적한다. Underlying Exposed API model이
다르면 blocking JDBC module과 R2DBC module은 별도의 architecture choice를 유지한다.

## 결과

감사는 portable gap이 없음을 확인했다. `README.md`, `README.ko.md`, research note는 이제 각
overlapping topic을 어떤 issue와 module이 다루는지 기록하고, R2DBC connection-factory
routing 및 JDBC DAO/transaction-template/cache/benchmark topic을 platform-specific으로
표시한다.

## 검증

- `exposed-workshop`에 열린 issue가 `#99`뿐임을 확인했다.
- `exposed-r2dbc-workshop`에 열린 issue가 없음을 확인했다.
- Closed roadmap issue set을 확인했다: `exposed-workshop#45`-`#63`,
  `exposed-r2dbc-workshop#32`-`#49`, `#69`, `#89`.

## 다음 작업

향후 example roadmap issue를 추가할 때는 parity table을 먼저 확인하고, portable teaching
concept에 대해서만 counterpart issue를 만든다.
