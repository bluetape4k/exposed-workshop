package exposed.sql.example

import exposed.shared.tests.TestDB
import exposed.shared.tests.withSuspendedTables
import exposed.shared.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.substring
import org.jetbrains.exposed.v1.core.trim
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

object Schema {

    /**
     * 도시 정보를 저장하는 테이블
     *
     * ```sql
     * -- Postgres
     *
     * CREATE TABLE IF NOT EXISTS cities (
     *      id SERIAL,
     *      "name" VARCHAR(50) NOT NULL,
     *
     *      CONSTRAINT PK_Cities_ID PRIMARY KEY (id)
     * )
     * ```
     */
    object CityTable: Table("cities") {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)

        override val primaryKey = PrimaryKey(id, name = "PK_Cities_ID")
    }

    /**
     * 사용자 정보를 저장하는 테이블
     * ```sql
     * CREATE TABLE IF NOT EXISTS users (
     *      id VARCHAR(10),
     *      "name" VARCHAR(50) NOT NULL,
     *      city_id INT NULL,
     *
     *      CONSTRAINT PK_User_ID PRIMARY KEY (id),
     *
     *      CONSTRAINT fk_users_city_id__id FOREIGN KEY (city_id)
     *          REFERENCES cities(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object UserTable: Table("users") {
        val id = varchar("id", length = 10)
        val name = varchar("name", length = 50)
        val cityId = optReference("city_id", CityTable.id)

        override val primaryKey = PrimaryKey(id, name = "PK_User_ID")
    }

    /**
     * [CityTable], [UserTable]을 이용하여 [statement]를 수행합니다.
     */
    fun withCityUsers(testDB: TestDB, statement: JdbcTransaction.() -> Unit) {
        withTables(testDB, CityTable, UserTable) {
            insertSampleData()
            commit()

            statement()
        }
    }

    /**
     * [CityTable], [UserTable]을 이용하여 Coroutines 환경에서 [statement]를 수행합니다.
     */
    suspend fun withSuspendedCityUsers(testDB: TestDB, statement: suspend JdbcTransaction.() -> Unit) {
        withSuspendedTables(testDB, CityTable, UserTable) {
            insertSampleData()
            commit()

            statement()
        }
    }

    fun insertSampleData() {
        // `Seoul` 도시 정보를 저장하고, ID를 반환합니다.
        val seoulId = CityTable.insert {
            it[name] = "Seoul"
        } get CityTable.id

        // `Busan` 도시 정보를 저장하고, ID를 반환합니다.
        val busanId = CityTable.insert {
            it[name] = "Busan"
        } get CityTable.id

        // INSERT INTO city ("name") VALUES (SUBSTRING(TRIM('   Daegu   '), 1, 2))
        val daeguId = CityTable.insert {
            it.update(name, stringLiteral("   Daegu   ").trim().substring(1, 2))
        }[CityTable.id]

        val daeguName = CityTable
            .selectAll()
            .where { CityTable.id eq daeguId }
            .single()[CityTable.name]
        daeguName shouldBeEqualTo "Da"

        UserTable.insert {
            it[id] = "debop"
            it[name] = "Debop.Bae"
            it[cityId] = seoulId
        }
        UserTable.insert {
            it[id] = "jane"
            it[name] = "Jane.Doe"
            it[cityId] = busanId
        }
        UserTable.insert {
            it[id] = "john"
            it[name] = "John.Doe"
            it[cityId] = busanId
        }

        UserTable.insert {
            it[id] = "alex"
            it[name] = "Alex"
            it[cityId] = null
        }
        UserTable.insert {
            it[id] = "smth"
            it[name] = "Something"
            it[cityId] = null
        }
    }
}
