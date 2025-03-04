package exposed.shared.repository

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder

/**
 * Exposed 를 사용하는 Repository 의 기본 인터페이스입니다.
 */
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
