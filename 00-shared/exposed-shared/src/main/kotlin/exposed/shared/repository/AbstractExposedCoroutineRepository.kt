package exposed.shared.repository

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteIgnoreWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

/**
 * Exposed 를 사용하는 Coroutines용 Repository 의 최상위 추상화 클래스
 */
abstract class AbstractExposedCoroutineRepository<T: Entity<ID>, ID: Any>(
    val table: IdTable<ID>,
): ExposedCoroutineRepository<T, ID> {

    companion object: KLogging()

    abstract fun ResultRow.toEntity(): T

    override suspend fun count(): Long =
        table.selectAll().count()

    override suspend fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long =
        table.selectAll().where(predicate).count()

    override suspend fun count(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    override suspend fun isEmpty(): Boolean =
        table.selectAll().empty()

    override suspend fun findById(id: ID): T =
        table.selectAll()
            .where { table.id eq id }
            .single()
            .toEntity()

    override suspend fun findByIdOrNull(id: ID): T? =
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

    override suspend fun findAll(): List<T> =
        table.selectAll().map { it.toEntity() }

    override suspend fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    override suspend fun delete(limit: Int?, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteWhere(limit = limit, op)

    override suspend fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    override suspend fun deleteIgnore(entity: T): Int =
        table.deleteIgnoreWhere { table.id eq id }

    override suspend fun deleteIgnoreById(id: ID): Int =
        table.deleteIgnoreWhere { table.id eq id }
}
