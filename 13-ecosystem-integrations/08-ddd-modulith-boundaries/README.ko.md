# DDD Bounded Context and Modulith Boundary Verification

[English](README.md) | 한국어

이 예제는 작은 DDD 흐름에 Spring Modulith boundary verification을 결합합니다.
Order context는 order persistence를 소유하고, `OrderAcceptedEvent` named
interface만 외부에 공개합니다. Shipping context는 그 event에만 반응하며 order
repository나 table을 직접 import하지 않습니다.

![DDD bounded contexts with Spring Modulith verification](../../docs/images/readme-diagrams/08-ddd-modulith-boundaries-flow-01.png)

## 예제가 증명하는 것

- `orders`와 `shipping`은 서로 다른 Spring Modulith module입니다.
- 각 context는 자기 Exposed table과 repository를 `internal` package에 둡니다.
- `orders.events`만 order context의 named interface로 공개합니다.
- `shipping`은 `allowedDependencies = ["orders :: events"]`만 허용합니다.
- 정상 애플리케이션은 `ApplicationModules.verify()`를 통과합니다.
- `shipping`이 `orders.internal.LeakyOrderRepository`를 import하는 negative
  fixture는 Spring Modulith `Violations`로 실패합니다.

## Boundary 구조

Production package는 architecture를 그대로 반영합니다.

| Package | Role |
|---|---|
| `orders` | order application service와 module metadata를 둡니다. |
| `orders.events` | `OrderAcceptedEvent`를 포함하는 exported named interface입니다. |
| `orders.internal` | Exposed order table과 repository를 둡니다. |
| `shipping` | event listener, read model, module metadata를 둡니다. |
| `shipping.internal` | Exposed shipping table과 repository를 둡니다. |

Root package에는 Spring Boot entrypoint와 공통 transaction manager 설정만 둡니다.
Schema initialization은 각 bounded context 안에 두어 root package가 module
internal에 의존하지 않게 했습니다.

## Event Handoff

Order context는 command를 받아 자기 Exposed repository로 order를 저장한 뒤 domain
event를 발행합니다.

```kotlin
val order = orderService.accept(
    AcceptOrderCommand(orderKey = "order-ddd-001", customerId = "customer-42")
)
```

Shipping context는 order transaction commit 이후 `OrderAcceptedEvent`를 listen하고,
자기 Exposed repository와 transaction boundary를 통해 reservation을 저장합니다.
Handler는 order table이나 repository를 import하지 않습니다.

## Negative Fixture

`src/test/kotlin/.../invalid` 아래 test fixture는 같은 module metadata를 반복하되,
다음 금지된 의존성을 추가합니다.

```kotlin
import exposed.examples.spring.modulith.boundaries.invalid.orders.internal.LeakyOrderRepository
```

Verifier test는 ArchUnit의 `ImportOption.Predefined.DO_NOT_INCLUDE_JARS`로 test class를
포함한 뒤, Spring Modulith가 `orders.internal` 의존성을 violation으로 보고하는지
확인합니다.

## 실행

```bash
./gradlew :08-ddd-modulith-boundaries:test
```

예상 결과: 정상 애플리케이션은 boundary verification을 통과하고, negative fixture는
테스트 assertion 안에서 verification 실패로 잡힙니다. 이 모듈은 local H2만 사용하며
외부 서비스에 연결하지 않습니다.

## 검증하는 동작

테스트는 다음을 확인합니다.

- 정상 module은 `ApplicationModules.verify()`를 통과합니다.
- `shipping`에서 `orders.internal`로 향하는 직접 의존성은 거부됩니다.
- order accept가 `ddd_modulith_orders`에 저장됩니다.
- 발행된 domain event가 `ddd_modulith_shipping_reservations` row를 만듭니다.
