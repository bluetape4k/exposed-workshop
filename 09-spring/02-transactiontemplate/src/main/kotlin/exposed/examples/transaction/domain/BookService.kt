package exposed.examples.transaction.domain

import exposed.examples.transaction.domain.BookSchema.Book
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.quoted
import net.datafaker.Faker
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionOperations

@Component
class BookService(
    @Qualifier("exposedTransactionTemplate") private val exposedTransactionTemplate: TransactionOperations,
    @Qualifier("withoutTransactionOperations") private val withoutTransactionOperations: TransactionOperations,
    private val jdbcTemplate: JdbcTemplate,
) {

    companion object: KLogging() {
        val faker = Faker()
    }

    fun execWithSpringAndExposedTransactions() {
        execWithExposedTransaction()
        execWithoutSpringTransaction()
    }

    fun executeSpringTransaction() {
        // exposedTransactionTemplate 는 Exposed 에서 제공하는 `SpringTransactionManager` 를 사용합니다.
        log.info { "Execute with spring transaction" }
        exposedTransactionTemplate.execute {
            createNewAuthor()
        }
    }

    fun execWithExposedTransaction() {
        log.info { "Execute with exposed transaction" }
        transaction {
            Book.new {
                title = faker.book().title()
                description = faker.lorem().sentence()
            }
        }
    }

    fun execWithoutSpringTransaction() {
        log.info { "Execute without spring transaction" }
        // withoutTransactionOperations 는 Transaction 적용이 안되어 있다.
        withoutTransactionOperations.execute {
            createNewAuthor()
        }
    }

    @Transactional
    fun execTransactionalAnnotation() {
        log.info { "Execute with Transactional annotation" }
        // withoutTransactionOperations 는 Transaction 적용이 안되어 있다.
        withoutTransactionOperations.execute {
            createNewAuthor()
        }
    }

    /**
     * 새로운 Author 를 생성합니다.
     *
     * ```sql
     * INSERT INTO AUTHORS("name", description) values ('John Doe', 'Lorem ipsum dolor sit amet.')
     * ```
     */
    private fun createNewAuthor() {
        // name 에 single quote 가 들어간 경우가 있습니다.
        val name = faker.name().fullName().quoted()
        val description = faker.lorem().sentence().quoted()

        val query = """INSERT INTO AUTHORS("name", description) values ($name, $description)"""
        jdbcTemplate.execute(query)
    }
}
