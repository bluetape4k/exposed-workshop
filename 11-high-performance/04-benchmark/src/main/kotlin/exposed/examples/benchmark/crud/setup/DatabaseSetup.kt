package exposed.examples.benchmark.crud.setup

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.examples.benchmark.crud.model.DepartmentJpa
import exposed.examples.benchmark.crud.model.DepartmentTable
import exposed.examples.benchmark.crud.model.EmployeeJpa
import exposed.examples.benchmark.crud.model.EmployeeTable
import exposed.examples.benchmark.crud.model.PersonJpa
import exposed.examples.benchmark.crud.model.PersonTable
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import jakarta.persistence.EntityManagerFactory
import org.hibernate.cfg.AvailableSettings
import org.hibernate.jpa.HibernatePersistenceProvider
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.Executors
import javax.sql.DataSource

/**
 * 벤치마크 전체에서 공유하는 PostgreSQL Testcontainer 인스턴스.
 */
val postgres: PostgreSQLServer by lazy {
    PostgreSQLServer.Launcher.postgres
}

/**
 * PostgreSQL Testcontainer 기반 HikariCP DataSource를 생성합니다.
 */
fun createDataSource(dbName: String, useVirtualThreads: Boolean = false): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = postgres.jdbcUrl
        driverClassName = postgres.driverClassName
        username = postgres.username
        password = postgres.password
        maximumPoolSize = 100
        isAutoCommit = false

        if (useVirtualThreads) {
            scheduledExecutor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory())
        }
    }
    return HikariDataSource(config)
}

/**
 * Exposed Database 연결을 설정합니다.
 */
fun setupExposedDatabase(dataSource: DataSource): Database {
    return Database.connect(dataSource)
}

/**
 * Exposed 테이블을 생성합니다.
 */
fun createExposedTables(db: Database) {
    transaction(db) {
        SchemaUtils.create(PersonTable, DepartmentTable, EmployeeTable)
    }
}

/**
 * Exposed 테이블을 초기화(truncate)합니다.
 */
fun truncateExposedTables(db: Database) {
    transaction(db) {
        EmployeeTable.deleteAll()
        DepartmentTable.deleteAll()
        PersonTable.deleteAll()
    }
}

/**
 * JPA EntityManagerFactory를 프로그래밍 방식으로 생성합니다.
 */
fun createEntityManagerFactory(dataSource: DataSource): EntityManagerFactory {
    val properties = mapOf<String, Any>(
        AvailableSettings.DATASOURCE to dataSource,
        AvailableSettings.DIALECT to "org.hibernate.dialect.PostgreSQLDialect",
        AvailableSettings.HBM2DDL_AUTO to "create",
        AvailableSettings.SHOW_SQL to false,
        AvailableSettings.FORMAT_SQL to false,
        AvailableSettings.STATEMENT_BATCH_SIZE to 50,
        AvailableSettings.ORDER_INSERTS to true,
        AvailableSettings.ORDER_UPDATES to true,
    )

    return HibernatePersistenceProvider().createContainerEntityManagerFactory(
        PersistenceUnitInfoImpl(
            persistenceUnitName = "benchmark",
            managedClassNames = listOf(
                PersonJpa::class.java.name,
                DepartmentJpa::class.java.name,
                EmployeeJpa::class.java.name,
            ),
            dataSource = dataSource,
        ),
        properties,
    )
}
