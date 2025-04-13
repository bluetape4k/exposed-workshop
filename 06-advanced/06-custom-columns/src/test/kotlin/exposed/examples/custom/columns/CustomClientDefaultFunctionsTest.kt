package exposed.examples.custom.columns

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.sql.ksuidMillisGenerated
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class CustomClientDefaultFunctionsTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS clientgenerated (
     *      id SERIAL PRIMARY KEY,
     *      timebased_uuid uuid NOT NULL,
     *      timebased_uuid_string VARCHAR(36) NOT NULL,
     *      snowflake BIGINT NOT NULL,
     *      ksuid VARCHAR(27) NOT NULL,
     *      ksuid_millis VARCHAR(27) NOT NULL
     * );
     * ```
     */
    object ClientGenerated: IntIdTable() {
        val timebasedUuid: Column<UUID> = uuid("timebased_uuid").timebasedUUIDGenerated()
        val timebasedUuidString: Column<String> = varchar("timebased_uuid_string", 36).timebasedUUIDGenerated()
        val snowflake: Column<Long> = long("snowflake").snowflakeGenerated()
        val ksuid: Column<String> = varchar("ksuid", 27).ksuidGenerated()
        val ksuidMillis: Column<String> = varchar("ksuid_millis", 27).ksuidMillisGenerated()
    }

    class ClientGeneratedEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<ClientGeneratedEntity>(ClientGenerated)

        var timebasedUuid by ClientGenerated.timebasedUuid
        var timebasedUuidString by ClientGenerated.timebasedUuidString
        var snowflake by ClientGenerated.snowflake
        var ksuid by ClientGenerated.ksuid
        var ksuidMillis by ClientGenerated.ksuidMillis

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = toStringBuilder()
            .add("timebasedUuid", timebasedUuid)
            .add("timebasedUuidString", timebasedUuidString)
            .add("snowflake", snowflake)
            .add("ksuid", ksuid)
            .add("ksuidMillis", ksuidMillis)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL - 클라이언트에서 기본값으로 생성하는 함수`(testDB: TestDB) {
        withTables(testDB, ClientGenerated) {
            val entityCount = 100
            val values = List(entityCount) { it + 1 }
            ClientGenerated.batchInsert(values) {}
            flushCache()
            entityCache.clear()

            val rows = ClientGenerated.selectAll().toList()

            rows.map { it[ClientGenerated.timebasedUuid] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.timebasedUuidString] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.snowflake] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.ksuid] }.distinct() shouldHaveSize entityCount
            rows.map { it[ClientGenerated.ksuidMillis] }.distinct() shouldHaveSize entityCount
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO - 클라이언트에서 기본값으로 생성하는 함수`(testDB: TestDB) {
        withTables(testDB, ClientGenerated) {
            val entityCount = 100
            val entities = List(entityCount) {
                ClientGeneratedEntity.new {}
            }
            flushCache()
            entityCache.clear()

            val loaded = ClientGeneratedEntity.all().toList()

            loaded.map { it.timebasedUuid }.distinct() shouldHaveSize entityCount
            loaded.map { it.timebasedUuidString }.distinct() shouldHaveSize entityCount
            loaded.map { it.snowflake }.distinct() shouldHaveSize entityCount
            loaded.map { it.ksuid }.distinct() shouldHaveSize entityCount
            loaded.map { it.ksuidMillis }.distinct() shouldHaveSize entityCount
        }
    }
}
