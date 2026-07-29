## 배경

Main source에서 table을 정의하는 Exposed workshop module 전반에 JetBrains Exposed Gradle
plugin을 도입했다.

## 결정

Workshop은 managed `bt4k` catalog가 아니라 기존 Exposed version alias에 묶인 repo-local
plugin alias를 사용한다.

## 결과

Spring, multi-tenant, performance, Ktor, production-integration 예제는 이제 명시적인
migration setting과 함께 `generateMigrations`를 노출한다.

## 검증

`git diff --check`, `./gradlew -q help`, `:spring-mvc-exposed:tasks --all`을 실행했다.

## 향후 보호 장치

해당 fixture에 구체적인 migration output이 필요하지 않다면 shared test fixture는 migration
plugin rollout에서 제외한다.
