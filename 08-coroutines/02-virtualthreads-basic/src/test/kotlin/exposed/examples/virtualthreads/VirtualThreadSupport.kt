package exposed.examples.virtualthreads

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.util.concurrent.ExecutorService

/**
 * Virtual Thread 에서 Exposed 를 사용할 수 있도록 지원하는 패키지 함수입니다.
 */
fun <T> newVirtualThreadTransaction(
    executor: ExecutorService? = null,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: Transaction.() -> T,
): T =
    virtualThreadTransactionAsync(executor, db, transactionIsolation, readOnly, statement).await()


/**
 * Virtual Thread 에서 Exposed 를 사용할 수 있도록 지원하는 패키지 함수입니다.
 */
fun <T> virtualThreadTransactionAsync(
    executor: ExecutorService? = null,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: Transaction.() -> T,
): VirtualFuture<T> = virtualFuture(executor = executor ?: VirtualThreadExecutor) {
    val isolationLevel = transactionIsolation ?: db.transactionManager.defaultIsolationLevel
    transaction(db = db, transactionIsolation = isolationLevel, readOnly = readOnly) {
        statement(this)
    }
}
