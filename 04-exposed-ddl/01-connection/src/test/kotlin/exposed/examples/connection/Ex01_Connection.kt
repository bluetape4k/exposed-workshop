package exposed.examples.connection

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.vendors.ColumnMetadata
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Types

class Ex01_Connection: AbstractExposedTest() {

    companion object: KLoggingChannel()

    /**
     * ```sql
     * -- H2
     * CREATE TABLE IF NOT EXISTS PEOPLE (
     *      ID BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      FIRSTNAME VARCHAR(80) NULL,
     *      LASTNAME VARCHAR(42) DEFAULT 'Doe' NOT NULL,
     *      AGE INT DEFAULT 18 NOT NULL
     * );
     * ```
     */
    object People: LongIdTable() {
        val firstName = varchar("firstname", 80).nullable()
        val lastName = varchar("lastname", 42).default("Doe")
        val age = integer("age").default(18)
    }

    /**
     * 테이블 컬럼의 메타데이터를 가져온다.
     */
    @Test
    fun `getting column metadata`() {
        withTables(TestDB.H2, People) {
            val columnMetadata = connection.metadata {
                columns(People)[People].shouldNotBeNull()
            }.toSet()

            columnMetadata.forEach { cm: ColumnMetadata ->
                log.debug { "Column Meta: $cm" }
            }

            val idColumnMeta = if ((db.dialect as H2Dialect).isSecondVersion) {
                ColumnMetadata("ID", Types.BIGINT, "BIGINT", false, 64, null, true, null)
            } else {
                ColumnMetadata("ID", Types.BIGINT, "BIGINT", false, 19, null, true, null)
            }

            val expected = setOf(
                idColumnMeta,
                ColumnMetadata("FIRSTNAME", Types.VARCHAR, "VARCHAR(80)", true, 80, null, false, null),
                ColumnMetadata("LASTNAME", Types.VARCHAR, "VARCHAR(42)", false, 42, null, false, "Doe"),
                ColumnMetadata("AGE", Types.INTEGER, "INT", false, 32, null, false, "18"),
            )

            columnMetadata shouldContainSame expected
        }
    }

    /**
     * 테이블 제약조건을 가져온다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS parent (
     *      id BIGSERIAL PRIMARY KEY,
     *      "scale" INT NOT NULL
     * );
     *
     * ALTER TABLE parent ADD CONSTRAINT parent_scale_unique UNIQUE ("scale");
     *
     * CREATE TABLE IF NOT EXISTS child (
     *      id BIGSERIAL PRIMARY KEY,
     *      "scale" INT NOT NULL,
     *
     *      CONSTRAINT fk_child_scale__scale FOREIGN KEY ("scale") REFERENCES parent("scale")
     *      ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `table constraints`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        val parent = object: LongIdTable("parent") {
            val scale = integer("scale").uniqueIndex()
        }
        val child = object: LongIdTable("child") {
            val scale = reference("scale", parent.scale)
        }

        withTables(testDB, parent, child) {
            /**
             * child 테이블과 관련된 테이블의 제약조건 정보를 가져온다.
             *
             * ```
             * key: parent, constraint: []
             * key: child, constraint: [ForeignKeyConstraint(fkName='fk_child_scale__scale')]
             * ```
             */
            val constraints = connection.metadata {
                tableConstraints(listOf(child))
            }

            constraints.forEach { (key, constraint) ->
                log.debug { "key: $key, constraint: $constraint" }
            }
            constraints.keys shouldHaveSize 2   // parent, child
        }
    }
}
