package exposed.examples.entities

import exposed.shared.mapping.compositeId.BookSchema
import exposed.shared.mapping.compositeId.BookSchema.Author
import exposed.shared.mapping.compositeId.BookSchema.Authors
import exposed.shared.mapping.compositeId.BookSchema.Book
import exposed.shared.mapping.compositeId.BookSchema.Books
import exposed.shared.mapping.compositeId.BookSchema.Office
import exposed.shared.mapping.compositeId.BookSchema.Offices
import exposed.shared.mapping.compositeId.BookSchema.Publisher
import exposed.shared.mapping.compositeId.BookSchema.Publishers
import exposed.shared.mapping.compositeId.BookSchema.Review
import exposed.shared.mapping.compositeId.BookSchema.Reviews
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.TestDB.H2_V1
import exposed.shared.tests.currentTestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.idParam
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.util.*
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@Suppress("DEPRECATION")
class Ex10_CompositeIdTableEntity: JdbcExposedTestBase() {

    companion object: KLogging()

    private val allTables = BookSchema.allTables

    /**
     * CompositeID를 가지는 모든 Table 들을 생성하고 삭제되는지 테스트한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create and drop CompositeIdTable`(testDB: TestDB) {
        withDb(testDB) {
            try {
                SchemaUtils.create(tables = allTables)

                allTables.forEach { it.exists().shouldBeTrue() }

                if (testDB !in TestDB.ALL_H2) {
                    SchemaUtils.statementsRequiredToActualizeScheme(tables = allTables).shouldBeEmpty()
                }
            } finally {
                SchemaUtils.drop(tables = allTables)
            }
        }
    }

    /**
     * CompositeID를 가진 엔티티를 Custom Constructor를 이용하여 생성하고 조회한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select using DAO`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            /**
             * ```sql
             * INSERT INTO PUBLISHERS (PUBLISHER_NAME, ISBN_CODE)
             * VALUES ('Publisher A', 'f0ca4ab7-5324-4162-a99d-815e9a34ded7')
             * ```
             */
            val p1 = Publisher.new(UUID.randomUUID()) {
                name = "Publisher A"
            }

            entityCache.clear()

            val result1 = Publisher.all().single()
            result1.name shouldBeEqualTo "Publisher A"

            // 엔티티들끼리 비교하기 (equals, hashCode 재정의를 제대로 해줘야함)
            result1 shouldBeEqualTo p1

            // 또는 엔티티 ID 를 비교하기
            result1.id shouldBeEqualTo p1.id

            // 또는 엔티티 ID의 wrapped value 를 비교하기
            result1.id.value shouldBeEqualTo p1.id.value

            // 또는 CompositeID의 각 컬럼의 값을 비교하기
            result1.pubId shouldBeEqualTo p1.pubId
            result1.isbn shouldBeEqualTo p1.isbn

            // pubId는 자동증가, isbn 은 UUID.randomUUID() 로 설정됨
            Publisher.new {
                name = "Publisher B"
            }
            // pubId는 자동증가, isbn 은 UUID.randomUUID() 로 설정됨
            Publisher.new {
                name = "Publisher C"
            }

            entityCache.clear()

