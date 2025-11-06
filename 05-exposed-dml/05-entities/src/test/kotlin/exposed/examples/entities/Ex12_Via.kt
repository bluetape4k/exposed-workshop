package exposed.examples.entities

import exposed.examples.entities.ViaTestData.ConnectionAutoTable
import exposed.examples.entities.ViaTestData.ConnectionTable
import exposed.examples.entities.ViaTestData.IConnectionTable
import exposed.examples.entities.ViaTestData.VNumber
import exposed.examples.entities.ViaTestData.VString
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.dao.InnerTableLink
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.util.*
import kotlin.reflect.jvm.isAccessible

/**
 * [ViaTestData]는 Exposed를 사용하여 다대다 관계를 테스트하기 위한 데이터 모델을 정의합니다.
 * 이 모델은 두 개의 엔티티(NumbersTable, StringsTable)와 이를 연결하는 두 개의 관계 테이블(ConnectionTable, ConnectionAutoTable)을 포함합니다.
 */
object ViaTestData {

    /**
     * NumbersTable은 UUID를 기본 키로 가지며, 정수를 저장하는 테이블입니다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS numbers (
     *      id uuid PRIMARY KEY,
     *      "number" INT NOT NULL
     * )
     * ```
     */
    object NumbersTable: UUIDTable() {
        val number = integer("number")
    }

    /**
     * StringsTable은 Long 타입의 ID를 기본 키로 가지며, 문자열을 저장하는 테이블입니다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS strings (
     *      id BIGINT PRIMARY KEY,
     *      "text" VARCHAR(10) NOT NULL
     * )
     * ```
     */
    object StringsTable: LongIdTable("") {
        val text = varchar("text", 10)
    }

    /**
     * IConnectionTable은 NumbersTable과 StringsTable 간의 관계를 정의하는 인터페이스입니다.
     * 이를 구현하는 테이블은 numId와 stringId를 포함해야 합니다.
     */
    interface IConnectionTable {
        val numId: Column<EntityID<UUID>>
        val stringId: Column<EntityID<Long>>
    }

    /**
     * ConnectionTable은 NumbersTable과 StringsTable 간의 관계를 정의하는 테이블입니다.
     * UUID를 기본 키로 사용하며, 두 테이블 간의 다대다 관계를 나타냅니다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS "Connection" (
     *      id uuid PRIMARY KEY,
     *      "numId" uuid NOT NULL,
     *      "stringId" BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_connection_numid__id FOREIGN KEY ("numId")
     *      REFERENCES numbers(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_connection_stringid__id FOREIGN KEY ("stringId")
     *      REFERENCES strings(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE "Connection"
     *      ADD CONSTRAINT connection_numid_stringid_unique UNIQUE ("numId", "stringId");
     * ```
     */
    object ConnectionTable: UUIDTable(), IConnectionTable {
        override val numId = reference("numId", NumbersTable)
        override val stringId = reference("stringId", StringsTable)

        init {
            uniqueIndex(numId, stringId)
        }
    }

    /**
     * ConnectionAutoTable은 ConnectionTable과 유사하지만, Long 타입의 자동 증가 ID를 기본 키로 사용합니다.
     * 또한, 외래 키 제약 조건에서 ON DELETE CASCADE를 사용합니다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS connectionauto (
     *      id SERIAL PRIMARY KEY,
     *      "numId" uuid NOT NULL,
     *      "stringId" BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_connectionauto_numid__id FOREIGN KEY ("numId")
     *      REFERENCES numbers(id) ON DELETE CASCADE ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_connectionauto_stringid__id FOREIGN KEY ("stringId")
     *      REFERENCES strings(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE connectionauto
     *      ADD CONSTRAINT connectionauto_numid_stringid_unique UNIQUE ("numId", "stringId");
     * ```
     */
    object ConnectionAutoTable: LongIdTable(), IConnectionTable {
        override val numId = reference("numId", NumbersTable, onDelete = ReferenceOption.CASCADE)
        override val stringId = reference("stringId", StringsTable, onDelete = ReferenceOption.CASCADE)

        init {
            uniqueIndex(numId, stringId)
        }
    }

    val allTables = arrayOf(NumbersTable, StringsTable, ConnectionTable, ConnectionAutoTable)

