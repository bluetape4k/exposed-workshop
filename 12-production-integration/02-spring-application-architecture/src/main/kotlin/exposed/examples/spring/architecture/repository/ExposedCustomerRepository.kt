package exposed.examples.spring.architecture.repository

import exposed.examples.spring.architecture.model.CreateCustomerCommand
import exposed.examples.spring.architecture.model.CustomerRecord
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Spring Boot 4 아키텍처 예제에서 사용하는 Exposed JDBC 저장소이다.
 */
internal class ExposedCustomerRepository(
    private val database: Database,
) : CustomerRepository {

    private val schemaReady = AtomicBoolean(false)
    private val schemaLock = ReentrantLock()

    override fun create(command: CreateCustomerCommand): CustomerRecord {
        ensureSchema()
        return transaction(database) {
            val id = Customers.insertAndGetId {
                it[name] = command.name
                it[email] = command.email
            }.value
            CustomerRecord(id = id, name = command.name, email = command.email)
        }
    }

    override fun findById(id: Long): CustomerRecord? {
        ensureSchema()
        return transaction(database) {
            Customers
                .selectAll()
                .where { Customers.id eq id }
                .singleOrNull()
                ?.toRecord()
        }
    }

    override fun findAll(): List<CustomerRecord> {
        ensureSchema()
        return transaction(database) {
            Customers
                .selectAll()
                .orderBy(Customers.id)
                .map { it.toRecord() }
        }
    }

    override fun count(): Long {
        ensureSchema()
        return transaction(database) {
            Customers.selectAll().count()
        }
    }

    override fun deleteAll() {
        ensureSchema()
        transaction(database) {
            Customers.deleteAll()
        }
    }

    private fun ensureSchema() {
        if (schemaReady.get()) {
            return
        }
        schemaLock.withLock {
            if (!schemaReady.get()) {
                transaction(database) {
                    SchemaUtils.create(Customers)
                }
                schemaReady.set(true)
            }
        }
    }
}

private object Customers : LongIdTable("spring_customers") {
    val name = varchar("name", 80)
    val email = varchar("email", 120)
}

private fun ResultRow.toRecord(): CustomerRecord =
    CustomerRecord(
        id = this[Customers.id].value,
        name = this[Customers.name],
        email = this[Customers.email]
    )

