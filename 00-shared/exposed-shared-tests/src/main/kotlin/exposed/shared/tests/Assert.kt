package exposed.shared.tests

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("UnusedReceiverParameter")
private val Transaction.failedOn: String
    get() = currentTestDB?.name ?: currentDialectTest.name

/**
 * 현재 트랜잭션 컨텍스트 정보를 포함해 boolean 참 여부를 검증합니다.
 */
fun Transaction.assertTrue(actual: Boolean) = assertTrue(actual, "Failed on $failedOn")

/**
 * 현재 트랜잭션 컨텍스트 정보를 포함해 boolean 거짓 여부를 검증합니다.
 */
fun Transaction.assertFalse(actual: Boolean) = assertFalse(actual, "Failed on $failedOn")

/**
 * 현재 트랜잭션 컨텍스트 정보를 포함해 두 값을 비교합니다.
 */
fun <T> Transaction.assertEquals(exp: T, act: T) = assertEquals(exp, act, "Failed on $failedOn")

/**
 * 단일 원소 컬렉션의 값을 현재 트랜잭션 컨텍스트 정보와 함께 비교합니다.
 */
fun <T> Transaction.assertEquals(exp: T, act: Collection<T>) = assertEquals(exp, act.single(), "Failed on $failedOn")

/**
 * [block] 이 실패하면서 롤백되는지 검증합니다.
 */
fun JdbcTransaction.assertFailAndRollback(message: String, block: () -> Unit) {
    commit()
    assertFails("Failed on ${currentDialectTest.name}. $message") {
        block()
        commit()
    }
    rollback()
}

/**
 * 지정한 예외 타입이 발생하는지 검증합니다.
 */
inline fun <reified T: Throwable> expectException(body: () -> Unit) {
    assertFailsWith<T>("Failed on ${currentDialectTest.name}") {
        body()
    }
}
