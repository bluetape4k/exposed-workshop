package exposed.examples.ktor.architecture.repository

import exposed.examples.ktor.architecture.model.CreateCustomerCommand
import exposed.examples.ktor.architecture.model.CustomerRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Exposed JDBC implementation that isolates blocking work on `Dispatchers.IO`.
 */
internal class ExposedCustomerRepository(
    private val database: Database,
) : CustomerRepository {

    private val schemaReady = AtomicBoolean(false)
    private val schemaMutex = Mutex()

    override suspend fun create(command: CreateCustomerCommand): CustomerRecord {
        ensureSchema()
        return transactionIO {
            val id = Customers.insertAndGetId {
                it[name] = command.name
                it[email] = command.email
            }.value
            CustomerRecord(id = id, name = command.name, email = command.email)
        }
    }

    override suspend fun findById(id: Long): CustomerRecord? {
        ensureSchema()
        return transactionIO {
            Customers
                .selectAll()
                .where { Customers.id eq id }
                .singleOrNull()
                ?.toRecord()
        }
    }

    override suspend fun findAll(): List<CustomerRecord> {
        ensureSchema()
        return transactionIO {
            Customers
                .selectAll()
                .orderBy(Customers.id)
                .map { it.toRecord() }
        }
    }

    override suspend fun count(): Long {
        ensureSchema()
        return transactionIO {
            Customers.selectAll().count()
        }
    }

    private suspend fun ensureSchema() {
        if (schemaReady.get()) {
            return
        }
        schemaMutex.withLock {
            if (!schemaReady.get()) {
                transactionIO {
                    SchemaUtils.create(Customers)
                }
                schemaReady.set(true)
            }
        }
    }

    private suspend fun <T> transactionIO(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction(database) {
                block()
            }
        }
}

private object Customers : LongIdTable("customers") {
    val name = varchar("name", 80)
    val email = varchar("email", 120)
}

private fun ResultRow.toRecord(): CustomerRecord =
    CustomerRecord(
        id = this[Customers.id].value,
        name = this[Customers.name],
        email = this[Customers.email]
    )
