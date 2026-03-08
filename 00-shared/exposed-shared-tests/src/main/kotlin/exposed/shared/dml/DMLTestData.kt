package exposed.shared.dml

import exposed.shared.dml.DMLTestData.Users.Flags
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import java.math.BigDecimal

/**
 * DML(Data Manipulation Language) 테스트에서 사용하는 샘플 테이블 및 데이터 정의 객체.
 *
 * Cities, Users, UserData, Sales 등 다양한 테이블과 테스트 데이터 삽입 헬퍼 함수를 제공합니다.
 */
object DMLTestData {

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS cities (
     *      city_id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object Cities: Table() {
        val id = integer("city_id").autoIncrement()
        val name = varchar("name", 50)

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS users (
     *      id VARCHAR(10) PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      city_id INT NULL,
     *      flags INT DEFAULT 0 NOT NULL,
     *
     *      CONSTRAINT fk_users_city_id__city_id FOREIGN KEY (city_id) REFERENCES cities(city_id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Users: Table() {
        val id = varchar("id", 10)
        val name = varchar("name", 50)
        val cityId = optReference("city_id", Cities.id)
        val flags = integer("flags").default(0)

        override val primaryKey = PrimaryKey(id)

        object Flags {
            const val IS_ADMIN = 0b1
            const val HAS_DATA = 0b1000
        }
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS userdata (
     *      user_id VARCHAR(10) NOT NULL,
     *      "comment" VARCHAR(30) NOT NULL,
     *      "value" INT NOT NULL,
     *
     *      CONSTRAINT fk_userdata_user_id__id FOREIGN KEY (user_id) REFERENCES users(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object UserData: Table() {
        val userId = reference("user_id", Users.id)
        val comment = varchar("comment", 30)
        val value = integer("value")
    }

    /**
     * Postgres:
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS sales (
     *      "year" INT NOT NULL,
     *      "month" INT NOT NULL,
     *      product VARCHAR(30) NULL,
     *      amount DECIMAL(8, 2) NOT NULL
     * )
     * ```
     */
    object Sales: Table() {
        val year = integer("year")
        val month = integer("month")
        val product = varchar("product", 30).nullable()
        val amount = decimal("amount", 8, 2)
    }

    /**
     * Postgres:
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS someamounts (
     *      amount DECIMAL(8, 2) NOT NULL
     * )
     * ```
     */
    object SomeAmounts: Table() {
        val amount = decimal("amount", 8, 2)
    }


    /**
     * [ResultRow] 컬렉션에서 도시 이름 목록을 추출합니다.
     *
     * @return 도시 이름 문자열 리스트
     */
    fun Iterable<ResultRow>.toCityNameList(): List<String> =
        map { it[Cities.name] }


    /**
     * 도시, 사용자, 사용자 데이터 테이블을 생성하고 샘플 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param testDB 사용할 테스트 데이터베이스
     * @param statement 테이블과 샘플 데이터가 준비된 상태에서 실행할 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    fun AbstractExposedTest.withCitiesAndUsers(
        testDB: TestDB,
        statement: JdbcTransaction.(
            cities: Cities,
            users: Users,
            userData: UserData,
        ) -> Unit,
    ) {
        val users = Users
        // val userFlags = Flags
        val cities = Cities
        val userData = UserData

        withTables(testDB, cities, users, userData) {
            val saintPetersburgId = cities.insert {
                it[name] = "St. Petersburg"
            } get Cities.id

            val munichId = cities.insert {
                it[name] = "Munich"
            } get Cities.id

            cities.insert {
                it[name] = "Prague"
            }

            users.insert {
                it[id] = "andrey"
                it[name] = "Andrey"
                it[cityId] = saintPetersburgId
                it[flags] = Flags.IS_ADMIN
            }

            users.insert {
                it[id] = "sergey"
                it[name] = "Sergey"
                it[cityId] = munichId
                it[flags] = Flags.IS_ADMIN or Flags.HAS_DATA
            }

            users.insert {
                it[id] = "eugene"
                it[name] = "Eugene"
                it[cityId] = munichId
                it[flags] = Flags.HAS_DATA
            }

            users.insert {
                it[id] = "alex"
                it[name] = "Alex"
                it[cityId] = null
            }

            users.insert {
                it[id] = "smth"
                it[name] = "Something"
                it[cityId] = null
                it[flags] = Flags.HAS_DATA
            }

            userData.insert {
                it[userId] = "smth"
                it[comment] = "Something is here"
                it[value] = 10
            }

            userData.insert {
                it[userId] = "smth"
                it[comment] = "Comment #2"
                it[value] = 20
            }

            userData.insert {
                it[userId] = "eugene"
                it[comment] = "Comment for Eugene"
                it[value] = 20
            }

            userData.insert {
                it[userId] = "sergey"
                it[comment] = "Comment for Sergey"
                it[value] = 30
            }

            statement(cities, users, userData)
        }
    }

    /**
     * 판매 데이터 테이블을 생성하고 샘플 판매 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param dialect 사용할 테스트 데이터베이스 방언
     * @param statement 테이블과 샘플 데이터가 준비된 상태에서 실행할 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    fun AbstractExposedTest.withSales(
        dialect: TestDB,
        statement: JdbcTransaction.(testDB: TestDB, sales: Sales) -> Unit,
    ) {
        val sales = Sales

        withTables(dialect, sales) { testDB ->
            insertSale(2018, 11, "tea", "550.10")
            insertSale(2018, 12, "coffee", "1500.25")
            insertSale(2018, 12, "tea", "900.30")
            insertSale(2019, 1, "coffee", "1620.10")
            insertSale(2019, 1, "tea", "650.70")
            insertSale(2019, 2, "coffee", "1870.90")
            insertSale(2019, 2, null, "10.20")

            statement(testDB, sales)
        }
    }

    private fun insertSale(year: Int, month: Int, product: String?, amount: String) {
        val sales = Sales
        sales.insert {
            it[Sales.year] = year
            it[Sales.month] = month
            it[Sales.product] = product
            it[Sales.amount] = amount.toBigDecimal()
        }
    }

    /**
     * 금액 테이블을 생성하고 샘플 금액 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param dialect 사용할 테스트 데이터베이스 방언
     * @param statement 테이블과 샘플 데이터가 준비된 상태에서 실행할 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    fun AbstractExposedTest.withSomeAmounts(
        dialect: TestDB,
        statement: JdbcTransaction.(testDB: TestDB, someAmounts: SomeAmounts) -> Unit,
    ) {
        val someAmounts = SomeAmounts

        withTables(dialect, someAmounts) {
            fun insertAmount(amount: BigDecimal) {
                someAmounts.insert {
                    it[SomeAmounts.amount] = amount
                }
            }
            insertAmount("650.70".toBigDecimal())
            insertAmount("1500.25".toBigDecimal())
            insertAmount("1000.00".toBigDecimal())

            statement(it, someAmounts)
        }
    }

    /**
     * 판매 데이터 및 금액 테이블을 함께 생성하고 샘플 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param dialect 사용할 테스트 데이터베이스 방언
     * @param statement 두 테이블과 샘플 데이터가 준비된 상태에서 실행할 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    fun AbstractExposedTest.withSalesAndSomeAmounts(
        dialect: TestDB,
        statement: JdbcTransaction.(
            testDB: TestDB,
            sales: Sales,
            someAmounts: SomeAmounts,
        ) -> Unit,
    ) {
        val sales = Sales
        val someAmounts = SomeAmounts

        withTables(dialect, sales, someAmounts) { testDB ->
            insertSale(2018, 11, "tea", "550.10")
            insertSale(2018, 12, "coffee", "1500.25")
            insertSale(2018, 12, "tea", "900.30")
            insertSale(2019, 1, "coffee", "1620.10")
            insertSale(2019, 1, "tea", "650.70")
            insertSale(2019, 2, "coffee", "1870.90")
            insertSale(2019, 2, null, "10.20")

            fun insertAmount(amount: BigDecimal) {
                someAmounts.insert {
                    it[SomeAmounts.amount] = amount
                }
            }
            insertAmount("650.70".toBigDecimal())
            insertAmount("1500.25".toBigDecimal())
            insertAmount("1000.00".toBigDecimal())

            statement(testDB, sales, someAmounts)
        }
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS orgs (
     *      id SERIAL PRIMARY KEY,
     *      uid VARCHAR(36) NOT NULL,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ALTER TABLE orgs ADD CONSTRAINT orgs_uid_unique UNIQUE (uid)
     * ```
     */
    object Orgs: IntIdTable() {
        val uid = varchar("uid", 36)
            .uniqueIndex()
            .clientDefault { TimebasedUuid.Reordered.nextIdAsString() }
        val name = varchar("name", 255)
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS orgmemberships (
     *      id SERIAL PRIMARY KEY,
     *      org VARCHAR(36) NOT NULL,
     *
     *      CONSTRAINT fk_orgmemberships_org__uid FOREIGN KEY (org) REFERENCES orgs(uid)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object OrgMemberships: IntIdTable() {
        val orgId = reference("org", Orgs.uid)
    }

    class Org(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Org>(Orgs)

        var uid by Orgs.uid
        var name by Orgs.name
    }

    class OrgMembership(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<OrgMembership>(OrgMemberships)

        var orgId by OrgMemberships.orgId
        var org by Org referencedOn OrgMemberships.orgId
    }

}
