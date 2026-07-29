# Source-verified README diagram

## 배경

High-performance cache strategy class diagram이 오래된 `entityTable` property name을
사용했다.

## 결정

Diagram member를 JDBC와 coroutine cache repository가 사용하는 현재 `table` override로
갱신한다.

## 검증

README diagram PNG를 다시 rendering하기 전에 repository source declaration을 확인한다.

## 향후 지침

Class diagram에는 현재 public/override member만 표시한다. 과거 refactoring plan에서 온
오래된 member name을 보존하지 않는다.
