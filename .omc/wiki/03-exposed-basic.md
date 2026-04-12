---
title: "03 Exposed Basic"
tags: [exposed, dsl, dao, basic, crud]
category: pattern
created: 2026-04-13
updated: 2026-04-13
---

# 03 Exposed Basic

DSL과 DAO 두 패턴을 City/User 도메인으로 비교 학습.

## DSL vs DAO

| 항목 | DSL | DAO |
|------|-----|-----|
| 스키마 | `object CityTable : Table("cities")` | `object CityTable : IntIdTable("cities")` |
| 삽입 | `CityTable.insert { it[name] = "Seoul" }` | `City.new { name = "Seoul" }` |
| 조회 | `CityTable.selectAll().where { id eq 1 }` | `City.findById(1)` |
| 수정 | `CityTable.update({ id eq 1 }) { ... }` | `city.name = "..."` (자동 반영) |
| 삭제 | `CityTable.deleteWhere { id eq 1 }` | `city.delete()` |
| 관계 | `innerJoin` | `city.users` (Lazy) / `.with()` (Eager) |

## 모듈

- `exposed-sql-example` — DSL 중심 SELECT/INSERT/UPDATE/DELETE
- `exposed-dao-example` — Entity 모델링, 관계 매핑, Eager Loading, 코루틴

## 핵심 포인트

- Eager loading `.with(City::users)` 으로 N+1 방지
- 코루틴: `newSuspendedTransaction { }` 내 Entity 접근

## 실행

```bash
./gradlew :exposed-sql-example:test
./gradlew :exposed-dao-example:test
```
