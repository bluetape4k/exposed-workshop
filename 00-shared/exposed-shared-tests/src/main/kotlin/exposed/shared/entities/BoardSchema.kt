package exposed.shared.entities

import io.bluetape4k.exposed.core.timebasedGenerated
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * 게시판(Board), 게시글(Post), 카테고리(Category) 엔티티와 테이블을 포함하는 스키마 정의 클래스.
 *
 * DAO 패턴을 사용하여 게시판과 게시글 간의 계층적 관계 및 카테고리 연관 관계를 나타냅니다.
 */
class BoardSchema {

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS board (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ALTER TABLE board ADD CONSTRAINT board_name_unique UNIQUE ("name");
     * ```
     */
    object Boards: IntIdTable("board") {
        val name = varchar("name", 255).uniqueIndex()
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS posts (
     *      id BIGSERIAL PRIMARY KEY,
     *      board INT NULL,
     *      parent BIGINT NULL,
     *      category VARCHAR(22) NULL,
     *      "optCategory" VARCHAR(22) NULL
     * );
     *
     * ALTER TABLE posts
     *      ADD CONSTRAINT posts_category_unique UNIQUE (category);
     *
     * ALTER TABLE posts
     *      ADD CONSTRAINT fk_posts_board__id FOREIGN KEY (board) REFERENCES board(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT;
     * ALTER TABLE posts
     *      ADD CONSTRAINT fk_posts_parent__id FOREIGN KEY (parent) REFERENCES posts(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT;
     * ALTER TABLE posts
     *      ADD CONSTRAINT fk_posts_category__uniqueid FOREIGN KEY (category) REFERENCES categories("uniqueId")
     *          ON DELETE RESTRICT ON UPDATE RESTRICT;
     * ALTER TABLE posts
     *      ADD CONSTRAINT fk_posts_optcategory__uniqueid FOREIGN KEY ("optCategory") REFERENCES categories("uniqueId")
     *          ON DELETE RESTRICT ON UPDATE RESTRICT;
     * ```
     */
    object Posts: LongIdTable("posts") {
        val boardId = optReference("board_id", Boards.id)
        val parentId = optReference("parent_id", this)
        val categoryId = optReference("category_uniqueId", Categories.uniqueId).uniqueIndex()
        val optCategoryId = optReference("optCategory_uniqueId", Categories.uniqueId)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS categories (
     *      id SERIAL PRIMARY KEY,
     *      "uniqueId" VARCHAR(22) NOT NULL,
     *      title VARCHAR(50) NOT NULL
     * );
     *
     * ALTER TABLE categories
     *      ADD CONSTRAINT categories_uniqueid_unique UNIQUE ("uniqueId");
     * ```
     */
    object Categories: IntIdTable("categories") {
        val uniqueId = varchar("uniqueId", 22).timebasedGenerated().uniqueIndex()
        val title = varchar("title", 50)
    }


    /** 게시판 엔티티. [Boards] 테이블과 매핑됩니다. */
    class Board(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Board>(Boards)

        var name by Boards.name
        val posts by Post optionalReferrersOn Posts.boardId  // one-to-many

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .toString()
    }

    /** 게시글 엔티티. [Posts] 테이블과 매핑됩니다. */
    class Post(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Post>(Posts)

        var board by Board optionalReferencedOn Posts.boardId     // many-to-one
        var parent by Post optionalReferencedOn Posts.parentId     // many-to-one
        val children by Post optionalReferrersOn Posts.parentId   // one-to-many
        var category: Category? by Category optionalReferencedOn Posts.categoryId   // many-to-one
        var optCategory: Category? by Category optionalReferencedOn Posts.optCategoryId  // many-to-one

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("board id", board?.id)
            .add("parent id", parent?.id)
            .toString()
    }

    /** 카테고리 엔티티. [Categories] 테이블과 매핑됩니다. */
    class Category(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Category>(Categories)

        val uniqueId by Categories.uniqueId
        var title by Categories.title
        val posts by Post optionalReferrersOn Posts.optCategoryId  // one-to-many

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("uniqueId", uniqueId)
            .add("title", title)
            .toString()
    }
}
