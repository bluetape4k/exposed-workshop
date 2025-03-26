package exposed.examples.jpa.ex05_relations.ex01_one_to_one

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * one-to-one bidirectional 관계를 Exposed로 구현한 예제
 */
class Ex02_OneToOne_Bidirectional: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS husband (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      wife_id INT NULL,
     *
     *      CONSTRAINT fk_husband_wife_id__id FOREIGN KEY (wife_id)
     *      REFERENCES wife(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object HusbandTable: IntIdTable("husband") {
        val name = varchar("name", 255)
        val wifeId = optReference("wife_id", WifeTable)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS wife (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object WifeTable: IntIdTable("wife") {
        val name = varchar("name", 255)
    }

    class Husband(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Husband>(HusbandTable)

        var name by HusbandTable.name

        /**
         * one-to-one bidirectional (husband -> wife)
         */
        var wife by Wife optionalReferencedOn HusbandTable.wifeId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("name", name)
                .add("wife id", wife?.idValue)
                .toString()
    }

    class Wife(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Wife>(WifeTable)

        var name by WifeTable.name

        /**
         * one-to-one bidirectional (wife -> husband)
         */
        val husband by Husband optionalBackReferencedOn HusbandTable.wifeId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("name", name)
                .add("husband id", husband?.idValue)
                .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-one bidirectional`(testDB: TestDB) {
        withTables(testDB, HusbandTable, WifeTable) {
            val wife = Wife.new {
                name = "Alice"
            }
            val husband = Husband.new {
                this.name = "Tom"
                this.wife = wife
            }
            flushCache()
            entityCache.clear()

            // SELECT wife.id, wife."name" FROM wife WHERE wife.id = 1
            husband.wife shouldBeEqualTo wife
            // SELECT husband.id, husband."name", husband.wife_id FROM husband WHERE husband.wife_id = 1
            wife.husband shouldBeEqualTo husband

            entityCache.clear()

            val husband2 = Husband.findById(husband.id)!!
            husband2 shouldBeEqualTo husband
            husband2.wife shouldBeEqualTo wife

            // husband 를 삭제해도 wife 는 삭제되지 않는다. (cascade restrict 이다)
            val wife2 = husband2.wife!!
            husband2.delete()
            Wife.findById(wife2.id).shouldNotBeNull()

            wife2.delete()

            entityCache.clear()

            // SELECT COUNT(HUSBAND.ID) FROM HUSBAND
            Husband.count() shouldBeEqualTo 0

            // SELECT COUNT(WIFE.ID) FROM WIFE
            Wife.count() shouldBeEqualTo 0
        }
    }
}
