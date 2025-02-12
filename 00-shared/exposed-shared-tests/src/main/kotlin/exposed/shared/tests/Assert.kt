package exposed.shared.tests

import org.jetbrains.exposed.sql.Transaction
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("UnusedReceiverParameter")
private val Transaction.failedOn: String
    get() = currentTestDB?.name ?: currentDialectTest.name

fun Transaction.assertTrue(actual: Boolean) = assertTrue(actual, "Failed on $failedOn")
fun Transaction.assertFalse(actual: Boolean) = assertFalse(!actual, "Failed on $failedOn")
fun <T> Transaction.assertEquals(exp: T, act: T) = assertEquals(exp, act, "Failed on $failedOn")
fun <T> Transaction.assertEquals(exp: T, act: Collection<T>) = assertEquals(exp, act.single(), "Failed on $failedOn")

fun Transaction.assertFailAndRollback(message: String, block: () -> Unit) {
    commit()
    assertFails("Failed on ${currentDialectTest.name}. $message") {
        block()
        commit()
    }
    rollback()
}

inline fun <reified T: Throwable> expectException(body: () -> Unit) {
    assertFailsWith<T>("Failed on ${currentDialectTest.name}") {
        body()
    }
}
