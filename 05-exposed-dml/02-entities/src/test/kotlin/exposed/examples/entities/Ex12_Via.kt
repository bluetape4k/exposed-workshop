package exposed.examples.entities

import exposed.examples.entities.ViaTestData.ConnectionAutoTable
import exposed.examples.entities.ViaTestData.ConnectionTable
import exposed.examples.entities.ViaTestData.IConnectionTable
import exposed.examples.entities.ViaTestData.VNumber
import exposed.examples.entities.ViaTestData.VString
import exposed.shared.tests.AbstractExposedTest
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
import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.InnerTableLink
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.inTopLevelTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.util.*
import kotlin.reflect.jvm.isAccessible

object ViaTestData {

    /**
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

    interface IConnectionTable {
        val numId: Column<EntityID<UUID>>
        val stringId: Column<EntityID<Long>>
    }

    /**
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

    class VNumber(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<VNumber>(NumbersTable)

        var number: Int by NumbersTable.number
        var connectedStrings: SizedIterable<VString> by VString via ConnectionTable
        var connectedAutoStrings: SizedIterable<VString> by VString via ConnectionAutoTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("number", number)
            .toString()
    }

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
 * many-to-many 에서 relation table 을 통해 연결된 entity 들을 조회하는 테스트 (`via` 테스트)
 */
class Ex11_Via: AbstractExposedTest() {

    companion object: KLogging()


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

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `connection 02`(testDB: TestDB) {
        withTables(testDB, *ViaTestData.allTables) {
            val n1 = VNumber.new { number = 1 }
            val n2 = VNumber.new { number = 2 }

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
        val name = varchar("name", 50)
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
        val parent = reference("parent_node_id", NodesTable)
        val child = reference("child_node_id", NodesTable)
    }

    class Node(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Node>(NodesTable)

        var name by NodesTable.name
        var parents by Node.via(NodeToNodes.child, NodeToNodes.parent)
        var children by Node.via(NodeToNodes.parent, NodeToNodes.child)

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
                parents = SizedCollection(root)
            }
            flushCache()
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
            checkParentsReferences(child1, listOf(root1, root2))
            checkParentsReferences(child2, listOf(root2))
            checkParentsReferences(root1, emptyList())
            checkParentsReferences(root2, emptyList())
        }
    }

    class NodeOrdered(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<NodeOrdered>(NodesTable)

        var name by NodesTable.name
        var parents by NodeOrdered.via(NodeToNodes.child, NodeToNodes.parent)
        var children by NodeOrdered
            .via(NodeToNodes.parent, NodeToNodes.child)
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
        val project = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE)
        val task = reference("task_id", Tasks, onDelete = ReferenceOption.CASCADE)
        val approved = bool("approved").default(false)

        override val primaryKey = PrimaryKey(project, task)

        init {
            // Composite ID 를 정의한다.
            addIdColumn(project)
            addIdColumn(task)
        }
    }

    class Project(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Project>(Projects)

        var name by Projects.name
        var tasks by Task via ProjectTasks

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

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("approved", approved)
            .toString()
    }

    class Task(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Task>(Tasks)

        var title by Tasks.title
        var approved by ProjectTasks.approved

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("title", title)
            .add("approved", approved)
            .toString()
    }

    /**
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
            val p1 = Project.new { name = "Project 1" }
            val p2 = Project.new { name = "Project 2" }

            val t1 = Task.new { title = "Task 1" }
            val t2 = Task.new { title = "Task 2" }
            val t3 = Task.new { title = "Task 3" }

            ProjectTask.new(
                CompositeID {
                    it[ProjectTasks.project] = p1.id
                    it[ProjectTasks.task] = t1.id
                }
            ) {
                approved = true
            }

            ProjectTask.new(
                CompositeID {
                    it[ProjectTasks.project] = p2.id
                    it[ProjectTasks.task] = t2.id
                }
            ) {
                approved = false
            }

            ProjectTask.new(
                CompositeID {
                    it[ProjectTasks.project] = p2.id
                    it[ProjectTasks.task] = t3.id
                }
            ) {
                approved = false
            }

            commit()

            inTopLevelTransaction(Connection.TRANSACTION_SERIALIZABLE) {
                maxAttempts = 1

                /**
                 * Eager loading one-to-many relation (`with(Project::tasks)` 사용)
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
                val p1Tasks = cache.getReferrers<Task>(p1.id, ProjectTasks.project)?.toList().orEmpty()
                p1Tasks.map { it.id } shouldBeEqualTo listOf(t1.id)
                p1Tasks.all { it.approved }.shouldBeTrue()

                // eager load 되었으므로 캐시에 존재한다.
                val p2Tasks = cache.getReferrers<Task>(p2.id, ProjectTasks.project)?.toList().orEmpty()
                p2Tasks.map { it.id } shouldBeEqualTo listOf(t2.id, t3.id)
                p2Tasks.all { !it.approved }.shouldBeTrue()

                // eager load 되었으므로 쿼리를 발생시키지 않는다.
                p1.tasks.count() shouldBeEqualTo 1
                p2.tasks.count() shouldBeEqualTo 2

                // eager load 되었으므로 쿼리를 발생시키지 않는다.
                p1.tasks.single().approved shouldBeEqualTo true
                p2.tasks.map { it.approved } shouldBeEqualTo listOf(false, false)
            }
        }
    }
}
