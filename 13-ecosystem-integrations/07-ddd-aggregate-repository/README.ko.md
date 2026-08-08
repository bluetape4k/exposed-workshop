# DDD Aggregate Lifecycle with Exposed Repository

[English](README.md) | 한국어

이 예제는 도메인 모델을 Exposed table 클래스와 분리합니다. `PurchaseOrder`
aggregate가 invariant, command method, pending domain event를 소유하고,
`OrderRepository`는 Exposed transaction 안에서 aggregate를 local H2 table로
매핑합니다.

도메인은 Bluetape4k Exposed core의 `AbstractAggregateRoot<Long>`와
`DomainEvent<Long>`를 사용합니다. Aggregate를 배치할 때
`Snowflakers.Global.nextId()`가 typed Snowflake ID를 만들고, 각 event는 해당
aggregate ID와 `Instant` 시각을 함께 보유합니다.

![DDD aggregate lifecycle with Exposed repository](../../docs/images/readme-diagrams/07-ddd-aggregate-repository-flow-01.png)

그림의 핵심은 경계입니다.

- command object가 `PurchaseOrder` aggregate를 만듭니다.
- aggregate는 SQL 코드에 들어가기 전에 `OrderPlaced`, `OrderApproved` 이벤트를
  기록합니다.
- repository는 order state, order line, domain event row를 같은 Exposed
  transaction 안에서 저장합니다.
- rollback 테스트는 event insert 이후 실패를 일으켜 order row와 event row가 함께
  commit되지 않았음을 증명합니다.

## DAO-first 예제와 다른 점

DAO/entity-first 예제는 database shape 자체가 학습 대상일 때 유용합니다. DDD
aggregate 예제는 business rule에서 시작합니다.

| DAO 또는 table-first 코드 | Aggregate-first 코드 |
|---|---|
| database column 주변 setter를 노출합니다. | `approve` 같은 command method를 노출합니다. |
| insert/update 근처에서 검증하는 경우가 많습니다. | value object와 aggregate method에서 검증합니다. |
| database row를 domain model로 다룹니다. | repository boundary에서 aggregate state를 row로 매핑합니다. |
| 명시적인 domain event 목록이 없는 경우가 많습니다. | persistence 전에 pending event를 기록합니다. |

## Domain Model

aggregate는 의도적으로 작게 유지합니다.

```kotlin
val order = PurchaseOrder.place(
    PlaceOrderCommand(
        orderNumber = OrderNumber("ORD-1000"),
        customerId = CustomerId("customer-1"),
        lines = listOf(
            OrderLine(Sku("book"), quantity = 2, Money.dollars("15.00")),
            OrderLine(Sku("course"), quantity = 1, Money.dollars("35.00")),
        ),
    )
)

order.approve(OperatorId("ops-1"))
```

Value object는 빈 identifier를 거부합니다. `OrderLine`은 0 이하 수량을 거부합니다.
aggregate는 order가 더 이상 `PLACED` 상태가 아니면 approval을 거부합니다.

## Repository Boundary

`OrderRepository.save(...)`가 Exposed transaction을 소유합니다. Aggregate는
이미 Snowflake ID를 가지므로 persistence가 별도 identity를 만들지 않습니다.

```kotlin
val savedId = transaction(db) {
    val orderId = aggregate.id
    insertOrderIfAbsent(aggregate)
    replaceOrderLines(orderId, aggregate.lines)
    updateOrder(orderId, aggregate)
    appendDomainEvents(orderId, aggregate.pendingEvents)
    orderId
}

aggregate.markEventsCommitted()
```

Domain event 목록은 transaction 성공 이후에만 비웁니다. Event insert 이후 실패가
발생하면 H2가 order, line, event row를 모두 rollback하고, aggregate에는 retry나
검사를 위한 pending event가 그대로 남습니다.
Event의 `occurredAt: Instant`는 epoch millisecond로 저장되고, aggregate ID는
`snowflakeGenerated()`가 만든 `Long` primary key로 유지됩니다.

## Tables

이 예제는 세 개의 local table을 사용합니다.

| Table | Purpose |
|---|---|
| `ddd_orders` | aggregate identity, customer id, status, version, total을 저장합니다. |
| `ddd_order_lines` | aggregate가 소유한 line item을 저장합니다. |
| `ddd_order_events` | 같은 transaction에서 capture한 순서 있는 domain event를 저장합니다. |

## 실행

```bash
./gradlew :07-ddd-aggregate-repository:test
```

예상 결과: 테스트는 in-memory H2에서 실행되고 외부 서비스에는 연결하지 않습니다.

## 검증하는 동작

테스트는 다음을 확인합니다.

- 잘못된 aggregate command는 Exposed table에 도달하기 전에 실패합니다.
- 새 aggregate를 저장하면 state와 `OrderPlaced` event가 함께 저장됩니다.
- aggregate를 다시 로드해 approve한 뒤 저장하면 `OrderApproved`가 `OrderPlaced`
  뒤에 추가됩니다.
- repository 실패를 시뮬레이션하면 aggregate row와 event row가 함께 rollback됩니다.
- 같은 aggregate를 두 번 approve하면 aggregate invariant로 실패합니다.
