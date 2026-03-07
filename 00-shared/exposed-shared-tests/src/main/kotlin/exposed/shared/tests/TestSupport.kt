package exposed.shared.tests

import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.util.*

/**
 * 현재 DB 방언에 맞는 식별자 대소문자 형식으로 변환합니다.
 *
 * @return 방언에 맞게 변환된 문자열. 현재 트랜잭션이 없으면 원본 문자열을 반환합니다.
 */
fun String.inProperCase(): String =
    TransactionManager.currentOrNull()?.db?.identifierManager?.inProperCase(this) ?: this

/** 현재 트랜잭션의 데이터베이스 방언을 반환합니다. */
val currentDialectTest: DatabaseDialect
    get() = TransactionManager.current().db.dialect

/** 현재 트랜잭션이 있는 경우 데이터베이스 방언을 반환하고, 없으면 `null`을 반환합니다. */
val currentDialectIfAvailableTest: DatabaseDialect?
    get() = TransactionManager.currentOrNull()?.db?.dialect
//        if (TransactionManager.currentOrNull() != null) {
//            currentDialectTest
//        } else {
//            null
//        }

/**
 * 주어진 열거형 원소들로 [EnumSet]을 생성합니다.
 *
 * @param E 열거형 타입
 * @param elements EnumSet에 포함할 열거형 원소들
 * @return 주어진 원소들을 포함하는 [EnumSet]
 */
inline fun <reified E: Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> =
    elements.toCollection(EnumSet.noneOf(E::class.java))

/**
 * SQL Server 방언에서 컬럼의 기본값 제약 조건 이름 부분을 반환합니다.
 *
 * @return SQL Server인 경우 제약 조건 이름 문자열, 그 외 방언에서는 빈 문자열
 */
fun <T> Column<T>.constraintNamePart() = (currentDialectTest as? SQLServerDialect)?.let {
    " CONSTRAINT DF_${table.tableName}_$name"
} ?: ""

/**
 * 기본 값으로 정보를 레코드를 생성하고, [duration]만큼 대기합니다.
 */
inline fun Table.insertAndWait(
    duration: Long,
    crossinline body: Table.(InsertStatement<Number>) -> Unit = {},
) {
    this.insert { body(it) }
    TransactionManager.current().commit()
    Thread.sleep(duration)
}

/**
 * 기본 값으로 정보를 레코드를 생성하고, [duration]만큼 대기합니다.
 */
suspend inline fun Table.insertAndSuspending(
    duration: Long,
    crossinline body: Table.(InsertStatement<Number>) -> Unit = {},
) {
    this.insert { body(it) }
    TransactionManager.current().commit()
    delay(duration)
}
