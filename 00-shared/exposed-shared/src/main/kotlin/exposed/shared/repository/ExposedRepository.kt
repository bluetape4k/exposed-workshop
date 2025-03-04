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

interface ExposedRepository<T: Entity<ID>, ID: Any> {

    fun count(): Long
    fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long
    fun count(op: Op<Boolean>): Long

    fun isEmpty(): Boolean

    fun findById(id: ID): T
    fun findByIdOrNull(id: ID): T?
    fun findAll(): List<T>

    fun delete(entity: T): Int
    fun delete(limit: Int? = null, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int
    fun deleteIgnore(entity: T): Int
    fun deleteById(id: ID): Int
    fun deleteIgnoreById(id: ID): Int
}

abstract class AbstractExposedRepository<T: Entity<ID>, ID: Any>(
    protected val table: IdTable<ID>,
): ExposedRepository<T, ID> {

    companion object: KLogging()

    abstract fun ResultRow.toEntity(): T

    override fun count(): Long =
        table.selectAll().count()

    override fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long =
        table.selectAll().where(predicate).count()

    override fun count(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    override fun isEmpty(): Boolean =
        table.selectAll().empty()

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

    override fun findAll(): List<T> =
        table.selectAll().map { it.toEntity() }

    override fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    override fun delete(limit: Int?, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int =
        table.deleteWhere(limit = limit, op)

    override fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    override fun deleteIgnore(entity: T): Int =
        table.deleteIgnoreWhere { table.id eq id }

    override fun deleteIgnoreById(id: ID): Int =
        table.deleteIgnoreWhere { table.id eq id }
}
