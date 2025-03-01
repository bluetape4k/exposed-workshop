package exposed.examples.jpa.ex01_relations.ex01_one_to_one

import exposed.examples.jpa.ex01_relations.ex01_one_to_one.Ex01_OneToOne_Unidirectional.Cavalier
import exposed.examples.jpa.ex01_relations.ex01_one_to_one.Ex01_OneToOne_Unidirectional.Horse
import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

/**
 * one-to-one unidirectional 관계를 Exposed로 구현한 예제
 *
 * [Cavalier] 와 [Horse] 가 1:1 관계를 맺고, [Cavalier] 가 [Horse] 를 소유하는 경우
 */
class Ex01_OneToOne_Unidirectional: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS cavalier (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      horse_id INT NULL,
     *
     *      CONSTRAINT fk_cavalier_horse_id__id FOREIGN KEY (horse_id)
     *      REFERENCES horse(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Cavaliers: IntIdTable("cavalier") {
        val name = varchar("name", 255)
        val horseId = optReference("horse_id", Horses)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS horse (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object Horses: IntIdTable("horse") {
        val name = varchar("name", 255)
    }

    class Cavalier(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Cavalier>(Cavaliers)

        var name by Cavaliers.name
        var horse by Horse optionalReferencedOn Cavaliers.horseId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Horse(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Horse>(Horses)

        var name by Horses.name

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `unidirectional one-to-one, cavalier has ownership`(testDB: TestDB) {
        withTables(testDB, Cavaliers, Horses) {
            val horse = Horse.new {
                name = "White Horse"
            }
            val cavalier = Cavalier.new {
                name = "Clint Eastwood"
                this.horse = horse
            }

            val loaded = Cavalier.findById(cavalier.id)!!
            loaded shouldBeEqualTo cavalier
            loaded.horse shouldBeEqualTo horse

            val horse2 = cavalier.horse!!
            horse2 shouldBeEqualTo horse

            // 현재로는 horse는 cavalier가 소유하므로, 삭제할 수 없다.
            // `onDelete` 를 ReferenceOption.SET_NULL, ReferenceOption.CASCADE 를 사용하면 삭제할 수 있다.
            if (testDB !in TestDB.ALL_POSTGRES) {
                assertFailsWith<ExposedSQLException> {
                    horse.delete()
                }
            }
            cavalier.delete()
            // SELECT COUNT(cavalier.id) FROM cavalier
            Cavalier.count() shouldBeEqualTo 0L

            // cavalier가 삭제되었으므로, horse를 삭제할 수 있다.
            Horse.count() shouldBeEqualTo 1L

            horse.delete()
            // SELECT COUNT(horse.id) FROM horse
            Horse.count() shouldBeEqualTo 0L
        }
    }
}
