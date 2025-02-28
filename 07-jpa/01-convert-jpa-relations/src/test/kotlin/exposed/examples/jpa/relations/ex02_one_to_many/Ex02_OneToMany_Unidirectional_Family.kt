package exposed.examples.jpa.relations.ex02_one_to_many

import exposed.examples.jpa.relations.ex02_one_to_many.FamilySchema.Child
import exposed.examples.jpa.relations.ex02_one_to_many.FamilySchema.ChildTable
import exposed.examples.jpa.relations.ex02_one_to_many.FamilySchema.Father
import exposed.examples.jpa.relations.ex02_one_to_many.FamilySchema.familyTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class Ex02_OneToMany_Unidirectional_Family: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many with ordering`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }  // collate 문제 (한글)

        withTables(testDB, *familyTables) {

            val father1 = Father.new {
                name = ("이성계")
            }

            val childId2 = ChildTable.insertAndGetId {
                it[name] = "이방원"
                it[birthday] = LocalDate.of(1390, 2, 10)
                it[father] = father1.id
            }
            val childId3 = ChildTable.insertAndGetId {
                it[name] = "이방석"
                it[birthday] = LocalDate.of(1400, 1, 21)
                it[father] = father1.id
            }
            val childId1 = ChildTable.insertAndGetId {
                it[name] = "이방번"
                it[birthday] = LocalDate.of(1380, 10, 5)
                it[father] = father1.id
            }

            flushCache()
            entityCache.clear()

            val loadedFather = Father.findById(father1.id)!!
            loadedFather shouldBeEqualTo father1

            // SELECT COUNT(*) FROM child WHERE child.father_id = 1
            loadedFather.children.count() shouldBeEqualTo 3

            /**
             * one-to-many with ordering
             *
             * ```sql
             * -- Postgres
             * SELECT child.id,
             *        child."name",
             *        child.birthday,
             *        child.father_id
             *   FROM child
             *  ORDER BY child.birthday ASC;
             *  ```
             */
            val expectedChildren = Child.all().orderBy(ChildTable.birthday to SortOrder.ASC).toList()

            /**
             * Lazy loading
             *
             * ```sql
             * -- Postgres
             * SELECT child.id,
             *        child."name",
             *        child.birthday,
             *        child.father_id
             *   FROM child
             *  WHERE child.father_id = 1
             *  ORDER BY child.birthday ASC,
             * ```
             */
            loadedFather.children.toList() shouldBeEqualTo expectedChildren
        }
    }
}
