package exposed.examples.ddl

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.nextIntVal
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex06_Sequence: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * SEQUENCE 정의하기
     *
     * ```kotlin
     * SchemaUtils.createSequence(myseq)
     * ```
     * ```sql
     * -- Postgres
     * CREATE SEQUENCE IF NOT EXISTS my_sequence
     *      START WITH 4 INCREMENT BY 2 MINVALUE 1 MAXVALUE 100 CYCLE CACHE 20
     * ```
     */
    private val myseq = org.jetbrains.exposed.v1.core.Sequence(
        name = "my_sequence",
        startWith = 4,
        incrementBy = 2,
        minValue = 1,
        maxValue = 100,
        cycle = true,
        cache = 20
    )

    /**
     * 시퀀스 생성하기
     *
     * ```sql
     * -- Postgres
     * CREATE SEQUENCE IF NOT EXISTS my_sequence
     *      START WITH 4
     *      INCREMENT BY 2
     *      MINVALUE 1
     *      MAXVALUE 100
     *      CYCLE
     *      CACHE 20
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `시퀀스 생성하기`(testDB: TestDB) {
        withDb(testDB) {
            Assumptions.assumeTrue { currentDialect.supportsCreateSequence }

            log.info { "myseq: ${myseq.ddl.single()}" }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS developer (
     *      id INT, "name" VARCHAR(255),
     *      CONSTRAINT pk_developer PRIMARY KEY (id, "name")
     * );
     * ```
     */
    private object Developer: Table("developer") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id, name)
    }

    /**
     * 시퀀스를 사용하여 컬럼 값 지정하기
     *
     * ```sql
     * -- Postgres
     * INSERT INTO developer (id, "name") VALUES (NEXTVAL('my_sequence'), 'John Doe');
     * INSERT INTO developer (id, "name") VALUES (NEXTVAL('my_sequence'), 'Jane Doe');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Sequence를 컬럼 값으로 지정하기`(testDB: TestDB) {
        withTables(testDB, Developer) {
            Assumptions.assumeTrue { currentDialect.supportsSequenceAsGeneratedKeys }

            try {
                // 시퀀스 생성
                SchemaUtils.createSequence(myseq)

                val developerId = Developer.insert {
                    it[id] = myseq.nextIntVal()
                    it[name] = "John Doe"
                }[Developer.id]

                log.info { "developerId: $developerId" }
                developerId shouldBeEqualTo myseq.startWith?.toInt()

                val developerId2 = Developer.insert {
                    it[id] = myseq.nextIntVal()
                    it[name] = "Jane Doe"
                }[Developer.id]

                log.info { "developerId2: $developerId2" }
                developerId2 shouldBeEqualTo myseq.startWith!!.toInt() + myseq.incrementBy!!.toInt()
            } finally {
                SchemaUtils.dropSequence(myseq)
            }
        }
    }

    /**
     * `autoIncrement` 에 custom sequence 지정하기
     *
     * ```sql
     * -- Postgres
     * CREATE SEQUENCE IF NOT EXISTS my_sequence
     *      START WITH 4 INCREMENT BY 2 MINVALUE 1 MAXVALUE 100 CYCLE CACHE 20;
     *
     * CREATE TABLE IF NOT EXISTS tester (
     *      id INT PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     *
     * INSERT INTO tester ("name", id) VALUES ('John Doe', NEXTVAL('my_sequence'));
     * INSERT INTO tester ("name", id) VALUES ('Jane Doe', NEXTVAL('my_sequence'));
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `autoIncrement 에 custom sequence 지정`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val id = integer("id").autoIncrement(myseq)
            val name = varchar("name", 255)

            override val primaryKey = PrimaryKey(id)
        }
        withDb(testDB) {
            Assumptions.assumeTrue { currentDialect.supportsSequenceAsGeneratedKeys }

            try {
                SchemaUtils.create(tester)
                myseq.exists().shouldBeTrue()
                tester.exists().shouldBeTrue()

                val id = tester.insert {
                    it[name] = "John Doe"
                }[tester.id]
                id.toLong() shouldBeEqualTo myseq.startWith!!


                val id2 = tester.insert {
                    it[name] = "Jane Doe"
                }[tester.id]

                id2.toLong() shouldBeEqualTo myseq.startWith!! + myseq.incrementBy!!
            } finally {
                SchemaUtils.drop(tester)
            }
        }
    }
}