            val result2 = Publisher.all().toList()
            result2 shouldHaveSize 3
        }
    }

    /**
     * SQL DSL query builder 를 이용하여 CompositeID 엔티티를 생성하고 조회한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select using DSL`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            Publishers.insert {
                it[Publishers.name] = "Publisher A"
            }
            entityCache.clear()

            val result: ResultRow = Publishers.selectAll().single()
            result[Publishers.name] shouldBeEqualTo "Publisher A"

            // 하나의 ResultRow에 CompositeID를 구성하는 모든 컬럼에 접근할 수 있다.
            val idResult = result[Publishers.id]
            assertIs<EntityID<CompositeID>>(idResult)
            idResult.value[Publishers.pubId] shouldBeEqualTo result[Publishers.pubId]
            idResult.value[Publishers.isbn] shouldBeEqualTo result[Publishers.isbn]

            // DSL query builder 에서 composite id column 을 사용하는 것이 작동하는지 테스트
            // SELECT publishers.isbn_code, publishers.pub_id FROM publishers WHERE (publishers.isbn_code = ?) AND (publishers.pub_id = ?)
            val dslQuery = Publishers
                .select(Publishers.id)
                .where { Publishers.id eq idResult }
                .prepareSQL(this)

            log.debug { "DSL Query: $dslQuery" }

            val selectClause = dslQuery.substringAfter("SELECT ").substringBefore(" FROM")

            // ID 컬럼은 PK 로부터 2개의 컬럼으로 분해되어야 한다. (pubId, isbn)
            selectClause.split(", ", ignoreCase = true) shouldHaveSize 2
            val whereClause = dslQuery.substringAfter(" WHERE ")

            // Composite PK 에 있는 2 개의 컬럼이 AND 연산자로 결합되어야 한다. (pubId, isbn)
            whereClause.split(" AND ", ignoreCase = true) shouldHaveSize 2

            // CompositeID 를 생성할 때 필요한 모든 컬럼의 값을 지정하지 않으면 에러가 발생해야 한다.
            assertFailsWith<IllegalStateException> {
                // isbn 컬럼의 값이 지정되지 않음
                val fake = EntityID(CompositeID { it[Publishers.pubId] = 7 }, Publishers)
                Publishers.selectAll().where { Publishers.id eq fake }
            }.apply {
                log.debug(this) { "예상되는 예외입니다" }
            }

            // Composite ID 의 일부 값으로 비교하는 것이 작동한다.
            // SELECT COUNT(*) FROM publishers WHERE publishers.pub_id <> 1
            val pubIdValue: Int = idResult.value[Publishers.pubId].value
            Publishers.selectAll()
                .where { Publishers.pubId neq pubIdValue }
                .count() shouldBeEqualTo 0L
        }
    }

    /**
     * `autoGenerated` 컬럼을 포함한 CompositeID를 가진 엔티티를 생성한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with compositeId auto generated parts using DAO`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            // `isbn`은 autoGenerated 컬럼이므로 값을 지정하지 않아도 된다.
            // INSERT INTO publishers (pub_id, publisher_name, isbn_code)
            // VALUES (578, 'Publisher A', 'f6d41ba3-c412-4cae-a985-5dd1effb34c1')
            val p1 = Publisher.new(CompositeID { it[Publishers.pubId] = 578 }) {
                name = "Publisher A"
            }
            entityCache.clear()

            val found1 = Publisher.find { Publishers.pubId eq 578 }.single()
            found1 shouldBeEqualTo p1
            found1.name shouldBeEqualTo "Publisher A"

            // `pubId`는 autoIncrement 컬럼이므로 값을 지정하지 않아도 된다.
            // INSERT INTO publishers (isbn_code, publisher_name)
            // VALUES ('41b990be-4f1f-40c1-8805-302da3e1e9ab', 'Publisher B')
            val isbn = UUID.randomUUID()
            val p2 = Publisher.new(CompositeID { it[Publishers.isbn] = isbn }) {
                name = "Publisher B"
            }
            entityCache.clear()

            val found2 = Publisher.find { Publishers.isbn eq isbn }.single()
            found2 shouldBeEqualTo p2
            found2.name shouldBeEqualTo "Publisher B"

            val expectedNextVal1 =
                if (currentTestDB in TestDB.ALL_MYSQL_LIKE || currentTestDB == H2_V1) 579 else 1
            found2.id.value[Publishers.pubId].value shouldBeEqualTo expectedNextVal1
        }
    }

    /**
     * CompositeID의 일부 컬럼이 autoIncrement 이거나 autoGenerated이 아닐 경우,
     * 해당 컬럼의 값을 지정하지 않으면 에러가 발생해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with composite id auto generated parts and missing not generated part using DAO`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val publisherA = Publisher.new {
                name = "Publisher A"
            }
            val authorA = Author.new {
                publisher = publisherA
                penName = "Author A"
            }
            val boolA = Book.new {
                title = "Book A"
                author = authorA
            }
            entityCache.clear()

            /**
             * `Reviews` 는 `rank`, `content` 둘 다 autoIncrement/autoGenerated 가 아니므로,
             * `content` 를 지정하지 않으면 에러가 발생해야 한다.
             */
            /**
             * `Reviews` 는 `rank`, `content` 둘 다 autoIncrement/autoGenerated 가 아니므로,
             * `content` 를 지정하지 않으면 에러가 발생해야 한다.
             */
            val compositeID = CompositeID {
                it[Reviews.rank] = 10L
            }
            expectException<IllegalStateException> {
                Review.new(compositeID) {
                    book = boolA
                }
            }
        }
    }

    /**
     * `insertAndGetId` 를 이용하여 CompositeID를 가진 행을 생성하고 반환받는다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and get compositeIds`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            // insert individual components (pubId, isbn 이 composite key 이다)
            val id1: EntityID<CompositeID> = Publishers.insertAndGetId {
                it[Publishers.pubId] = 725
                it[Publishers.isbn] = UUID.randomUUID()
                it[Publishers.name] = "Publisher A"
            }
            entityCache.clear()

            id1.value[Publishers.pubId].value shouldBeEqualTo 725

            val id2 = Publishers.insertAndGetId {
                it[Publishers.name] = "Publisher B"
            }
            // MYSQL 을 제외하면, pubId 는 autoIncrement 이므로 1 이어야 한다.
            val expectedNextVal1 =
                if (currentTestDB in TestDB.ALL_MYSQL_LIKE || currentTestDB == H2_V1) 726 else 1
            id2.value[Publishers.pubId].value shouldBeEqualTo expectedNextVal1

            // CompositeID 에 속한 모든 컬럼에 값을 지정하여 insert 한다.
            val id3 = Publishers.insertAndGetId {
                it[id] = CompositeID { id ->
                    id[Publishers.pubId] = 999
                    id[Publishers.isbn] = UUID.randomUUID()
                }
                it[Publishers.name] = "Publisher C"
            }
            id3.value[Publishers.pubId].value shouldBeEqualTo 999

            // `EntityID<CompositeID>` 를 생성해서 `id` 컬럼에 지정한다.
            val id4 = Publishers.insertAndGetId {
                it[id] = EntityID(
                    CompositeID { id ->
                        id[Publishers.pubId] = 111
                        id[Publishers.isbn] = UUID.randomUUID()
                    },
                    Publishers
                )
                it[Publishers.name] = "Publisher D"
            }
            id4.value[Publishers.pubId].value shouldBeEqualTo 111

            // CompositeID 의 일부 컬럼이 autoGenerated 이므로, 값을 지정하지 않아도 된다.
            val id5 = Publishers.insertAndGetId {
                it[id] = CompositeID { id -> id[Publishers.pubId] = 1001 }
                it[Publishers.name] = "Publisher E"
            }
            id5.value[Publishers.pubId].value shouldBeEqualTo 1001

            // CompositeID 의 일부 컬럼이 autoIncrement 이므로, 값을 지정하지 않아도 된다.
            val id6 = Publishers.insertAndGetId {
                it[id] = CompositeID { id -> id[Publishers.isbn] = UUID.randomUUID() }
                it[Publishers.name] = "Publisher F"
            }

            val expectedNextVal2 =
                if (currentTestDB in TestDB.ALL_MYSQL_LIKE || currentTestDB == H2_V1) 1002 else 2
            id6.value[Publishers.pubId].value shouldBeEqualTo expectedNextVal2
        }
    }

    /**
     * CompositeID를 구성하는 컬럼들을 개별로 지정하여 INSERT 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert using manual composite ids`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            /**
             * DSL 방식에서 CompositeID의 컬럼들을 개별적으로 설정한다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO publishers (pub_id, isbn_code, publisher_name)
             * VALUES (725, '31027894-9b64-497a-8d5b-1b1ba3b9942d', 'Publisher A')
             * ```
             */
            Publishers.insert {
                it[Publishers.pubId] = 725
                it[Publishers.isbn] = UUID.randomUUID()
                it[Publishers.name] = "Publisher A"
            }
            entityCache.clear()

            Publishers.selectAll().single()[Publishers.pubId].value shouldBeEqualTo 725

            /**
             * DAO 방식에서 CompositeID 를 구성해서 제공한다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO publishers (isbn_code, pub_id, publisher_name)
             * VALUES ('026f07c4-c8ef-496f-96ba-78883b4dfc32', 611, 'Publisher B')
             * ```
             */
            val fullId = CompositeID {
                it[Publishers.pubId] = 611
                it[Publishers.isbn] = UUID.randomUUID()
            }
            val p2Id = Publisher.new(fullId) {
                name = "Publisher B"
            }.id
            entityCache.clear()

            p2Id.value[Publishers.pubId].value shouldBeEqualTo 611
            Publisher.findById(p2Id)?.id?.value?.get(Publishers.pubId)?.value shouldBeEqualTo 611
        }
    }

    /**
     * CompositeID 를 이용하여 조회 (`findById`) 하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find by composite id`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            val id1: EntityID<CompositeID> = Publishers.insertAndGetId {
                it[Publishers.pubId] = 725
                it[Publishers.isbn] = UUID.randomUUID()
                it[Publishers.name] = "Publisher A"
            }
            entityCache.clear()

            /**
             * CompositeID를 구성하는 `pubId` 와 `isbn` 컬럼의 값으로 엔티티를 조회한다.
             * ```sql
             * SELECT publishers.pub_id,
             *        publishers.isbn_code,
             *        publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.isbn_code = '94c88967-3b67-4e6f-85be-1564b2f3b0c0')
             *    AND (publishers.pub_id = 725)
             * ```
             */
            val p1: Publisher? = Publisher.findById(id1)
            p1.shouldNotBeNull()
            p1.id.value[Publishers.pubId].value shouldBeEqualTo id1.value[Publishers.pubId].value
            p1.id.value[Publishers.isbn].value shouldBeEqualTo id1.value[Publishers.isbn].value

            /**
             * CompositeID에 해당하는 `pubId` 와 `isbn` 컬럼의 값을 기본값을 사용하도록 한다
             *
             * pubId 는 autoIncrement 이므로, DB에서 발행한다 (여기서는 1 이어야 한다)
             * isbn 은 autoGenerated 이므로, UUID 값이어야 한다.
             *
             * ```sql
             * INSERT INTO publishers (publisher_name, isbn_code)
             * VALUES ('Publisher B', 'ef2825d8-df36-4ecf-976d-c0a158cd7cd1')
             * ```
             */
            val id2: EntityID<CompositeID> = Publisher.new {
                name = "Publisher B"
            }.id

            entityCache.clear()

            /**
             * id2 를 이용하여 엔티티를 조회한다.
             *
             * ```sql
             * SELECT publishers.pub_id,
             *        publishers.isbn_code,
             *        publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.isbn_code = '66cd5670-fc83-43ab-8a7b-762830258f71')
             *    AND (publishers.pub_id = 1)
             * ```
             */
            val p2: Publisher? = Publisher.findById(id2)
            p2.shouldNotBeNull()
            p2.name shouldBeEqualTo "Publisher B"
            p2.id.value[Publishers.pubId] shouldBeEqualTo id2.value[Publishers.pubId]  // 1
            p2.id.value[Publishers.isbn] shouldBeEqualTo id2.value[Publishers.isbn]  // UUID
            p2.id shouldBeEqualTo id2  // 이걸로 간단하게 할 수 있다.

            // EntityID의 값에 해당하는 CompositeID 로 조회하기
            val compositeId1: CompositeID = id1.value
            val p3: Publisher? = Publisher.findById(compositeId1)
            p3.shouldNotBeNull() shouldBeEqualTo p1
        }
    }

    /**
     * CompositeID를 가지는 엔티티를 DSL 를 이용하여 조회한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find with DSL Builder`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            val p1 = Publisher.new {
                name = "Publisher A"
            }
            entityCache.clear()

            /**
             * CompositeID의 일부 컬럼의 값을 LIKE 를 이용하여 조회한다.
             *
             * ```sql
             * SELECT publishers.pub_id, publishers.isbn_code, publishers.publisher_name
             *   FROM publishers
             *  WHERE publishers.publisher_name LIKE '% A'
             * ```
             */
            Publisher.find { Publishers.name like "% A" }.single().id shouldBeEqualTo p1.id

            /**
             * CompositeID로 조회 (CompositeID의 모든 컬럼의 값이 일치해야 한다)
             *
             * ```sql
             * SELECT publishers.pub_id, publishers.isbn_code, publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.isbn_code = '579e537c-cd12-47a7-8476-f8786f12a781')
             *    AND (publishers.pub_id = 1)
             * ```
             */
            val p2 = Publisher.find { Publishers.id eq p1.id }.single()
            p2 shouldBeEqualTo p1

            /**
             * CompositeID의 일부 컬럼의 값을 이용하여 조회한다.
             *
             * ```sql
             * SELECT publishers.pub_id, publishers.isbn_code, publishers.publisher_name
             *   FROM publishers
             *  WHERE publishers.isbn_code = '579e537c-cd12-47a7-8476-f8786f12a781'
             * ```
             */
            val existingIsbnValue: UUID = p1.id.value[Publishers.isbn].value
            val p3 = Publisher.find { Publishers.isbn eq existingIsbnValue }.single()
            p3 shouldBeEqualTo p1
        }
    }

    /**
     * `idParam` 을 이용하여 CompositeID를 가진 엔티티를 조회한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `조회 시 idParam with composite value`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            val publisherId = CompositeID {
                it[Publishers.pubId] = 42
                it[Publishers.isbn] = UUID.randomUUID()
            }
            val publisherEntityId: EntityID<CompositeID> = Publishers.insertAndGetId {
                it[id] = publisherId
                it[Publishers.name] = "Publisher A"
            }

            entityCache.clear()

            /**
             * Publishers.id 에 해당하는 CompositeID의 parameters 에 publisherEntityId 를 지정한다.
             *
             * ```sql
             * -- Postgres
             * SELECT publishers.pub_id, publishers.isbn_code, publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.isbn_code = 'a3e57c35-5393-4fe3-9e19-666276b24098')
             *    AND (publishers.pub_id = 42)
             * ```
             */
            val query: Query = Publishers
                .selectAll()
                .where { Publishers.id eq idParam(publisherEntityId, Publishers.id) }

            val selectClause = query.prepareSQL(this, prepared = true)
            log.debug { "Select Clause: $selectClause" }

            query.single()[Publishers.name] shouldBeEqualTo "Publisher A"
        }
    }

    /**
     * CompositeID를 가지는 엔티티에 대한 업데이트를 테스트한다.
     *
     * ```sql
     * UPDATE publishers
     *    SET publisher_name='Publisher B'
     *  WHERE isbn_code = 'cc99457f-8225-45ae-9cf6-22d79c39c578' AND pub_id = 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update composite entity`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            val p1 = Publisher.new {
                name = "Publisher A"
            }
            entityCache.clear()

            Publisher.all().count() shouldBeEqualTo 1L
            Publisher.all().single().name shouldBeEqualTo "Publisher A"

            p1.name = "Publisher B"
            entityCache.clear()

            Publisher.findById(p1.id)?.name shouldBeEqualTo "Publisher B"
        }
    }

    /**
     * CompositeID를 가지는 엔티티에 대한 삭제를 테스트한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete composite entity`(testDB: TestDB) {
        withTables(testDB, Publishers) {
            val p1 = Publisher.new { name = "Publisher A" }
            val p2 = Publisher.new { name = "Publisher B" }

            Publisher.all().count() shouldBeEqualTo 2L

            entityCache.clear()

            /**
             * 엔티티를 삭제하는 경우, CompositeID의 모든 컬럼의 값이 일치해야 한다.
             *
             *```sql
             * -- Postgres
             * DELETE FROM publishers
             *  WHERE (publishers.isbn_code = '9dbf99c1-c3ee-4662-a03b-778175279a39')
             *    AND (publishers.pub_id = 1)
             * ```
             */
            p1.delete()

            val result = Publisher.all().single()
            result shouldBeEqualTo p2

            /**
             * CompositeID 의 일부 컬럼의 값만을 이용하여 삭제한다.
             *
             * ```sql
             * DELETE FROM publishers WHERE publishers.pub_id = 2
             * ```
             */
            val existingPubIdValue: Int = p2.id.value[Publishers.pubId].value  // 2
            Publishers.deleteWhere { Publishers.pubId eq existingPubIdValue }
            Publisher.all().count() shouldBeEqualTo 0L
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS towns (
     *      zip_code VARCHAR(8),
     *      "name" VARCHAR(64),
     *      population BIGINT NULL,
     *
     *      CONSTRAINT pk_towns PRIMARY KEY (zip_code, "name")
     * )
     * ```
     */
    object Towns: CompositeIdTable("towns") {
        val zipCode = varchar("zip_code", 8).entityId()
        val name = varchar("name", 64).entityId()
        val population = long("population").nullable()

        override val primaryKey = PrimaryKey(zipCode, name)
    }

    class Town(id: EntityID<CompositeID>): CompositeEntity(id) {
        companion object: CompositeEntityClass<Town>(
            Ex10_CompositeIdTableEntity.Towns
        )

        var population by Ex10_CompositeIdTableEntity.Towns.population

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("population", population)
            .toString()
    }

    /**
     * `isNull()` 와 `eq()` 를 사용하여 CompositeID를 가진 엔티티를 조회한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `isNull and eq with alias`(testDB: TestDB) {
        withTables(testDB, Towns) {
            val townAValue = CompositeID {
                it[Towns.zipCode] = "1A2 3B4"
                it[Towns.name] = "Town A"
            }
            val townAId: EntityID<CompositeID> =
                Towns.insertAndGetId {
                    it[id] = townAValue
                }

            flushCache()

            val smallCity = Towns.alias("small_city")

            /**
             * ```sql
             * SELECT small_city.zip_code, small_city."name", small_city.population
             *   FROM towns small_city
             *  WHERE (small_city.population IS NULL)
             *    AND (small_city.zip_code = '1A2 3B4') AND (small_city."name" = 'Town A')
             * ```
             */
            val result1 = smallCity.selectAll()
                .where {
                    smallCity[Towns.population].isNull() and (smallCity[Towns.id] eq townAId)
                }
                .single()

            result1[smallCity[Towns.population]].shouldBeNull()

            /**
             * ```sql
             * SELECT small_city."name"
             *   FROM towns small_city
             *  WHERE (small_city.zip_code = '1A2 3B4') AND (small_city."name" = 'Town A')
             * ```
             */
            val result2 = smallCity
                .select(smallCity[Towns.name])
                .where { smallCity[Towns.id] eq townAId.value }
                .single()

            result2[smallCity[Towns.name]] shouldBeEqualTo townAValue[Towns.name]
        }
    }

    /**
     * `idParam` 을 이용하여 CompositeID를 가진 엔티티를 조회한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `id param with composite value`(testDB: TestDB) {
        withTables(testDB, Towns) {
            val townAValue = CompositeID {
                it[Towns.zipCode] = "1A2 3B4"
                it[Towns.name] = "Town A"
            }
            val townAId: EntityID<CompositeID> =
                Towns.insertAndGetId {
                    it[id] = townAValue
                    it[Towns.population] = 4
                }

            entityCache.clear()

            /**
             * Towns.id 에 해당하는 CompositeID의 parameters 에 townAId 를 지정한다.
             *
             * ```sql
             * SELECT TOWNS.ZIP_CODE, TOWNS."name", TOWNS.POPULATION
             *   FROM TOWNS
             *  WHERE (TOWNS.ZIP_CODE = ?) AND (TOWNS."name" = ?)
             * ```
             */
            val query: Query = Towns.selectAll()
                .where { Towns.id eq idParam(townAId, Towns.id) }

            val selectClause = query.prepareSQL(this, prepared = true)
            log.debug { "Select Clause: $selectClause" }

            val whereClause = selectClause.substringAfter("WHERE ")
            whereClause shouldBeEqualTo "(${fullIdentity(Towns.zipCode)} = ?) AND (${
                fullIdentity(
                    Towns.name
                )
            } = ?)"

            query.single()[Towns.population] shouldBeEqualTo 4
        }
    }

    /**
     * 갱신된 Entity를 flush 한 후 조회한다.
     *
     * `inTopLevelTransaction` 을 사용하면, 새로운 Tx 에 의해 작동되므로, 엔티티 캐시가 공유되지 않는다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `flushing updated entity`(testDB: TestDB) {
        withTables(testDB, Towns) {
            val id = CompositeID {
                it[Towns.zipCode] = "1A2 3B4"
                it[Towns.name] = "Town A"
            }

            /**
             * ID를 지정하여 Entity를 생성한다.
             *
             * ```sql
             *  INSERT INTO towns (zip_code, "name", population)
             *  VALUES ('1A2 3B4', 'Town A', 1000)
             *  ```
             */
            inTopLevelTransaction(Connection.TRANSACTION_SERIALIZABLE) {
                Town.new(id) {
                    population = 1000
                }
            }
            /**
             * EntityID를 이용하여 조회한 후, population 값을 갱신한다.
             */
            inTopLevelTransaction(Connection.TRANSACTION_SERIALIZABLE) {
                val town = Town[id]
                town.population = 2000
            }
            /**
             * EntityID를 이용하여 조회한 후, population 값을 확인한다
             */
            inTopLevelTransaction(Connection.TRANSACTION_SERIALIZABLE) {
                val town = Town[id]
                town.population shouldBeEqualTo 2000
            }
        }
    }

    /**
     * 참조된 엔티티를 생성하고 조회한다.
     *
     * ```sql
     * SELECT reviews.code, reviews.`rank`, reviews.book_id
     *   FROM reviews
     *  WHERE reviews.book_id = 1
     * ```
     *
     * ```sql
     * SELECT authors.id, authors.publisher_id, authors.publisher_isbn, authors.pen_name
     *   FROM authors
     *  WHERE (authors.publisher_id = 1) AND (authors.publisher_isbn = '80776f58-24b0-4892-bec6-c2de2ca8fa4b')
     *```
     * ```sql
     * SELECT offices.zip_code,
     *        offices.`name`,
     *        offices.area_code,
     *        offices.staff,
     *        offices.publisher_id,
     *        offices.publisher_isbn
     *   FROM offices
     *  WHERE (offices.publisher_id = 1) AND (offices.publisher_isbn = '80776f58-24b0-4892-bec6-c2de2ca8fa4b')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select referenced entities`(testDB: TestDB) {
        withTables(testDB, tables = allTables) {
            val publisherA = Publisher.new {
                name = "Publisher A"
            }
            val authorA = Author.new {
                publisher = publisherA
                penName = "Author A"
            }
            val authorB = Author.new {
                publisher = publisherA
                penName = "Author B"
            }
            val bookA = Book.new {
                title = "Book A"
                author = authorB
            }
            val bookB = Book.new {
                title = "Book B"
                author = authorB
            }
            val reviewIdValue = CompositeID {
                it[Reviews.content] = "Not bad"
                it[Reviews.rank] = 12345L
            }
            val reviewA = Review.new(reviewIdValue) {
                book = bookA
            }
            val officeAIdValue = CompositeID {
                it[Offices.zipCode] = "1A2 3B4"
                it[Offices.name] = "Office A"
                it[Offices.areaCode] = 789
            }
            val officeA = Office.new(officeAIdValue) { }
            val officeBIdValue = CompositeID {
                it[Offices.zipCode] = "5C6 7D8"
                it[Offices.name] = "Office B"
                it[Offices.areaCode] = 456
            }
            val officeB = Office.new(officeBIdValue) {
                publisher = publisherA
            }

            entityCache.clear()

            // Child 엔티티 참조 (Author -> Publisher, Book -> Author, Review -> Book, Office -> Publisher)
            authorA.publisher.id.value[Publishers.pubId] shouldBeEqualTo publisherA.id.value[Publishers.pubId]
            authorA.publisher shouldBeEqualTo publisherA
            authorB.publisher shouldBeEqualTo publisherA
            bookA.author?.publisher shouldBeEqualTo publisherA
            bookA.author shouldBeEqualTo authorB
            reviewA.book shouldBeEqualTo bookA
            reviewA.book.author shouldBeEqualTo authorB
            officeA.publisher.shouldBeNull()
            officeB.publisher shouldBeEqualTo publisherA

            // parent entity references
            // 부모 엔티티 참조 (Book -> Review, Publisher -> Author, Office)
            bookA.review.shouldNotBeNull() shouldBeEqualTo reviewA
            publisherA.authors.toList() shouldContainSame listOf(authorA, authorB)
            publisherA.office.shouldNotBeNull()

            // 만약 복수 개의 자식이 부모 엔티티를 참조하면, `backReference` 와 `optBackReferencedOn` 은 마지막 것을 저장한다.
            publisherA.office shouldBeEqualTo officeB
            publisherA.allOffices.toList() shouldContainSame listOf(officeB)
        }
    }

    /**
     * `inList` 연산자를 CompositeID 엔티티 조회에 사용하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inList with CompositeID Entities`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val publisherA = Publisher.new {
                name = "Publisher A"
            }
            val authorA = Author.new {
                publisher = publisherA
                penName = "Author A"
            }
            val authorB = Author.new {
                publisher = publisherA
                penName = "Author B"
            }
            val officeAIdValue = CompositeID {
                it[Offices.zipCode] = "1A2 3B4"
                it[Offices.name] = "Office A"
                it[Offices.areaCode] = 789
            }
            val officeA = Office.new(officeAIdValue) { }
            val officeBIdValue = CompositeID {
                it[Offices.zipCode] = "5C6 7D8"
                it[Offices.name] = "Office B"
                it[Offices.areaCode] = 456
            }
            val officeB = Office.new(officeBIdValue) {
                publisher = publisherA
            }

            commit()

            /**
             * Autor 를 조회할 때, Author의 Publisher 도 Eager Loading 할 때
             *
             * ```sql
             * SELECT authors.id, authors.publisher_id, authors.publisher_isbn, authors.pen_name
             *   FROM authors
             *  WHERE authors.id = 1
             * ```
             * ```sql
             * SELECT publishers.pub_id,
             *        publishers.isbn_code,
             *        publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.pub_id, publishers.isbn_code) = (1, 'd2090a50-3290-4026-88c1-1aa92bd775b0')
             * ```
             */
            inTopLevelTransaction(Connection.TRANSACTION_READ_COMMITTED) {
                maxAttempts = 1
                // preload referencedOn - child to single parent
                Author.find { Authors.id eq authorA.id }.first().load(Author::publisher)

                val foundAuthor = Author.testCache(authorA.id)
                foundAuthor.shouldNotBeNull()
                Publisher.testCache(foundAuthor.readCompositeIDValues(Publishers))?.id shouldBeEqualTo publisherA.id
            }

            /**
             * Preload optionalReferencedOn - child to single parent?
             *
             * ```sql
             * SELECT offices.zip_code,
             *        offices."name",
             *        offices.area_code,
             *        offices.staff,
             *        offices.publisher_id,
             *        offices.publisher_isbn
             *   FROM offices
             * ```
             *
             * ```sql
             * SELECT publishers.pub_id,
             *        publishers.isbn_code,
             *        publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.pub_id, publishers.isbn_code) = (1, 'd2090a50-3290-4026-88c1-1aa92bd775b0')
             * ```
             *
             */
            inTopLevelTransaction(Connection.TRANSACTION_READ_COMMITTED) {
                maxAttempts = 1

                // preload optionalReferencedOn - child to single parent?
                Office.all().with(Office::publisher)
                val foundOfficeA = Office.testCache(officeA.id)
                foundOfficeA.shouldNotBeNull()

                val foundOfficeB = Office.testCache(officeB.id)
                foundOfficeB.shouldNotBeNull()
                foundOfficeA.readValues[Offices.publisherId].shouldBeNull()
                foundOfficeA.readValues[Offices.publisherIsbn].shouldBeNull()
                Publisher.testCache(foundOfficeB.readCompositeIDValues(Publishers))?.id shouldBeEqualTo publisherA.id
            }
        }
    }

    /**
     * `backReferencedOn` 관계인 CompositeID 엔티티를 미리 로딩한다. (Eager Loading)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `preload backReferencedOn`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val publisherA = Publisher.new { name = "Publisher A" }
            val officeAIdValue = CompositeID {
                it[Offices.zipCode] = "1A2 3B4"
                it[Offices.name] = "Office A"
                it[Offices.areaCode] = 789
            }
            Office.new(officeAIdValue) { }
            val officeBIdValue = CompositeID {
                it[Offices.zipCode] = "5C6 7D8"
                it[Offices.name] = "Office B"
                it[Offices.areaCode] = 456
            }
            val officeB = Office.new(officeBIdValue) {
                publisher = publisherA
            }
            val bookA = Book.new { title = "Book A" }
            val reviewIdValue = CompositeID {
                it[Reviews.content] = "Not bad"
                it[Reviews.rank] = 12345L
            }
            val reviewA = Review.new(reviewIdValue) {
                book = bookA
            }

            commit()

            /**
             * Preload backReferencedOn - parent to single child (one-to-one) (Book -> Review)
             *
             * ```sql
             * SELECT books.book_id, books.title, books.author_id
             *   FROM books
             *  WHERE books.book_id = 1
             * ```
             * ```sql
             * SELECT reviews.code, reviews."rank", reviews.book_id
             *   FROM reviews
             *  WHERE reviews.book_id = (1)
             *
             * ```
             */
            inTopLevelTransaction(Connection.TRANSACTION_READ_COMMITTED) {
                maxAttempts = 1
                // preload backReferencedOn - parent to single child
                Book.find { Books.id eq bookA.id }.first().load(Book::review)

                val cache = TransactionManager.current().entityCache
                val result = cache.getReferrers<Review>(bookA.id, Reviews.book)?.map { it.id }.orEmpty()
                result shouldBeEqualTo listOf(reviewA.id)
            }

            /**
             * Preload optionalBackReferencedOn - parent to single child? (one-to-one) (Publisher -> Office?)
             *
             * ```
             * SELECT publishers.pub_id,
             *        publishers.isbn_code,
             *        publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.isbn_code = '24bbfafe-1de3-4b9e-9601-4955b9f0b360') AND (publishers.pub_id = 1)
             * ```
             * ```sql
             * SELECT offices.zip_code,
             *        offices."name",
             *        offices.area_code,
             *        offices.staff,
             *        offices.publisher_id,
             *        offices.publisher_isbn
             *   FROM offices
             *  WHERE (offices.publisher_id, offices.publisher_isbn) = (1, '24bbfafe-1de3-4b9e-9601-4955b9f0b360')
             * ```
             */
            inTopLevelTransaction(Connection.TRANSACTION_READ_COMMITTED) {
                maxAttempts = 1
                // preload optionalBackReferencedOn - parent to single child?
                Publisher.find { Publishers.id eq publisherA.id }.first().load(Publisher::office)

                val cache = TransactionManager.current().entityCache
                val result = cache.getReferrers<Office>(publisherA.id, Offices.publisherId)?.map { it.id }.orEmpty()
                result shouldBeEqualTo listOf(officeB.id)
            }
        }
    }

    /**
     * 참조하는 CompositeID 엔티티를 미리 로딩한다. (Eager Loading)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `preload referrersOn`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val publisherA = Publisher.new {
                name = "Publisher A"
            }
            val authorA = Author.new {
                publisher = publisherA
                penName = "Author A"
            }
            val authorB = Author.new {
                publisher = publisherA
                penName = "Author B"
            }
            val officeAIdValue = CompositeID {
                it[Offices.zipCode] = "1A2 3B4"
                it[Offices.name] = "Office A"
                it[Offices.areaCode] = 789
            }
            Office.new(officeAIdValue) {}
            val officeBIdValue = CompositeID {
                it[Offices.zipCode] = "5C6 7D8"
                it[Offices.name] = "Office B"
                it[Offices.areaCode] = 456
            }
            val officeB = Office.new(officeBIdValue) {
                publisher = publisherA
            }

            commit()
            entityCache.clear()

            /**
             * Preload referrersOn - parent to multiple children
             *
             * ```sql
             * SELECT publishers.pub_id,
             *        publishers.isbn_code,
             *        publishers.publisher_name
             *   FROM publishers
             *  WHERE (publishers.isbn_code = 'e36de68c-3425-4188-8f73-ae4b658d86c9') AND (publishers.pub_id = 1)
             * ```
             * ```sql
             * SELECT authors.id,
             *        authors.publisher_id,
             *        authors.publisher_isbn,
             *        authors.pen_name
             *   FROM authors
             *  WHERE (authors.publisher_id, authors.publisher_isbn) = (1, 'e36de68c-3425-4188-8f73-ae4b658d86c9')
             * ```
             */
            inTopLevelTransaction(Connection.TRANSACTION_SERIALIZABLE) {
                maxAttempts = 1
                // preload referrersOn - parent to multiple children
                val cache = TransactionManager.current().entityCache

                // Publisher 를 로드하고, 관련 Authors 도 함께 로드한다.
                Publisher.find { Publishers.id eq publisherA.id }.first().load(Publisher::authors)

                val result = cache.getReferrers<Author>(publisherA.id, Authors.publisherId)?.map { it.id }.orEmpty()
                result shouldContainSame listOf(authorA.id, authorB.id)
            }

            /**
             * Preload optionalReferrersOn - parent to multiple children?
             *
             * ```sql
             * SELECT publishers.pub_id, publishers.isbn_code, publishers.publisher_name
             *   FROM publishers
             * ```
             * ```sql
             * SELECT offices.zip_code,
             *        offices."name",
             *        offices.area_code,
             *        offices.staff,
             *        offices.publisher_id,
             *        offices.publisher_isbn
             *   FROM offices
             *  WHERE (offices.publisher_id, offices.publisher_isbn) = (1, 'e36de68c-3425-4188-8f73-ae4b658d86c9')
             * ```
             */
            inTopLevelTransaction(Connection.TRANSACTION_SERIALIZABLE) {
                maxAttempts = 1
                // preload optionalReferrersOn - parent to multiple children?
                val cache = TransactionManager.current().entityCache

                // Publisher 를 로드하고, 관련 Offices 도 함께 로드한다.
                Publisher.all().with(Publisher::allOffices)

                val result = cache.getReferrers<Office>(publisherA.id, Offices.publisherId)?.map { it.id }.orEmpty()
                result shouldBeEqualTo listOf(officeB.id)
            }
        }
    }

    /**
     * CompositeID를 가진 테이블에서 해당 `Entity`의 `EntityID<CompositeID>` 를 조회한다.
     */
    @Suppress("UNCHECKED_CAST")
    private fun Entity<*>.readCompositeIDValues(table: CompositeIdTable): EntityID<CompositeID> {
        val referenceColumn = this.klass.table.foreignKeys.single().references
        return EntityID(
            CompositeID {
                referenceColumn.forEach { (child, parent) ->
                    it[parent as Column<EntityID<Any>>] = this.readValues[child] as Any
                }
            },
            table
        )
    }
}
