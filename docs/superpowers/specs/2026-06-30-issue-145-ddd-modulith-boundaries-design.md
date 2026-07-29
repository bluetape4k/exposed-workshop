# Issue #145 DDD Modulith boundary 설계

## 배경

Issue #145는 DDD bounded context, Spring Modulith boundary verification, Exposed
persistence를 조합하는 runnable workshop example을 추가한다. 이 예제는
`13-ecosystem-integrations/08-ddd-modulith-boundaries`에 속한다.

## 요구사항

- 별도 package, Exposed table, repository, transaction scope를 가진 bounded context를 최소 두
  개 model한다.
- 다른 context의 direct repository access를 허용하지 않고 order context에서 domain event를
  publish한다.
- Exported event package를 Spring Modulith named interface로 표시한다.
- Consuming context는 해당 named interface에만 의존할 수 있도록 `allowedDependencies`로
  설정한다.
- `ApplicationModules.verify()`를 사용하는 positive verifier test를 포함한다.
- 다른 context의 internal repository에 대한 direct dependency가 boundary verification에
  실패함을 증명하는 negative test fixture를 포함한다.
- 예제는 H2와 no external credential 기반 local-first로 유지한다.
- `bluetape4k-diagram` checklist와 visual inspection을 통과하는 bilingual README file 및
  generated diagram asset을 제공한다.
- 모듈을 Chapter 13 docs와 examples workflow에 등록한다.

## 설계

모듈은 두 Spring Modulith module을 갖는다.

- `orders`: order를 받고 Exposed로 저장하며 `OrderAcceptedEvent`를 publish한다.
- `shipping`: `OrderAcceptedEvent`를 listen하고 자체 Exposed table/repository로 shipment
  reservation을 저장한다.

`orders.events` package는 유일한 exported named interface다. `shipping` module metadata는
`allowedDependencies = ["orders :: events"]`를 선언한다. Negative test fixture는
`shipping`에서 `orders.internal` repository를 import하며, 이는 verification에 실패해야 한다.

## 비목표

- Distributed event broker나 external database 없음.
- REST API 없음. Test와 README가 workshop interface다.
- Chapter 13 Modulith 예제가 이미 사용하는 Spring Modulith 및 Exposed dependency 외에 새
  production dependency 없음.

## 검증

- Production code가 존재하기 전 red test run:
  `./gradlew :08-ddd-modulith-boundaries:test --no-daemon --no-configuration-cache --rerun-tasks`
- Targeted test 및 build:
  `./gradlew :08-ddd-modulith-boundaries:test --no-daemon --no-configuration-cache --rerun-tasks`
  `./gradlew :08-ddd-modulith-boundaries:build --no-daemon --no-configuration-cache --rerun-tasks --warning-mode all`
- Registration 및 docs:
  `./gradlew projects --no-daemon --no-configuration-cache`
  `actionlint .github/workflows/examples.yml`
  `git diff --check`
- Diagram:
  `bluetape4k-diagram` audit를 실행하고 generated PNG를 full size로 검사한다.
