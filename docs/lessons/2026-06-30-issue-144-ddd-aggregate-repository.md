# 2026-06-30 Issue #144 - DDD aggregate repository 예제

## 배경

Issue #144는 첫 13장 domain-architecture 예제를 추가한다. 이 모듈은 Exposed table class를
domain model로 바꾸지 않으면서 DDD aggregate를 보여 줘야 했다.

## 결정

Value object, command method, pending event를 가진 작은 `PurchaseOrder` aggregate와, 하나의
Exposed transaction 안에서 state, owned line, event row를 mapping하는 `OrderRepository`를
사용한다.

## 결과

예제는 H2 기반 local-first로 유지된다. Test는 aggregate invariant, persistence, event insertion
후 rollback, domain event capture order를 다룬다.

## 향후 보호 장치

다음 DDD 또는 Modulith 예제에서는 먼저 test에서 aggregate/business boundary를 보이게 한 뒤
Exposed mapping과 diagram을 추가한다. Table class가 aggregate API가 되게 하지 않는다.
