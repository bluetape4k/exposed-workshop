# Issue #145 DDD Modulith boundary 계획

## 단계 1 - Test 우선 scaffold

작업:
- `08-ddd-modulith-boundaries` Gradle module skeleton을 추가한다.
- Positive Modulith verification, negative boundary violation fixture, event-driven persistence
  handoff를 위한 Spring Boot integration test를 추가한다.
- Production code를 추가하기 전에 module test command를 실행하고 expected failure를 기록한다.

DoD:
- Test는 compile되거나 missing production symbol 때문에만 실패한다.
- Failure는 새 module이 아직 구현되지 않았음을 증명한다.

## 단계 2 - Bounded context 구현

작업:
- `orders`와 `shipping` bounded context를 구현한다.
- Exposed table과 repository를 각 context 내부로 유지한다.
- `orders.events` named interface를 통해 `OrderAcceptedEvent`를 publish/consume한다.

DoD:
- Positive verifier test가 통과한다.
- Event handoff test는 `shipping`이 `orders.internal` direct access 없이 자체 table에 write함을
  증명한다.

## 단계 3 - 문서와 diagram

작업:
- `README.md`, `README.ko.md`, generated SVG/PNG diagram을 추가한다.
- Chapter 13 및 root README link를 갱신한다.

DoD:
- 두 README file은 DDD/Modulith/Exposed flow를 설명한다.
- Diagram은 automated audit와 full-size visual inspection을 통과한다.

## 단계 4 - Register CI Surface

작업:
- Module build task를 `.github/workflows/examples.yml`에 추가한다.
- Gradle project listing으로 module registration을 검증한다.

DoD:
- `:08-ddd-modulith-boundaries` appears in `./gradlew projects`.
- Workflow syntax는 validate된다 with `actionlint`.

## 단계 5 - 검토 및 PR

작업:
- Targeted tests/build, diff check, verifier checklist, review gate를 실행한다.
- Lore trailer로 commit하고 issue #145용 PR을 연다.

DoD:
- PR은 issue assignee, milestone, label을 반영한다.
- PR body는 `## DoD Status`로 끝난다.
- Live PR 및 issue metadata는 `gh`로 검증된다.
