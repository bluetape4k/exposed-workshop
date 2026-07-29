# README diagram image 검증

## 배경

README diagram asset은 reviewed pastel infographic style로 다시 생성됐다.

## 결정

PNG README embed는 대응되는 SVG source와 함께 유지한다. Mermaid `init`와 theme
configuration block은 database table이 아니므로 ERD image를 rendering할 때 무시해야
한다.

## 결과

Workshop README diagram asset은 README link 변경 없이 다시 생성됐다. ERD image는 더
이상 `init:`나 `themeVariables` pseudo-table을 보여 주지 않는다. Class diagram은
inheritance arrow stem이 보이도록 더 넓은 vertical spacing을 사용한다.

## 검증

- Full regeneration: `rendered=294`, `missing=[]`.
- README image links: `missing=0`.
- Local SVG image embeds: `0`.
- Mermaid residue: `0`.
- Asset counts: `png=147`, `svg=147`.
- `init`/theme ERD residue: `0`.
- Whitespace check: `git diff --check`.

## 향후 지침

ERD에서는 renderer config block을 non-domain metadata로 취급한다. 작은 ERD에서는 text가
읽히고 relation이 숨겨지지 않는다면 compact output을 허용할 수 있다.

## 2026-05-20 Class routing follow-up

`03-exposed-basic-class-01`은 오래된 compact two-entity snapshot을 보존하지 않고 현재
`UserCities` shared sample에서 다시 만들었다. 수정된 diagram은 `CountryTable`,
`CityTable`, `UserTable`, `UserToCityTable`, `Country`, `City`, `User`를 포함하며,
table/entity mapping lane과 many-to-many relationship을 위한 routed bridge lane을
갖는다.

Source에 bridge table이나 추가 parent entity가 있다면 향후 class diagram은 대칭적인
two-by-two layout보다 source-verified relationship cluster를 우선한다.
