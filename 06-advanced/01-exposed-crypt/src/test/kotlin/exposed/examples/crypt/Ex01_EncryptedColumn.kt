package exposed.examples.crypt

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import nl.altindag.log.LogCaptor
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainNone
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldStartWith
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exposedLogger
import org.jetbrains.exposed.v1.crypt.Algorithms
import org.jetbrains.exposed.v1.crypt.Encryptor
import org.jetbrains.exposed.v1.crypt.encryptedBinary
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex01_EncryptedColumn: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * Exposed 에서 제공하는 Encryptor 로 암호화할 때, 컬럼의 최대 길이를 계산할 수 있다.
     *
     * [Encryptor.maxColLength] 를 사용하면 된다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `output length of encryption`(testDB: TestDB) {
        fun assertSize(encryptor: Encryptor, str: String) {
            encryptor.encrypt(str).toUtf8Bytes().size shouldBeEqualTo encryptor.maxColLength(str.toUtf8Bytes().size)
        }

        val encryptors = arrayOf(
            "AES_256_PBE_GCM" to Algorithms.AES_256_PBE_GCM("passwd", "12345678"),
            "AES_256_PBE_CBC" to Algorithms.AES_256_PBE_CBC("passwd", "12345678"),
            "BLOW_FISH" to Algorithms.BLOW_FISH("sadsad"),
            "TRIPLE_DES" to Algorithms.TRIPLE_DES("1".repeat(24))
        )
        val testString = arrayOf(
            "1",
            "2".repeat(10),
            "3".repeat(31),
            "4".repeat(1001),
            "5".repeat(5391)
        )

        encryptors.forEach { (algorithm, encryptor) ->
            testString.forEach { testStr ->
                log.debug { "Testing $algorithm, str length=${testStr.length}" }
                assertSize(encryptor, testStr)
            }
        }
    }

    /**
     * 암호화된 컬럼에 문자열을 저장하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `encrypted column type with a string`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS stringtable (
         *      id SERIAL PRIMARY KEY,
         *      "name" VARCHAR(80) NOT NULL,        -- AES_256_PBE_CBC
         *      city VARCHAR(80) NOT NULL,          -- AES_256_PBE_GCM
         *      address VARCHAR(100) NOT NULL,      -- BLOW_FISH
         *      age VARCHAR(100) NOT NULL           -- TRIPLE_DES
         * )
         * ```
         */
        val nameEncryptor = Algorithms.AES_256_PBE_CBC("passwd", "5c0744940b5c369b")
        val stringTable = object: IntIdTable("StringTable") {
            val name: Column<String> = encryptedVarchar("name", 80, nameEncryptor)
            val city: Column<String> =
                encryptedVarchar("city", 80, Algorithms.AES_256_PBE_GCM("passwd", "5c0744940b5c369b"))
            val address: Column<String> = encryptedVarchar("address", 100, Algorithms.BLOW_FISH("key"))
            val age: Column<String> = encryptedVarchar("age", 100, Algorithms.TRIPLE_DES("1".repeat(24)))
        }

        withTables(testDB, stringTable) {
            val logCaptor = LogCaptor.forName(exposedLogger.name)
            logCaptor.setLogLevelToDebug()

            /**
             * ```sql
             * -- Base64 encoded string
             * INSERT INTO stringtable ("name", city, address, age)
             * VALUES (Ny/Xkij4SFLuCTT2fWZpVh3s0LTIfyKC59JPZPSOdu8=,
             *         R+NiRhvJ60s+AFqK6+ULu5HdQY0+/ZQDYQYM/9CgKiz2zpixFAR5ng==,
             *         w6iiV7UMvhzfCyERcyae5w==,
             *         czkyHR39GE5uH1JBNe01ow==
             * )
             * ```
             */
            val insertedStrings = listOf("testName", "testCity", "testAddress", "testAge")
            val (insertedName, insertedCity, insertedAddress, insertedAge) = insertedStrings
            val id1 = stringTable.insertAndGetId {
                it[name] = insertedName
                it[city] = insertedCity
                it[address] = insertedAddress
                it[age] = insertedAge
            }

            val insertLog = logCaptor.debugLogs.single()
            insertLog.shouldStartWith("INSERT ")
            insertLog.shouldContainNone(insertedStrings)

            logCaptor.clearLogs()
            logCaptor.resetLogLevel()
            logCaptor.close()

            stringTable.selectAll().count() shouldBeEqualTo 1L

            val row = stringTable.selectAll().where { stringTable.id eq id1 }.single()

            row[stringTable.name] shouldBeEqualTo insertedName
            row[stringTable.city] shouldBeEqualTo insertedCity
            row[stringTable.address] shouldBeEqualTo insertedAddress
            row[stringTable.age] shouldBeEqualTo insertedAge

            /**
             * NOTE: 암호화된 컬럼으로 검색은 불가능하다. --> 매번 암호화 할 때마다 다른 결과를 가지게 한다. 물론 복호화는 잘된다.
             * NOTE: 이런 방식은 암호화 컬럼은 인덱스로 사용할 수 없다는 뜻이다.
             * HINT: jasypt 를 활용해 암호화 시 항상 같은 결과를 내도록 해서 검색이 가능하도록 할 수 있다.
             */
            assertFailsWith<AssertionError> {
                stringTable.selectAll()
                    .where { stringTable.name eq nameEncryptor.encrypt(insertedName) }
                    .shouldNotBeEmpty()
            }
        }
    }

    /**
     * 암호화된 컬럼의 타입의 값 갱신하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update encrypted column type`(testDB: TestDB) {

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS stringtable (
         *      id SERIAL PRIMARY KEY,
         *      "name" VARCHAR(100) NOT NULL,       -- AES_256_PBE_GCM
         *      city bytea NOT NULL,                -- AES_256_PBE_CBC
         *      address VARCHAR(100) NOT NULL       -- BLOW_FISH
         * )
         * ```
         */
        val stringTable = object: IntIdTable("StringTable") {
            val name: Column<String> = encryptedVarchar("name", 100, Algorithms.AES_256_PBE_GCM("passwd", "12345678"))
            val city: Column<ByteArray> = encryptedBinary("city", 100, Algorithms.AES_256_PBE_CBC("passwd", "12345678"))
            val address: Column<String> = encryptedVarchar("address", 100, Algorithms.BLOW_FISH("key"))
        }

        withTables(testDB, stringTable) {
            val logCaptor = LogCaptor.forName(exposedLogger.name)
            logCaptor.setLogLevelToDebug()

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO stringtable ("name", city, address)
             * VALUES (GLYN2dtSPlEklEDqu2WuXdsOtQBLSUZ+5QgW8AdrHvfVj5JBQSQT5Q==,
             *        [B@677349fb,
             *        NCoXob9KL2ffCyERcyae5w==
             * )
             * ```
             */
            val insertedStrings = listOf("TestName", "TestCity", "TestAddress")
            val (insertedName, insertedCity, insertedAddress) = insertedStrings
            val id = stringTable.insertAndGetId {
                it[name] = insertedName
                it[city] = insertedCity.toUtf8Bytes()
                it[address] = insertedAddress
            }

            val insertLog = logCaptor.debugLogs.single()
            insertLog.shouldStartWith("INSERT ")
            insertLog.shouldContainNone(insertedStrings)

            logCaptor.clearLogs()

            /**
             * ```sql
             * -- Postgres
             * UPDATE stringtable
             *    SET "name"=V8kN75IkkYqYAejR/Xz4Vs7hakXQGRrVL7vcCzTRku8dgwfqR5Ft+tE=,
             *        city=[B@4466cf5d,
             *        address=NCoXob9KL2cVJbWS6U+KeQ==
             *  WHERE stringtable.id = 1
             * ```
             */
            val updatedStrings = listOf("TestName2", "TestCity2", "TestAddress2")
            val (updatedName, updatedCity, updatedAddress) = updatedStrings
            stringTable.update({ stringTable.id eq id }) {
                it[name] = updatedName
                it[city] = updatedCity.toUtf8Bytes()
                it[address] = updatedAddress
            }

            val updateLog = logCaptor.debugLogs.single()
            updateLog.shouldStartWith("UPDATE ")
            updateLog.shouldContainNone(updatedStrings)

            logCaptor.clearLogs()
            logCaptor.resetLogLevel()
            logCaptor.close()

            stringTable.selectAll().count() shouldBeEqualTo 1L

            val row = stringTable.selectAll()
                .where { stringTable.id eq id }
                .first()

            row[stringTable.name] shouldBeEqualTo updatedName
            row[stringTable.city].toUtf8String() shouldBeEqualTo updatedCity
            row[stringTable.address] shouldBeEqualTo updatedAddress
        }
    }
}
