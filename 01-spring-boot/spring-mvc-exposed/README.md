# 01 Spring Boot: Spring MVC with Exposed

Spring MVC + Virtual Threads 환경에서 Exposed DAO/DSL을 실습하는 모듈입니다. 영화/배우 도메인을 중심으로 REST API, Repository, Swagger, Virtual Thread 설정을 순차 학습합니다.

## 학습 목표

- MVC 컨트롤러 + Exposed Repository 구조를 익힌다.
- Virtual Threads + Tomcat executor 전환 방식과 트랜잭션 조합을 이해한다.
- Swagger/OpenAPI로 API를 문서화하고 테스트한다.

## 선수 지식

- `/Users/debop/work/bluetape4k/exposed-workshop/00-shared/exposed-shared-tests/README.md`
- REST 컨트롤러/DTO 기반 Spring API 경험

## 핵심 개념

- `MovieSchema`/`ActorSchema`를 중심으로 LongIdTable+Entity 모델
- Repository에서 Exposed DSL(`insertAndGetId`, `select`, `andWhere`) 재사용
- Swagger `OpenAPI` + Tomcat Virtual Thread executor 설정

## 실행 방법

```bash
./gradlew :exposed-01-spring-boot-spring-mvc-exposed:bootRun
```

## 실습 체크리스트

- `GET /actors`, `GET /movies` 결과를 Postman에서 확인
- Virtual Threads 설정을 껐다 켰다 하며 처리량 차이 분석

## 성능·안정성 체크포인트

- Swagger UI가 `/swagger-ui.html`에서 정상 노출되는지 확인
- Tomcat executor에 Virtual Thread가 적용되어 최대 동시 처리량을 확보하는지 검증

## 다음 모듈

- [spring-webflux-exposed](../spring-webflux-exposed/README.md)
