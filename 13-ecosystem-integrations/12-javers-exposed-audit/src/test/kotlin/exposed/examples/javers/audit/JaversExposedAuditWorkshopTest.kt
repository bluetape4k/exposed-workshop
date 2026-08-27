package exposed.examples.javers.audit

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotContain
import org.javers.core.Javers
import org.javers.core.metamodel.`object`.SnapshotType
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class JaversExposedAuditWorkshopTest {

    private lateinit var database: Database
    private lateinit var javers: Javers

    @BeforeEach
    fun beforeEach() {
        database = Database.connect(
            url = "jdbc:h2:mem:javers-audit-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        javers = createJavers(database)
        transaction(database) {
            SchemaUtils.create(Customers)
        }
    }

    @AfterEach
    fun afterEach() {
        AuditContextHolder.clear()
    }

    @Test
    fun `JaVers와 Exposed schema는 반복 초기화할 수 있다`() {
        createJavers(database)

        transaction(database) {
            Customers.selectAll().count().shouldBeZero()
            CommitTable.selectAll().count().shouldBeZero()
            CdoSnapshotTable.selectAll().count().shouldBeZero()
        }
    }

    @Test
    fun `create update audit exposes metadata diff and history`() {
        val audit = JaversAuditHistory(javers)
        val subscription = subscribeAudit(database, javers)
        val id = try {
            AuditContextHolder.with(AuditContext(actor = "alice", requestId = "request-create")) {
                transaction(database) {
                    CustomerEntity.new {
                        name = "Alice"
                        email = "alice@example.com"
                        secret = "initial-secret"
                    }.id.value
                }
            }.also { customerId ->
                AuditContextHolder.with(AuditContext(actor = "bob", requestId = "request-update")) {
                    transaction(database) {
                        requireNotNull(CustomerEntity.findById(customerId)).apply {
                            name = "Alice Updated"
                            email = "alice.updated@example.com"
                        }
                    }
                }
            }
        } finally {
            subscription.close()
        }

        val snapshots = audit.snapshots(id)
        snapshots shouldHaveSize 2
        snapshots[0].type shouldBeEqualTo SnapshotType.UPDATE
        snapshots[0].getPropertyValue("name") shouldBeEqualTo "Alice Updated"
        snapshots[0].commitMetadata.author shouldBeEqualTo "bob"
        snapshots[0].commitMetadata.properties["requestId"] shouldBeEqualTo "request-update"
        snapshots[0].commitMetadata.properties["changeType"] shouldBeEqualTo "Updated"
        audit.changes(id).shouldNotBeEmpty()
        audit.history(id).apply {
            current shouldBeEqualTo AuditedCustomer(
                id = id,
                name = "Alice Updated",
                email = "alice.updated@example.com",
            )
            snapshots shouldHaveSize 2
        }
    }

    @Test
    fun `one transaction records only the final flushed customer state`() {
        val subscription = subscribeAudit(database, javers)
        val id = try {
            val customerId = AuditContextHolder.with(AuditContext("alice", "request-create")) {
                transaction(database) {
                    CustomerEntity.new {
                        name = "Alice"
                        email = "alice@example.com"
                        secret = "secret"
                    }.id.value
                }
            }

            AuditContextHolder.with(AuditContext("alice", "request-bulk-update")) {
                transaction(database) {
                    requireNotNull(CustomerEntity.findById(customerId)).apply {
                        name = "Alice 1"
                        name = "Alice 2"
                        email = "alice.2@example.com"
                    }
                }
            }
            customerId
        } finally {
            subscription.close()
        }

        val snapshots = JaversAuditHistory(javers).snapshots(id)
        snapshots shouldHaveSize 2
        snapshots.first().getPropertyValue("name") shouldBeEqualTo "Alice 2"
        snapshots.first().getPropertyValue("email") shouldBeEqualTo "alice.2@example.com"
    }

    @Test
    fun `rollback removes business and audit rows`() {
        val subscription = subscribeAudit(database, javers)
        try {
            assertFailsWith<IllegalStateException> {
                AuditContextHolder.with(AuditContext("alice", "request-rollback")) {
                    transaction(database) {
                        CustomerEntity.new {
                            name = "Rolled back"
                            email = "rollback@example.com"
                            secret = "secret"
                        }
                        error("rollback audit transaction")
                    }
                }
            }
        } finally {
            subscription.close()
        }

        transaction(database) {
            Customers.selectAll().count().shouldBeZero()
            CommitTable.selectAll().count().shouldBeZero()
            CdoSnapshotTable.selectAll().count().shouldBeZero()
        }
    }

    @Test
    fun `unchanged entity does not create a duplicate audit commit`() {
        val subscription = subscribeAudit(database, javers)
        val id = try {
            val customerId = AuditContextHolder.with(AuditContext("alice", "request-create")) {
                transaction(database) {
                    CustomerEntity.new {
                        name = "Alice"
                        email = "alice@example.com"
                        secret = "secret"
                    }.id.value
                }
            }

            AuditContextHolder.with(AuditContext("bob", "request-duplicate")) {
                transaction(database) {
                        requireNotNull(CustomerEntity.findById(customerId)).apply {
                        name = "Alice"
                        email = "alice@example.com"
                    }
                }
            }
            customerId
        } finally {
            subscription.close()
        }

        JaversAuditHistory(javers).snapshots(id) shouldHaveSize 1
        transaction(database) {
            CommitTable.selectAll().count() shouldBeEqualTo 1L
        }
    }

    @Test
    fun `secret is excluded from JaVers properties and encoded state`() {
        val subscription = subscribeAudit(database, javers)
        val id = try {
            val customerId = AuditContextHolder.with(AuditContext("alice", "request-create")) {
                transaction(database) {
                    CustomerEntity.new {
                        name = "Alice"
                        email = "alice@example.com"
                        secret = "do-not-store-in-audit"
                    }.id.value
                }
            }

            AuditContextHolder.with(AuditContext("alice", "request-secret")) {
                transaction(database) {
                    requireNotNull(CustomerEntity.findById(customerId)).secret = "changed-secret"
                }
            }
            customerId
        } finally {
            subscription.close()
        }

        transaction(database) {
            CdoSnapshotTable.selectAll().forEach { row ->
                row[CdoSnapshotTable.state] shouldNotContain "\"secret\""
                row[CdoSnapshotTable.state] shouldNotContain "do-not-store-in-audit"
                row[CdoSnapshotTable.changedProperties] shouldNotContain "secret"
            }
        }
        JaversAuditHistory(javers).snapshots(id).all { snapshot ->
            snapshot.changed.none { property -> property == "secret" }
        }.shouldBeEqualTo(true)
    }

    @Test
    fun `closed subscription is idempotent and stops auditing`() {
        val subscription = subscribeAudit(database, javers)
        subscription.close()
        subscription.close()

        AuditContextHolder.with(AuditContext("alice", "request-closed")) {
            transaction(database) {
                CustomerEntity.new {
                    name = "No audit"
                    email = "no-audit@example.com"
                    secret = "secret"
                }
            }
        }

        transaction(database) {
            CommitTable.selectAll().count().shouldBeZero()
            CdoSnapshotTable.selectAll().count().shouldBeZero()
        }
    }

    @Test
    fun `only one global subscription can own the audit hook`() {
        val subscription = subscribeAudit(database, javers)
        try {
            assertFailsWith<IllegalStateException> {
                subscribeAudit(database, javers)
            }
        } finally {
            subscription.close()
        }

        subscribeAudit(database, javers).close()
    }

    @Test
    fun `context is nested restored and removed after normal or exceptional exit`() {
        assertFailsWith<IllegalStateException> { AuditContextHolder.requireCurrent() }

        AuditContextHolder.with(AuditContext("outer", "request-outer")) {
            AuditContextHolder.requireCurrent().actor shouldBeEqualTo "outer"
            AuditContextHolder.with(AuditContext("inner", "request-inner")) {
                AuditContextHolder.requireCurrent().actor shouldBeEqualTo "inner"
            }
            AuditContextHolder.requireCurrent().requestId shouldBeEqualTo "request-outer"
        }
        assertFailsWith<IllegalStateException> { AuditContextHolder.requireCurrent() }

        assertFailsWith<IllegalStateException> {
            AuditContextHolder.with(AuditContext("exception", "request-exception")) {
                error("context scope failure")
            }
        }
        assertFailsWith<IllegalStateException> { AuditContextHolder.requireCurrent() }
    }

    @Test
    fun `missing context fails closed and rolls back all rows`() {
        val subscription = subscribeAudit(database, javers)
        try {
            assertFailsWith<IllegalStateException> {
                transaction(database) {
                    CustomerEntity.new {
                        name = "Anonymous"
                        email = "anonymous@example.com"
                        secret = "secret"
                    }
                }
            }
        } finally {
            subscription.close()
        }

        transaction(database) {
            Customers.selectAll().count().shouldBeZero()
            CommitTable.selectAll().count().shouldBeZero()
            CdoSnapshotTable.selectAll().count().shouldBeZero()
        }
    }

    @Test
    fun `subscription rejects entity changes from another database`() {
        val otherDatabase = Database.connect(
            url = "jdbc:h2:mem:javers-audit-other-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        createJavers(otherDatabase)
        transaction(otherDatabase) {
            SchemaUtils.create(Customers)
        }

        val subscription = subscribeAudit(database, javers)
        try {
            assertFailsWith<IllegalStateException> {
                AuditContextHolder.with(AuditContext("alice", "request-cross-database")) {
                    transaction(otherDatabase) {
                        CustomerEntity.new {
                            name = "Cross database"
                            email = "cross-database@example.com"
                            secret = "secret"
                        }
                    }
                }
            }
        } finally {
            subscription.close()
        }

        transaction(otherDatabase) {
            Customers.selectAll().count().shouldBeZero()
            CommitTable.selectAll().count().shouldBeZero()
            CdoSnapshotTable.selectAll().count().shouldBeZero()
        }
    }

    @Test
    fun `subscription rejects a Javers instance bound to another database`() {
        val otherDatabase = Database.connect(
            url = "jdbc:h2:mem:javers-audit-javers-other-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        val otherJavers = createJavers(otherDatabase)

        assertFailsWith<IllegalStateException> {
            subscribeAudit(database, otherJavers)
        }

        subscribeAudit(database, javers).close()
    }
}
