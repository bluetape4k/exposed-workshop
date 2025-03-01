package exposed.examples.jpa.ex02_entities

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.javatime.date

object BlogSchema {

    val blogTables = arrayOf(
        PostTable, PostDetailsTable, PostCommentTable, PostTagTable, TagTable
    )

    /**
     * ```sql
     * -- Postgres:
     * CREATE TABLE IF NOT EXISTS posts (
     *      id BIGSERIAL PRIMARY KEY,
     *      title VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object PostTable: LongIdTable("posts") {
        val title = varchar("title", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS post_details (
     *      id BIGINT NOT NULL,
     *      created_on DATE NOT NULL,
     *      created_by VARCHAR(255) NOT NULL,
     *
     *      CONSTRAINT fk_post_details_id__id FOREIGN KEY (id)
     *      REFERENCES posts(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object PostDetailsTable: IdTable<Long>("post_details") {
        override val id: Column<EntityID<Long>> = reference("id", PostTable)   // one-to-one relationship
        val createdOn = date("created_on")  // .defaultExpression(CurrentDate)
        val createdBy = varchar("created_by", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS post_comments (
     *      id BIGSERIAL PRIMARY KEY,
     *      post_id BIGINT NOT NULL,
     *      review VARCHAR(255) NOT NULL,
     *
     *      CONSTRAINT fk_post_comments_post_id__id FOREIGN KEY (post_id)
     *      REFERENCES posts(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * CREATE INDEX post_comments_post_id ON post_comments (post_id)
     * ```
     */
    object PostCommentTable: LongIdTable("post_comments") {
        val postId = reference("post_id", PostTable).index()
        val review = varchar("review", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS tags (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ```
     */
    object TagTable: LongIdTable("tags") {
        val name = varchar("name", 255)
    }

    /**
     * Many-to-many relationship table
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS post_tags (
     *      id BIGSERIAL PRIMARY KEY,
     *      post_id BIGINT NOT NULL,
     *      tag_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_post_tags_post_id__id FOREIGN KEY (post_id)
     *      REFERENCES posts(id) ON DELETE CASCADE ON UPDATE RESTRICT,
     *      CONSTRAINT fk_post_tags_tag_id__id FOREIGN KEY (tag_id)
     *      REFERENCES tags(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE post_tags
     *      ADD CONSTRAINT post_tags_post_id_tag_id_unique UNIQUE (post_id, tag_id);
     * ```
     */
    object PostTagTable: LongIdTable("post_tags") {
        val postId = reference("post_id", PostTable, onDelete = ReferenceOption.CASCADE)
        val tagId = reference("tag_id", TagTable, onDelete = ReferenceOption.CASCADE)

        init {
            uniqueIndex(postId, tagId)
        }
    }


    class Post(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Post>(PostTable)

        var title by PostTable.title

        val details: PostDetails by PostDetails backReferencedOn PostDetailsTable.id  // one-to-one relationship
        val comments: SizedIterable<PostComment> by PostComment referrersOn PostCommentTable.postId
        val tags: SizedIterable<Tag> by Tag via PostTagTable // Tag.via (PostTagTable.post, PostTagTable.tag)

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("title", title)
                .toString()
    }

    class PostDetails(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<PostDetails>(PostDetailsTable)

        val post: Post by Post referencedOn PostDetailsTable.id   // one-to-one relationship
        var createdOn by PostDetailsTable.createdOn
        var createdBy by PostDetailsTable.createdBy

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("post id", post.idValue)
                .add("createdOn", createdOn)
                .add("createdBy", createdBy)
                .toString()
    }

    class PostComment(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<PostComment>(PostCommentTable)

        // one-to-many relationship
        var post by Post referencedOn PostCommentTable.postId
        var review by PostCommentTable.review

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("post id", post.idValue)
                .add("review", review)
                .toString()
    }

    class Tag(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Tag>(TagTable)

        var name by TagTable.name
        val posts: SizedIterable<Post> by Post via PostTagTable // Post.via(PostTagTable.tag, PostTagTable.post) 와 같다.

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("name", name)
                .toString()
    }

}
