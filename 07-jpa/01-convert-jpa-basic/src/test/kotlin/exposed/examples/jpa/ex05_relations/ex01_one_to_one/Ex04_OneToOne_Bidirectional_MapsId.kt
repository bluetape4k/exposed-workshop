package exposed.examples.jpa.ex05_relations.ex01_one_to_one

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * JPA @MapsId 형태의 bidirectional one-to-one 관계를 Exposed로 구현한 예제
 */
class Ex04_OneToOne_Bidirectional_MapsId: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS authors (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object Authors: IntIdTable("authors") {
        val name: Column<String> = varchar("name", 255)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS pictures (
     *      author_id INT PRIMARY KEY,
     *      "path" VARCHAR(255) NOT NULL,
     *
     *      CONSTRAINT fk_pictures_author_id__id FOREIGN KEY (author_id)
     *      REFERENCES authors(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     * ```
     */
    object Pictures: IdTable<Int>("pictures") {
        // @MapsId 와 같다. (Authors.id 를 Id로 사용한다)
        override val id: Column<EntityID<Int>> = reference(
            "author_id",
            Authors,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.CASCADE
        )
        val path: Column<String> = varchar("path", 255)

        override val primaryKey = PrimaryKey(Pictures.id)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS biographys (
     *      author_id INT PRIMARY KEY,
     *      information VARCHAR(255) NULL,
     *
     *      CONSTRAINT fk_biographys_author_id__id FOREIGN KEY (author_id)
     *      REFERENCES authors(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     * ```
     */
    object Biographys: IdTable<Int>("biographys") {
        // @MapsId 와 같다. (Authors.id 를 Id로 사용한다)
        override val id: Column<EntityID<Int>> = reference(
            "author_id",
            Authors,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.CASCADE
        )
        val infomation: Column<String?> = varchar("information", 255).nullable()

        override val primaryKey = PrimaryKey(Biographys.id)
    }

    private val allTables = arrayOf(Authors, Pictures, Biographys)

    class Author(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Author>(Authors)

        var name by Authors.name
        val picture by Picture backReferencedOn Pictures
        val biography by Biography backReferencedOn Biographys

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Picture(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Picture>(Pictures)

        var path by Pictures.path
        val author by Author referencedOn Pictures.id

        // NOTE: one-to-one 관계에서 id 값을 참조하는 경우 `id` 로 비교하면 안되고, `idValue` 로 비교해야 한다.
        // NOTE: id 속성 중 table 이 one-to-one 의 owner 테이블을 가르킨다.
        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("path", path)
            .add("author id", author.idValue)
            .toString()
    }

    class Biography(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Biography>(Biographys)

        var infomation by Biographys.infomation
        val author by Author referencedOn Biographys.id

        // NOTE: one-to-one 관계에서 id 값을 참조하는 경우 `id` 로 비교하면 안되고, `idValue` 로 비교해야 한다.
        // NOTE: id 속성 중 table 이 one-to-one 의 owner 테이블을 가르킨다.
        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("infomation", infomation)
                .add("author id", author.idValue)
                .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-one share main entity identifier`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val author = Author.new {
                name = faker.name().name()
            }
            val picture = Picture.new(author.id.value) {
                path = faker.internet().url()
            }
            val biography = Biography.new(author.id.value) {
                infomation = faker.name().fullName()
            }

            entityCache.clear()

            val author2 = Author.findById(author.id.value)!!
            author2 shouldBeEqualTo author

            val biography2 = Biography.findById(biography.id.value)!!
            biography2 shouldBeEqualTo biography
            biography2.author shouldBeEqualTo author

            val picture2 = Picture.findById(author.id.value)!!
            picture2 shouldBeEqualTo picture
            picture2.author shouldBeEqualTo author

            entityCache.clear()

            /**
             * `load`를 사용해 1:1 관계의 엔티티들을 eager loading
             *
             * ```sql
             * SELECT AUTHORS.ID, AUTHORS."name"
             *   FROM AUTHORS
             *  WHERE AUTHORS.ID = 1;
             *
             * SELECT BIOGRAPHYS.ID, BIOGRAPHYS.INFORMATION
             *   FROM BIOGRAPHYS
             *  WHERE BIOGRAPHYS.ID = 1;
             *
             * SELECT PICTURES.ID, PICTURES."path"
             *   FROM PICTURES
             *  WHERE PICTURES.ID = 1;
             * ```
             */
            val author3 = Author.findById(author.id)!!.load(Author::picture, Author::biography)

            entityCache.clear()

            // Load by join
            val authors = Authors
                .innerJoin(Pictures)
                .innerJoin(Biographys)
                .selectAll()
                .where { Authors.id eq author.id }
                .map { Author.wrapRow(it) }

            authors.forEach {
                log.debug { it }
            }
            authors shouldHaveSize 1
            val author4 = authors.first()
            author4 shouldBeEqualTo author
            author4.picture shouldBeEqualTo picture
            author4.biography shouldBeEqualTo biography

            // cascade delete (author -> biography, picture)
            author.delete()
            entityCache.clear()

            // SELECT COUNT(AUTHORS.ID) FROM AUTHORS
            Author.count() shouldBeEqualTo 0L
            // SELECT COUNT(*) FROM AUTHORS
            Author.all().count() shouldBeEqualTo 0L

            /**
             * Pictures, Biographys 는 author 가 삭제되면 같이 삭제된다.
             *
             * `Picture.count()` 는 자신만의 entity id 가 없으므로 실행할 수 없다. (SELECT COUNT(AUTHOR_ID) FROM PICTURES)
             *
             * ```sql
             * SELECT COUNT(*) FROM PICTURES
             * SELECT COUNT(*) FROM BIOGRAPHYS
             */
            Picture.all().count() shouldBeEqualTo 0L
            Biography.all().count() shouldBeEqualTo 0L
        }
    }
}
