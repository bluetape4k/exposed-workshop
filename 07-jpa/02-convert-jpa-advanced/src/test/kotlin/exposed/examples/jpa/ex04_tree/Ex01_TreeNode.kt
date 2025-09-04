package exposed.examples.jpa.ex04_tree

import exposed.examples.jpa.ex04_tree.TreeNodeSchema.TreeNode
import exposed.examples.jpa.ex04_tree.TreeNodeSchema.TreeNodeTable
import exposed.examples.jpa.ex04_tree.TreeNodeSchema.buildTreeNodes
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * TREE 구조를 가지는 엔티티에 대해 Self Reference Table을 이용하여 구현한다.
 */
class Ex01_TreeNode: JdbcExposedTestBase() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `build tree nodes`(testDB: TestDB) {
        withTables(testDB, TreeNodeTable) {
            buildTreeNodes()

            val root = TreeNode.find { TreeNodeTable.title eq "root" }.single()
            val child1 = TreeNode.find { TreeNodeTable.title eq "child1" }.single()
            val child2 = TreeNode.find { TreeNodeTable.title eq "child2" }.single()
            val grandChild1 = TreeNode.find { TreeNodeTable.title eq "grandChild1" }.single()
            val grandChild2 = TreeNode.find { TreeNodeTable.title eq "grandChild2" }.single()

            flushCache()
            entityCache.clear()

            // SELECT tree_nodes.id, tree_nodes.title, tree_nodes.description, tree_nodes."depth", tree_nodes.parent_id FROM tree_nodes WHERE tree_nodes.id = 2
            val loadedChild1 = TreeNode.findById(child1.id)!!
            loadedChild1 shouldBeEqualTo child1

            // SELECT tree_nodes.id, tree_nodes.title, tree_nodes.description, tree_nodes."depth", tree_nodes.parent_id FROM tree_nodes WHERE tree_nodes.id = 1
            loadedChild1.parent shouldBeEqualTo root

            // SELECT COUNT(*) FROM tree_nodes WHERE tree_nodes.parent_id = 2
            loadedChild1.children.count() shouldBeEqualTo 2L
            loadedChild1.children.toSet() shouldContainSame setOf(grandChild1, grandChild2)

            // 모든 Root 노드 조회
            /**
             * ```sql
             * -- Postgres
             * SELECT tree_nodes.id, tree_nodes.title, tree_nodes.description, tree_nodes."depth", tree_nodes.parent_id
             *   FROM tree_nodes
             *  WHERE tree_nodes.parent_id IS NULL
             * ```
             */
            val roots = TreeNode.find { TreeNodeTable.parentId.isNull() }.toList()
            roots shouldBeEqualTo listOf(root)

            // child1 및 자손들을 모두 삭제
            /**
             * ```sql
             * -- Postgres
             * DELETE FROM tree_nodes WHERE tree_nodes.id = 5;
             * DELETE FROM tree_nodes WHERE tree_nodes.id = 2;
             * ```
             */
            child1.deleteDescendants()

            root.children.count() shouldBeEqualTo 1L
            TreeNode.findById(grandChild1.id).shouldBeNull()
            TreeNode.findById(grandChild2.id).shouldBeNull()

            // child1 은 삭제되었지만, child2 는 삭제되지 않았다.
            TreeNode.findById(child1.id).shouldBeNull()
            TreeNode.findById(child2.id).shouldNotBeNull()
        }
    }

    /**
     * [TreeNode]의 자식과의 Join 을 이용하여 부모와 자식노드를 구하기
     *
     * 부모 노드가 `child1` 인 노드와 그의 자식 노드들을 찾습니다.
     *
     * ```sql
     * SELECT parent.title,
     *        child.title
     *   FROM tree_nodes parent
     *      INNER JOIN tree_nodes child ON (parent.id = child.parent_id)
     *  WHERE parent.title = 'child1'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `join with children`(testDB: TestDB) {
        withTables(testDB, TreeNodeTable) {
            buildTreeNodes()

            val parent = TreeNodeTable.alias("parent")
            val child = TreeNodeTable.alias("child")

            val join = parent.innerJoin(child) { parent[TreeNodeTable.id] eq child[TreeNodeTable.parentId] }

            val titles = join
                .select(parent[TreeNodeTable.title], child[TreeNodeTable.title])
                .where { parent[TreeNodeTable.title] eq "child1" }
                .map { row ->
                    row[parent[TreeNodeTable.title]] to row[child[TreeNodeTable.title]]
                }

            titles shouldHaveSize 2
            titles.forEach {
                log.debug { "parent: ${it.first}, child: ${it.second}" }
                it.first shouldBeEqualTo "child1"
                it.second shouldStartWith "grand"
            }
        }
    }

    /**
     * SubQuery 를 이용하여 Covering Index 를 사용하여 조회하기
     *
     * `grand%` 로 시작하는 노드의 부모 노드를 찾습니다.
     *
     * ```sql
     * SELECT tree_nodes.id,
     *        tree_nodes.title,
     *        tree_nodes.description,
     *        tree_nodes."depth",
     *        tree_nodes.parent_id
     *   FROM tree_nodes
     *  WHERE tree_nodes.id IN (SELECT sub.parent_id
     *                            FROM tree_nodes sub
     *                           WHERE sub.title LIKE 'grand%')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `using subquery - inSubQuery`(testDB: TestDB) {
        withTables(testDB, TreeNodeTable) {
            buildTreeNodes()

            val sub = TreeNodeTable.alias("sub")

            val subQuery = sub
                .select(sub[TreeNodeTable.parentId])
                .where { sub[TreeNodeTable.title] like "grand%" }

            val query = TreeNodeTable.selectAll()
                .where { TreeNodeTable.id inSubQuery subQuery }

            val nodes = TreeNode.wrapRows(query).toList()
            nodes shouldHaveSize 1
            nodes.single().title shouldBeEqualTo "child1"
        }
    }

    /**
     * SubQuery 를 Convering 인덱스로 활용하기
     *
     * `child%` 로 시작하는 노드를 부모로 가진 노드 조회하기
     *
     * ```sql
     * -- Postgres
     * SELECT tree_nodes.id,
     *        tree_nodes.title,
     *        tree_nodes.description,
     *        tree_nodes."depth",
     *        tree_nodes.parent_id
     *   FROM tree_nodes
     *      INNER JOIN (SELECT tree_nodes.id
     *                    FROM tree_nodes
     *                   WHERE tree_nodes.title LIKE 'child%') sub
     *              ON (tree_nodes.parent_id = sub.id);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `using join subquery`(testDB: TestDB) {
        withTables(testDB, TreeNodeTable) {
            buildTreeNodes()

            val sub = TreeNodeTable
                .select(TreeNodeTable.id)
                .where { TreeNodeTable.title like "child%" }
                .alias("sub")

            val joinQuery = TreeNodeTable.innerJoin(sub) { TreeNodeTable.parentId eq sub[TreeNodeTable.id] }
                .select(TreeNodeTable.columns)

            // Query 결과인 ResultSet 으로 Entity 만들기
            val nodes = TreeNode.wrapRows(joinQuery).toList()
            nodes shouldHaveSize 2
            nodes.map { it.title } shouldContainSame listOf("grandChild1", "grandChild2")
        }
    }
}