    /**
     * VNumber는 NumbersTable의 행을 나타내는 엔티티 클래스입니다.
     * 연결된 VString 엔티티를 조회할 수 있는 프로퍼티를 제공합니다.
     */
    class VNumber(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<VNumber>(NumbersTable)

        var number: Int by NumbersTable.number
        var connectedStrings: SizedIterable<VString> by VString via ConnectionTable // ConnectionTable을 통해 연결된 VString 엔티티
        var connectedAutoStrings: SizedIterable<VString> by VString via ConnectionAutoTable // ConnectionAutoTable을 통해 연결된 VString 엔티티

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("number", number)
            .toString()
    }

    /**
     * VString은 StringsTable의 행을 나타내는 엔티티 클래스입니다.
     */
    class VString(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<VString>(StringsTable)

        var text: String by StringsTable.text

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("text", text)
            .toString()
    }
}

/**
 * many-to-many 에서 relation table 을 통해 연결된 entity 들을 다루는 예제입니다.
 */
class Ex12_Via: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * [VNumber] 엔티티에 대해, 지정된 관계 테이블에 값을 넣고 확인하는 헬퍼 메서드입니다.
     */
    private fun VNumber.testWithBothTables(
        valuesToSet: List<VString>,
        body: (IConnectionTable, List<ResultRow>) -> Unit,
    ) {
        listOf(ConnectionTable, ConnectionAutoTable).forEach { ct ->
            when (ct) {
                is ConnectionTable -> connectedStrings = SizedCollection(valuesToSet)
                is ConnectionAutoTable -> connectedAutoStrings = SizedCollection(valuesToSet)
            }

            val result = ct.selectAll().toList()
            body(ct, result)
        }
    }

    /**
     * 하나의 VNumber와 VString 간 다대다 관계를 생성 후, 올바르게 매핑되었는지 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `connection 01`(testDB: TestDB) {
        withTables(testDB, *ViaTestData.allTables) {
            val n = VNumber.new { number = 42 }
            val s = VString.new { text = "foo" }

            n.testWithBothTables(listOf(s)) { ct, result ->
                val row = result.single()
                row[ct.numId] shouldBeEqualTo n.id
                row[ct.stringId] shouldBeEqualTo s.id
            }
        }
    }

    /**
     * 여러 개의 VString을 하나의 VNumber에 연결하고, 데이터를 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `connection 02`(testDB: TestDB) {
        withTables(testDB, *ViaTestData.allTables) {
            val n1 = VNumber.new { number = 1 }
            val n2 = VNumber.new { number = 2 }
            n1.id._value.shouldNotBeNull()
            n2.id._value.shouldNotBeNull()

            val s1 = VString.new { text = "foo" }
            val s2 = VString.new { text = "bar" }

            n1.testWithBothTables(listOf(s1, s2)) { ct, row ->
                row shouldHaveSize 2

                row[0][ct.numId] shouldBeEqualTo n1.id
                row[1][ct.numId] shouldBeEqualTo n1.id
                row.map { it[ct.stringId] } shouldBeEqualTo listOf(s1.id, s2.id)
            }
        }
    }

    /**
     * 두 개의 VNumber와 여러 VString을 연결한 후, 관계를 해제했을 때의 동작을 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `connection 03`(testDB: TestDB) {
        withTables(testDB, *ViaTestData.allTables) {
            val n1 = VNumber.new { number = 1 }
            val n2 = VNumber.new { number = 2 }

            val s1 = VString.new { text = "aaa" }
            val s2 = VString.new { text = "bbb" }

            n1.testWithBothTables(listOf(s1, s2)) { _, _ -> }
            n2.testWithBothTables(listOf(s1, s2)) { _, row ->
                row shouldHaveSize 4
                n1.connectedStrings.toList() shouldBeEqualTo listOf(s1, s2)
                n2.connectedStrings.toList() shouldBeEqualTo listOf(s1, s2)
            }
            n1.testWithBothTables(emptyList()) { table, row ->
                row shouldHaveSize 2
                row[0][table.numId] shouldBeEqualTo n2.id
                row[1][table.numId] shouldBeEqualTo n2.id
                n1.connectedStrings.toList().shouldBeEmpty()
                n2.connectedStrings.toList() shouldBeEqualTo listOf(s1, s2)
            }
        }
    }

    /**
     * 여러 연결 관계에서 SizedCollection을 이용해 특정 항목을 제거했을 때의 동작을 테스트합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `connection 04`(testDB: TestDB) {
        withTables(testDB, *ViaTestData.allTables) {
            val n1 = VNumber.new { number = 1 }
            val n2 = VNumber.new { number = 2 }

            val s1 = VString.new { text = "aaa" }
            val s2 = VString.new { text = "bbb" }

            n1.testWithBothTables(listOf(s1, s2)) { _, _ -> }
            n2.testWithBothTables(listOf(s1, s2)) { _, row ->
                row shouldHaveSize 4
                n1.connectedStrings.toList() shouldBeEqualTo listOf(s1, s2)
                n2.connectedStrings.toList() shouldBeEqualTo listOf(s1, s2)
            }
            // SizedCollection에서 제거하면 cascade delete 되어 s2 가 삭제된다.
            n1.testWithBothTables(listOf(s1)) { _, row ->
                row shouldHaveSize 3
                n1.connectedStrings.toList() shouldBeEqualTo listOf(s1)
                n2.connectedStrings.toList() shouldBeEqualTo listOf(s1, s2)
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS nodes (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object NodesTable: IntIdTable() {
        val name: Column<String> = varchar("name", 50)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS nodetonodes (
     *      parent_node_id INT NOT NULL,
     *      child_node_id INT NOT NULL,
     *
     *      CONSTRAINT fk_nodetonodes_parent_node_id__id FOREIGN KEY (parent_node_id)
     *      REFERENCES nodes(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_nodetonodes_child_node_id__id FOREIGN KEY (child_node_id)
     *      REFERENCES nodes(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object NodeToNodes: Table() {
        val parentId: Column<EntityID<Int>> = reference("parent_node_id", NodesTable)
        val childId: Column<EntityID<Int>> = reference("child_node_id", NodesTable)

        init {
            uniqueIndex(parentId, childId)
        }
    }

    class Node(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Node>(NodesTable)

        var name: String by NodesTable.name

        // NodeToNodes.childId -> NotToNodes.parentId 
        var parents: SizedIterable<Node> by Node.via(NodeToNodes.childId, NodeToNodes.parentId)

        // NodeToNodes.parentId -> NodeToNodes.childId
        var children: SizedIterable<Node> by Node.via(NodeToNodes.parentId, NodeToNodes.childId)

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = "Node($idValue)"
    }

    /**
     * [NodesTable] 의 계층 구조를 [NodeToNodes] 를 통해 표현합니다.
     * 이를 사용하여 계층구조를 관리하는 예를 보여줍니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `hierarchy references`(testDB: TestDB) {
        withTables(testDB, NodesTable, NodeToNodes) {

            /**
             * ```sql
             * INSERT INTO nodes ("name") VALUES ('root');
             * INSERT INTO nodes ("name") VALUES ('child1');
             *
             * SELECT nodes.id, nodes."name", nodetonodes.parent_node_id, nodetonodes.child_node_id
             *   FROM nodes INNER JOIN nodetonodes ON nodes.id = nodetonodes.parent_node_id
             *  WHERE nodetonodes.child_node_id = 2;
             *
             * DELETE FROM nodetonodes
             *  WHERE (nodetonodes.child_node_id = 2)
             *    AND (nodetonodes.parent_node_id != 1);
             *
             * INSERT INTO nodetonodes (child_node_id, parent_node_id) VALUES (2, 1);
             * ```
             */
            val root = Node.new { name = "root" }
            val child1 = Node.new {
                name = "child1"
                parents = SizedCollection(root)      // 이렇게 parents 를 지정하면 `NodeToNodes` 에 insert 된다.
            }

            entityCache.clear()

            log.debug { "Create Root, Child1 " }

            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM nodes INNER JOIN nodetonodes ON nodes.id = nodetonodes.parent_node_id
             *  WHERE nodetonodes.child_node_id = 1
             * ```
             */
            root.parents.count() shouldBeEqualTo 0L
            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM nodes INNER JOIN nodetonodes ON nodes.id = nodetonodes.child_node_id
             *  WHERE nodetonodes.parent_node_id = 1
             * ```
             */
            root.children.count() shouldBeEqualTo 1L


            val child2 = Node.new { name = "child2" }

            /**
             * root.children 을 update 한다. (child 에 해당하는 정보를 모두 지우고, insert 한다)
             *
             * ```sql
             * -- Postgres
             * SELECT nodes.id, nodes."name", nodetonodes.child_node_id, nodetonodes.parent_node_id
             *   FROM nodes INNER JOIN nodetonodes ON nodes.id = nodetonodes.child_node_id
             *  WHERE nodetonodes.parent_node_id = 1;
             *
             * DELETE FROM nodetonodes
             *  WHERE (nodetonodes.parent_node_id = 1)
             *    AND (nodetonodes.child_node_id NOT IN (2, 3));
             *
             * INSERT INTO nodetonodes (parent_node_id, child_node_id) VALUES (1, 3)
             * ```
             */
            root.children = SizedCollection(child1, child2)

            /**
             * ```sql
             * SELECT nodes.id, nodes."name", nodetonodes.parent_node_id, nodetonodes.child_node_id
             *   FROM nodes INNER JOIN nodetonodes ON nodes.id = nodetonodes.parent_node_id
             *  WHERE nodetonodes.child_node_id = 2
             * ```
             */
            child1.parents.singleOrNull() shouldBeEqualTo root

            /**
             * ```sql
             * SELECT nodes.id, nodes."name", nodetonodes.parent_node_id, nodetonodes.child_node_id
             *   FROM nodes INNER JOIN nodetonodes ON nodes.id = nodetonodes.parent_node_id
             *  WHERE nodetonodes.child_node_id = 3
             * ```
             */
            child2.parents.singleOrNull() shouldBeEqualTo root
        }
    }

    /**
     * Insert & Refresh 테스트
     *
     * ```sql
     * -- Postgres
     * INSERT INTO strings (id, "text") VALUES (1335610413324173312, 'foo');
     *
     * SELECT strings.id, strings."text"
     *   FROM strings
     *  WHERE strings.id = 1335610413324173312;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `refresh entity`(testDB: TestDB) {
        withTables(testDB, *ViaTestData.allTables) {
            val s = VString.new {
                text = "foo"
            }

            s.refresh(true)
            s.text shouldBeEqualTo "foo"
        }
    }

    /**
     * 계층 구조가 있는 엔티티를 eager loading한 뒤, 부모-자식 관계가 캐시에 정상적으로 반영되는지 검증합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `warm up on hierarchy entities`(testDB: TestDB) {
        withTables(testDB, NodesTable, NodeToNodes) {
            val child1 = Node.new { name = "child1" }
            val child2 = Node.new { name = "child2" }
            val root1 = Node.new {
                name = "root1"
                children = SizedCollection(child1)
            }
            val root2 = Node.new {
                name = "root2"
                children = SizedCollection(child1, child2)
            }

            flushCache()

            fun checkChildrenReferences(node: Node, values: List<Node>) {
                val sourceColumn = (Node::children
                    .apply { isAccessible = true }.getDelegate(node) as InnerTableLink<*, *, *, *>).sourceColumn
                val children = entityCache.getReferrers<Node>(node.id, sourceColumn)
                children?.toList() shouldBeEqualTo values
            }

            /**
             * Eager loading for one-to-many relation (`with(Node::children)` 사용)
             *
             * ```sql
             * -- Postgres
             * SELECT nodes.id, nodes."name" FROM nodes;
             *
             * SELECT nodes.id,
             *        nodes."name",
             *        nodetonodes.parent_node_id,
             *        nodetonodes.child_node_id
             *   FROM nodes INNER JOIN nodetonodes ON nodetonodes.child_node_id = nodes.id
             *  WHERE nodetonodes.parent_node_id IN (1, 2, 3, 4)
             * ```
             */
            val nodeWithChildren = Node.all().with(Node::children).toList()
            nodeWithChildren.shouldNotBeEmpty()

            checkChildrenReferences(child1, emptyList())
            checkChildrenReferences(child2, emptyList())
            checkChildrenReferences(root1, listOf(child1))
            checkChildrenReferences(root2, listOf(child1, child2))

            fun checkParentsReferences(node: Node, values: List<Node>) {
                val sourceColumn = (Node::parents
                    .apply { isAccessible = true }.getDelegate(node) as InnerTableLink<*, *, *, *>).sourceColumn
                val parents = entityCache.getReferrers<Node>(node.id, sourceColumn)
                parents?.toList() shouldBeEqualTo values
            }

            /**
             * Eager loading for many-to-one relation (`with(Node::parents)` 사용)
             *
             * ```sql
             * -- Postgres
             * SELECT nodes.id, nodes."name" FROM nodes;
             *
             * SELECT nodes.id,
             *        nodes."name",
             *        nodetonodes.parent_node_id,
             *        nodetonodes.child_node_id
             *   FROM nodes INNER JOIN nodetonodes ON nodetonodes.parent_node_id = nodes.id
             *  WHERE nodetonodes.child_node_id IN (1, 2, 3, 4)
             * ```
             */
            val nodeWithParents = Node.all().with(Node::parents).toList()
            nodeWithParents.shouldNotBeEmpty()

            checkParentsReferences(child1, listOf(root1, root2))
            checkParentsReferences(child2, listOf(root2))
            checkParentsReferences(root1, emptyList())
            checkParentsReferences(root2, emptyList())
        }
    }

    class NodeOrdered(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<NodeOrdered>(NodesTable)

        var name by NodesTable.name
        var parents by NodeOrdered.via(NodeToNodes.childId, NodeToNodes.parentId)
        var children by NodeOrdered
            .via(NodeToNodes.parentId, NodeToNodes.childId)
            .orderBy(NodesTable.name to SortOrder.ASC)

        override fun equals(other: Any?): Boolean = (other as? NodeOrdered)?.id == id
        override fun hashCode(): Int = Objects.hash(id)
        override fun toString(): String = "NodeOrdered($id)"
    }

    /**
     * one-to-many 에 해당하는 [SizedIterable] 을 정렬하여 조회하는 테스트
     *
     * ```sql
     * SELECT nodes.id,
     *        nodes."name",
     *        nodetonodes.child_node_id,
     *        nodetonodes.parent_node_id
     *   FROM nodes INNER JOIN nodetonodes ON nodes.id = nodetonodes.child_node_id
     *  WHERE nodetonodes.parent_node_id = 1
     *  ORDER BY nodes."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `order by sized collection`(testDB: TestDB) {
        withTables(testDB, NodesTable, NodeToNodes) {
            val root = NodeOrdered.new { name = "root" }
            listOf("#3", "#0", "#2", "#4", "#1").forEach {
                NodeOrdered.new {
                    name = it
                    parents = SizedCollection(root)
                }
            }

            root.children.forEachIndexed { index, node ->
                node.name shouldBeEqualTo "#$index"
            }

            flushCache()

            root.children.map { it.name } shouldBeEqualTo listOf("#0", "#1", "#2", "#3", "#4")
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS projects (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object Projects: IntIdTable("projects") {
        val name = varchar("name", 50)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tasks (
     *      id SERIAL PRIMARY KEY,
     *      title VARCHAR(64) NOT NULL
     * )
     * ```
     */
    object Tasks: IntIdTable("tasks") {
        val title = varchar("title", 64)
    }

    /**
     * [Projects] 와 [Tasks] 의 Id 를 Primary Key 로 가지는 매핑용 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS project_tasks (
     *      project_id INT,
     *      task_id INT,
     *      approved BOOLEAN DEFAULT FALSE NOT NULL,
     *
     *      CONSTRAINT pk_project_tasks PRIMARY KEY (project_id, task_id),
     *
     *      CONSTRAINT fk_project_tasks_project_id__id FOREIGN KEY (project_id)
     *      REFERENCES projects(id) ON DELETE CASCADE ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_project_tasks_task_id__id FOREIGN KEY (task_id)
     *      REFERENCES tasks(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * )
     * ```
     */
    object ProjectTasks: CompositeIdTable("project_tasks") {
        val projectId: Column<EntityID<Int>> = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE)
        val taskId: Column<EntityID<Int>> = reference("task_id", Tasks, onDelete = ReferenceOption.CASCADE)

        val approved = bool("approved").default(false)

        override val primaryKey = PrimaryKey(projectId, taskId)

        init {
            // Composite ID 를 정의한다.
            addIdColumn(projectId)
            addIdColumn(taskId)
        }
    }

    class Project(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Project>(Projects)

        var name: String by Projects.name
        var tasks: SizedIterable<Task> by Task via ProjectTasks  // ProjectTasks 를 통해 연결된 Task 엔티티

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("name", name)
                .toString()
    }

    /**
     * [Project] 와 [Task] 를 연결하는 [ProjectTasks] 의 Entity
     */
    class ProjectTask(id: EntityID<CompositeID>): CompositeEntity(id) {
        companion object: CompositeEntityClass<ProjectTask>(ProjectTasks)

        var approved by ProjectTasks.approved

        val projectId: EntityID<Int>
            get() = id.value[ProjectTasks.projectId]

        val project: Project?
            get() = Project.findById(projectId)

        val taskId: EntityID<Int>
            get() = id.value[ProjectTasks.taskId]

        val task: Task?
            get() = Task.findById(taskId)


        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("approved", approved)
            .toString()
    }

    class Task(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Task>(Tasks)

        var title: String by Tasks.title
        var approved: Boolean by ProjectTasks.approved  // ProjectTasks 를 통해 연결된 approved 속성
        var projects: SizedIterable<Project> by Project via ProjectTasks  // ProjectTasks 를 통해 연결된 Project 엔티티

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("title", title)
            .add("approved", approved)
            .toString()
    }

    /**
     * project-tasks 관계에서 composite ID를 사용한 추가 속성(approved)을 포함한 매핑 동작을 테스트합니다.
     *
     * ```sql
     * -- create ProjectTask
     * INSERT INTO project_tasks (task_id, project_id, approved) VALUES (1, 1, TRUE);
     * INSERT INTO project_tasks (task_id, project_id, approved) VALUES (2, 2, FALSE);
     * INSERT INTO project_tasks (task_id, project_id, approved) VALUES (3, 2, FALSE);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `additional link data using composite id inner table`(testDB: TestDB) {
        withTables(testDB, Projects, Tasks, ProjectTasks) {

            // 샘플 데이터 추가
            val p1 = Project.new { name = "Project 1" }
            val p2 = Project.new { name = "Project 2" }

            val t1 = Task.new { title = "Task 1" }
            val t2 = Task.new { title = "Task 2" }
            val t3 = Task.new { title = "Task 3" }

            // Project 와 Task 를 연결하는 ProjectTask 엔티티 생성
            ProjectTask.new(
                CompositeID {
                    it[ProjectTasks.projectId] = p1.id
                    it[ProjectTasks.taskId] = t1.id
                }
            ) {
                approved = true
            }

            ProjectTask.new(
                CompositeID {
                    it[ProjectTasks.projectId] = p2.id
                    it[ProjectTasks.taskId] = t2.id
                }
            ) {
                approved = false
            }

            ProjectTask.new(
                CompositeID {
                    it[ProjectTasks.projectId] = p2.id
                    it[ProjectTasks.taskId] = t3.id
                }
            ) {
                approved = false
            }

            commit()

            // 캐시를 공유하지 않도록, 다른 트랜잭션에서 조회 작업을 수행합니다.
            inTopLevelTransaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                maxAttempts = 1

                /**
                 * Project 조회 시 관련된 Task 엔티티를 eager loading 합니다. (`with(Project::tasks)` 사용)
                 *
                 * ```sql
                 * -- Postgres
                 * SELECT projects.id, projects."name" FROM projects;
                 *
                 * SELECT tasks.id,
                 *        tasks.title,
                 *        project_tasks.project_id,
                 *        project_tasks.task_id,
                 *        project_tasks.approved
                 *   FROM tasks INNER JOIN project_tasks ON project_tasks.task_id = tasks.id
                 *  WHERE project_tasks.project_id IN (1, 2)
                 * ```
                 */
                Project.all().with(Project::tasks).toList()
                val cache = TransactionManager.current().entityCache

                // eager load 되었으므로 캐시에 존재한다.
                val p1Tasks = cache.getReferrers<Task>(p1.id, ProjectTasks.projectId)?.toList().orEmpty()
                p1Tasks.map { it.id } shouldBeEqualTo listOf(t1.id)
                p1Tasks.all { it.approved }.shouldBeTrue()

                // eager load 되었으므로 캐시에 존재한다.
                val p2Tasks = cache.getReferrers<Task>(p2.id, ProjectTasks.projectId)?.toList().orEmpty()
                p2Tasks.map { it.id } shouldBeEqualTo listOf(t2.id, t3.id)
                p2Tasks.all { !it.approved }.shouldBeTrue()

                // eager load 되었으므로 count() 함수에 대한 쿼리를 발생시키지 않는다.
                p1.tasks.count() shouldBeEqualTo 1
                p2.tasks.count() shouldBeEqualTo 2

                // eager load 되었으므로 쿼리를 발생시키지 않는다.
                p1.tasks.single().approved shouldBeEqualTo true
                p2.tasks.map { it.approved } shouldBeEqualTo listOf(false, false)
            }
        }
    }
}
