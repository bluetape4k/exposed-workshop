package exposed.shared.repository

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder

/**
 * Exposed 를 사용하는 Coroutines용 Repository 의 기본 인터페이스
 */
interface ExposedCoroutineRepository<T: Entity<ID>, ID: Any> {

    suspend fun count(): Long
    suspend fun count(predicate: SqlExpressionBuilder.() -> Op<Boolean>): Long
    suspend fun count(op: Op<Boolean>): Long

    suspend fun isEmpty(): Boolean

    suspend fun findById(id: ID): T
    suspend fun findByIdOrNull(id: ID): T?
    suspend fun findAll(): List<T>

    suspend fun delete(entity: T): Int
    suspend fun delete(limit: Int? = null, op: (IdTable<ID>).(ISqlExpressionBuilder) -> Op<Boolean>): Int
    suspend fun deleteIgnore(entity: T): Int
    suspend fun deleteById(id: ID): Int
    suspend fun deleteIgnoreById(id: ID): Int
}
