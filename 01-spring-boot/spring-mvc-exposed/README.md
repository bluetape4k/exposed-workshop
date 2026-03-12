# 01 Spring Boot: Spring MVC with Exposed

Spring MVC + Virtual Threads 환경에서 Exposed DAO/DSL을 실습하는 모듈입니다. 영화/배우 도메인을 중심으로 REST API, Repository, Swagger, Virtual Thread 설정을 순차 학습합니다.

## 학습 목표

- MVC 컨트롤러 + Exposed Repository 구조를 익힌다.
- Virtual Threads + Tomcat executor 전환 방식과 트랜잭션 조합을 이해한다.
- Swagger/OpenAPI로 API를 문서화하고 테스트한다.

## 선수 지식

- [`00-shared/exposed-shared-tests`](../../00-shared/exposed-shared-tests/README.md): 공통 테스트 베이스 클래스와 DB 설정 참고
- REST 컨트롤러/DTO 기반 Spring API 경험

## 핵심 개념

- `MovieSchema`/`ActorSchema`를 중심으로 LongIdTable+Entity 모델
- Repository에서 Exposed DSL(`insertAndGetId`, `select`, `andWhere`) 재사용
- Swagger `OpenAPI` + Tomcat Virtual Thread executor 설정

## 실행 방법

```bash
# 애플리케이션 기동
./gradlew :spring-mvc-exposed:bootRun

# 테스트 실행
./gradlew :spring-mvc-exposed:test
```

## 실습 체크리스트

- `GET /actors`, `GET /movies` 결과를 Postman에서 확인
- Virtual Threads 설정을 껐다 켰다 하며 처리량 차이 분석

## 성능·안정성 체크포인트

- Swagger UI가 `/swagger-ui.html`에서 정상 노출되는지 확인
- Tomcat executor에 Virtual Thread가 적용되어 최대 동시 처리량을 확보하는지 검증

## 핵심 시나리오 설명

### Actor REST API 통합 테스트

`WebTestClient`를 사용해 실제 HTTP 요청을 보내며 컨트롤러 → Repository → DB 전체 흐름을 검증한다.
생성 후 삭제(POST → DELETE), 잘못된 파라미터 방어 처리 등의 시나리오가 포함된다.

관련 파일:
- 컨트롤러 테스트: [`src/test/kotlin/exposed/workshop/springmvc/controller/ActorControllerTest.kt`](src/test/kotlin/exposed/workshop/springmvc/controller/ActorControllerTest.kt)
- 영화 컨트롤러 테스트: [`src/test/kotlin/exposed/workshop/springmvc/controller/MovieControllerTest.kt`](src/test/kotlin/exposed/workshop/springmvc/controller/MovieControllerTest.kt)

### Repository + Spring 트랜잭션 통합

`@Transactional(readOnly = true)` / `@Transactional`을 분리 적용해 읽기 전용 조회와
쓰기 연산의 트랜잭션 경계를 명확히 구분한다. 잘못된 birthday 파라미터가 전달되어도
예외 없이 전체 목록을 반환하는 방어 로직도 검증한다.

관련 파일:
- Repository 테스트: [`src/test/kotlin/exposed/workshop/springmvc/domain/repository/ActorRepositoryTest.kt`](src/test/kotlin/exposed/workshop/springmvc/domain/repository/ActorRepositoryTest.kt)

### Virtual Threads + Tomcat Executor

`application.yml`에서 Tomcat executor를 Virtual Thread 기반으로 전환하면
스레드 블로킹 없이 높은 동시성을 확보할 수 있다. 설정을 켜고 끄며 처리량 차이를 직접 측정해본다.

## 다음 모듈

- [spring-webflux-exposed](../spring-webflux-exposed/README.md)
