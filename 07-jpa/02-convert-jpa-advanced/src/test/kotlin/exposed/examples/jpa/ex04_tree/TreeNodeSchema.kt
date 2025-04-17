package exposed.examples.jpa.ex04_tree

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Transaction

object TreeNodeSchema {

    /**
     * 트리 노드 테이블
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS TREE_NODES (
     *      ID BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      TITLE VARCHAR(255) NOT NULL,
     *      DESCRIPTION TEXT NULL,
     *      DEPTH INT DEFAULT 0,
     *      PARENT_ID BIGINT NULL
     * )
     * ```
     */
    object TreeNodeTable: LongIdTable("tree_nodes") {
        val title = varchar("title", 255)
        val description = text("description").nullable()
        val depth = integer("depth").default(0)

        val parentId = optReference("parent_id", TreeNodeTable)
    }

    class TreeNode(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<TreeNode>(TreeNodeTable) {
            override fun new(init: TreeNode.() -> Unit): TreeNode {
                val node = super.new { }
                node.init()
                node.depth = (node.parent?.depth ?: 0) + 1
                return node
            }
        }

        var title by TreeNodeTable.title
        var description by TreeNodeTable.description
        var depth by TreeNodeTable.depth
        var parent: TreeNode? by TreeNode optionalReferencedOn TreeNodeTable.parentId

        // 자식 노드 조회
        val children: SizedIterable<TreeNode>
            get() = TreeNode.find { TreeNodeTable.parentId eq id }

        /**
         * DFS 방식으로 자신뿐 아니라 모든 자손까지 삭제한다.
         */
        fun deleteDescendants() {
            children.forEach { it.deleteDescendants() }
            delete()
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("title", title)
            .add("description", description)
            .add("depth", depth)
            .add("parent id", parent?.idValue)
            .toString()
    }

    internal fun Transaction.buildTreeNodes() {
        val root = TreeNode.new { title = "root" }
        commit()

        val child1 = TreeNode.new { title = "child1"; parent = root }
        val child2 = TreeNode.new { title = "child2"; parent = root }
        commit()

        val grandChild1 = TreeNode.new { title = "grandChild1"; parent = child1; }
        val grandChild2 = TreeNode.new { title = "grandChild2"; parent = child1; }

        flushCache()
    }

}
