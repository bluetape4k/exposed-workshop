package exposed.examples.jdbc.domain

import exposed.examples.jdbc.domain.BookSchema.Book
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.quoted
import net.datafaker.Faker
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionOperations

@Component
class BookService(
    @Qualifier("operations1") private val operations1: TransactionOperations,
    @Qualifier("operations2") private val operations2: TransactionOperations,
    private val jdbcTemplate: JdbcTemplate,
) {

    companion object: KLogging() {
        val faker = Faker()
    }

    fun execWithSpringAndExposedTransactions() {
        execWithExposedTransaction()
        execWithSpringTransaction()
    }

    fun execWithSpringTransaction() {
        // operations1 는 Transaction 적용이 되어 있다.
        operations1.execute {
            log.info { "Execute with spring transaction" }
            createNewAuthor()
        }
    }

    fun execWithExposedTransaction() {
        transaction {
            Book.new {
                title = faker.book().title()
                description = faker.lorem().sentence()
            }
        }
    }

    fun execWithoutSpringTransaction() {
        transaction {
            // INSERT INTO BOOKS (TITLE, DESCRIPTION) VALUES ('Terrible Swift Sword', 'Autem nostrum nam cumque.')
            Book.new {
                title = faker.book().title()
                description = faker.lorem().sentence()
            }
        }
        // operations2 는 Transaction 적용이 안되어 있다.
        operations2.execute {
            log.info { "Execute without spring transaction" }
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
