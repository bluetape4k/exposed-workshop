package exposed.examples.jdbc.domain

import exposed.examples.jdbc.AbstractTransactionApplicationTest
import exposed.examples.jdbc.domain.BookSchema.AuthorTable
import exposed.examples.jdbc.domain.BookSchema.BookTable
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TransactionTemplateTest(
    @Autowired private val bookService: BookService,
): AbstractTransactionApplicationTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @BeforeAll
    fun beforeAll() {
        transaction {
            SchemaUtils.create(AuthorTable, BookTable)
        }
    }

    @Order(1)
    @RepeatedTest(REPEAT_SIZE)
    fun `without spring transaction`() {
        bookService.execWithoutSpringTransaction()
    }

    @Order(2)
    @RepeatedTest(REPEAT_SIZE)
    fun `with spring transaction`() {
        bookService.execWithSpringTransaction()
    }

    @Order(3)
    @RepeatedTest(REPEAT_SIZE)
    fun `with exposed transaction`() {
        bookService.execWithExposedTransaction()
    }

    @Order(4)
    @RepeatedTest(REPEAT_SIZE)
    fun `with spring and exposed transaction`() {
        bookService.execWithSpringAndExposedTransactions()
    }
}
