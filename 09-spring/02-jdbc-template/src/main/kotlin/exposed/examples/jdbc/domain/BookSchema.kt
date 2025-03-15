package exposed.examples.jdbc.domain

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table

object BookSchema {

    /**
     * Authors Table
     *
     * ```sql
     * -- H2
     * CREATE TABLE IF NOT EXISTS AUTHORS (
     *      ID BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      DESCRIPTION TEXT NOT NULL
     * );
     * ```
     */
    object AuthorTable: LongIdTable("authors") {
        val name = varchar("name", 50)
        val description = text("description")
    }

    /**
     * Books Table
     *
     * ```sql
     * -- H2
     * CREATE TABLE IF NOT EXISTS BOOKS (
     *      ID BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      TITLE VARCHAR(255) NOT NULL,
     *      DESCRIPTION TEXT NOT NULL
     * )
     * ```
     */
    object BookTable: LongIdTable("books") {
        val title = varchar("title", 255)
        val description = text("description")
    }

    /**
     * Book - Author mapping table (many-to-many)
     *
     * ```sql
     * -- H2
     * CREATE TABLE IF NOT EXISTS BOOK_AUTHOR_MAP (
     *      BOOK_ID BIGINT NOT NULL,
     *      AUTHOR_ID BIGINT NOT NULL,
     *
     *      CONSTRAINT FK_BOOK_AUTHOR_MAP_BOOK__ID FOREIGN KEY (BOOK_ID)
     *          REFERENCES BOOKS(ID) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *      CONSTRAINT FK_BOOK_AUTHOR_MAP_AUTHOR__ID FOREIGN KEY (AUTHOR_ID)
     *          REFERENCES AUTHORS(ID) ON DELETE RESTRICT ON UPDATE RESTRICT)
     * ```
     */
    object BookAuthorTable: Table("book_author_map") {
        val bookId = reference("book_id", BookTable)
        val authorId = reference("author_id", AuthorTable)
    }

    class Author(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Author>(AuthorTable)

        var name by AuthorTable.name
        var description by AuthorTable.description

        val books: SizedIterable<Book> by Book via BookAuthorTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("description", description)
            .toString()
    }

    class Book(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Book>(BookTable)

        var title by BookTable.title
        var description by BookTable.description

        val authors: SizedIterable<Author> by Author via BookAuthorTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("title", title)
            .add("description", description)
            .toString()
    }
}
