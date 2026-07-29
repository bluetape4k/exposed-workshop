# Issue #145 DDD Modulith boundary lesson

## 배경

모듈은 Spring Modulith verification과 Exposed persistence로 DDD bounded context를 보여 준다.

## 결정

Schema initialization은 각 module package 안에 둔다. `orders.internal` 또는
`shipping.internal`을 import하는 shared root initializer 자체가 boundary violation이며,
`ApplicationModules.verify()`가 이를 올바르게 거부한다.

## 결과

예제는 `orders.events`를 single named interface로 사용하고, direct
`shipping -> orders.internal` dependency가 verification에 실패함을 증명하는 negative test
fixture를 포함한다.

## 향후 지침

Modulith 예제에서는 architecture를 문서화하기 전에 `ApplicationModules.verify()`를 실행한다.
Failure는 단순 test failure가 아니라 실제 design feedback으로 취급한다.
