# Exposed Spring Repository

Spring Data Repository 패턴을 사용하여 Exposed를 사용한 Repository 구현하기

`ExposedRepository` 는 `EntityClass` 의 기능을 포함하여, Spring Data Repository 패턴을 구현합니다. 이 Repository는
`Exposed`를 사용하여 데이터베이스와 상호작용합니다.

## ExposedRepository

`ExposedRepository` 는 Spring Data 의 `CrudRepository` 와 유사한 기능을 Exposed 를 통해 제공하는 인터페이스입니다.


<details>
    <summary>ExposedRepository 소스</summary>

```kotlin
package exposed.shared.repository

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.AbstractQuery
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * Exposed 를 사용하는 Repository 의 기본 인터페이스입니다.
 */
interface ExposedRepository<T: Entity<ID>, ID: Any> {

    val table: IdTable<ID>

    val currentTransaction: org.jetbrains.exposed.sql.Transaction
        get() = TransactionManager.current()

    fun ResultRow.toEntity(): T

    fun count(): Long
    fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long
    fun count(op: Op<Boolean>): Long

    fun isEmpty(): Boolean
    fun exists(query: AbstractQuery<*>): Boolean

    fun findById(id: ID): T
    fun findByIdOrNull(id: ID): T?
    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        predicate: SqlExpressionBuilder.() -> Op<Boolean> = { Op.TRUE },
    ): List<T>

    fun delete(entity: T): Int
    fun deleteById(id: ID): Int
    fun deleteAll(
        limit: Int? = null,
        op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE },
    ): Int

    fun deleteIgnore(entity: T): Int
    fun deleteByIdIgnore(id: ID): Int
    fun deleteAllIgnore(
        limit: Int? = null,
        op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean> = { Op.TRUE },
    ): Int
}
```

</details>

## AbstractExposedRepository

`AbstractExposedRepository` 는 `ExposedRepository` 의 기본 구현체입니다. Spring Data 의 `CrudRepository` 의 기능 대부분을 기본적으로 제공합니다.

<details>
    <summary>AbstractExposedRepository 소스</summary>

```kotlin
package exposed.shared.repository

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.AbstractQuery
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteIgnoreWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

/**
 * Exposed 를 사용하는 Repository 의 최상위 구현체
 */
abstract class AbstractExposedRepository<T: Entity<ID>, ID: Any>(
    override val table: IdTable<ID>,
): ExposedRepository<T, ID> {

    companion object: KLogging()

    override fun count(): Long =
        table.selectAll().count()

    override fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long =
        table.selectAll().where(predicate).count()

    override fun count(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    override fun isEmpty(): Boolean =
        table.selectAll().empty()

    override fun exists(query: AbstractQuery<*>): Boolean {
        val exists = org.jetbrains.exposed.sql.exists(query)
        return table.select(exists).first()[exists]
    }

    override fun findById(id: ID): T =
        table.selectAll()
            .where { table.id eq id }
            .single()
            .toEntity()

    override fun findByIdOrNull(id: ID): T? =
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

    override fun findAll(limit: Int?, offset: Long?, predicate: SqlExpressionBuilder.() -> Op<Boolean>): List<T> {
        return table.selectAll()
            .where(predicate)
            .apply {
                limit?.let { limit(it) }
                offset?.let { offset(it) }
            }
            .map { it.toEntity() }
    }

    override fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    override fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    override fun deleteAll(limit: Int?, op: IdTable<ID>.(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteWhere(limit = limit, op = op)

    override fun deleteIgnore(entity: T): Int =
        table.deleteIgnoreWhere { table.id eq id }

    override fun deleteByIdIgnore(id: ID): Int =
        table.deleteIgnoreWhere { table.id eq id }

    override fun deleteAllIgnore(limit: Int?, op: IdTable<ID>.(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteIgnoreWhere(limit = limit, op = op)
}
```

</details>

## Custom Repository

### ActorRepository

`ActorExposedRepository.kt` 는 `Actor` 엔티티에 대한 CRUD 작업을 수행하는 `ExposedRepository` 의 구현체입니다.
`Exposed`를 사용하여 데이터베이스와 상호작용합니다.

[ActorExposedRepository](src/main/kotlin/exposed/examples/springmvc/domain/repository/ActorExposedRepository.kt)
