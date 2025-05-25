//package exposed.examples.jpa.ex06_cte
//
//import exposed.examples.jpa.ex04_tree.TreeNodeSchema.TreeNodeTable
//import exposed.examples.jpa.ex04_tree.TreeNodeSchema.buildTreeNodes
//import exposed.shared.tests.AbstractExposedTest
//import exposed.shared.tests.TestDB
//import exposed.shared.tests.withTables
//import io.bluetape4k.logging.KLogging
//import io.bluetape4k.logging.debug
//import org.amshove.kluent.shouldHaveSize
//import org.jetbrains.exposed.v1.core.IColumnType
//import org.jetbrains.exposed.v1.core.Transaction
//import org.jetbrains.exposed.v1.core.statements.ReturningStatement
//import org.jetbrains.exposed.v1.core.statements.Statement
//import org.jetbrains.exposed.v1.core.statements.StatementType
//import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
//import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
//import org.jetbrains.exposed.v1.jdbc.statements.BlockingExecutable
//import org.jetbrains.exposed.v1.jdbc.statements.ReturningBlockingExecutable
//import org.junit.jupiter.api.Assumptions
//import org.junit.jupiter.params.ParameterizedTest
//import org.junit.jupiter.params.provider.MethodSource
//import java.io.Serializable
//import java.sql.ResultSet
//
///**
// * Exposed (0.59.0)에서는 아직 CTE(Common Table Expression) 기능을 제공하지 않습니다.
// *
// * CTE(Common Table Expression)를 사용하기 위해서 직접 쿼리를 작성해야 합니다.
// * 이 예제는 PostgreSQL에서만 동작합니다.
// */
//class Ex01_CTE: AbstractExposedTest() {
//
//    companion object: KLogging()
//
//    private val stmt =
//        """
//        WITH RECURSIVE deep_nodes AS (
//            -- Anchor 쿼리 (루트 노드)
//            SELECT id, title, parent_id, depth, id::TEXT as path
//              FROM tree_nodes
//             WHERE parent_id IS NULL
//
//            UNION ALL
//
//            -- Recursive 쿼리 (자식 노드)
//            SELECT tn.id, tn.title, tn.parent_id, tn.depth, (deep_nodes.path || '.' || tn.id::TEXT) as path
//              FROM tree_nodes tn JOIN deep_nodes ON tn.parent_id = deep_nodes.id
//        )
//        -- 최종 결과
//        SELECT id, title, parent_id, depth, path FROM deep_nodes
//        """.trimIndent()
//
//    @ParameterizedTest
//    @MethodSource(ENABLE_DIALECTS_METHOD)
//    fun `raw common table expression for treeNode`(testDB: TestDB) {
//        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }
//
//        withTables(testDB, TreeNodeTable) {
//            buildTreeNodes()
//
//            exec(stmt, explicitStatementType = StatementType.SELECT) { rs ->
//                while (rs.next()) {
//                    val id = rs.getLong("id")
//                    val title = rs.getString("title")
//                    val parentId = rs.getLong("parent_id")
//                    val depth = rs.getInt("depth")
//                    val path = rs.getString("path")
//                    log.debug { "id=$id, title=$title, parent_id=$parentId, depth=$depth, path=$path" }
//                }
//            }
//        }
//    }
//
//    @ParameterizedTest
//    @MethodSource(ENABLE_DIALECTS_METHOD)
//    fun `raw common table expression to treeNodeRecord`(testDB: TestDB) {
//        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }
//
//        withTables(testDB, TreeNodeTable) {
//            buildTreeNodes()
//
//            val records = execCTE(stmt) {
//                TreeNodeRecord(
//                    it.getLong("id"),
//                    it.getString("title"),
//                    it.getLong("parent_id"),
//                    it.getInt("depth"),
//                    it.getString("path"),
//                )
//            }
//
//            records shouldHaveSize 5
//
//            records.forEach {
//                log.debug { "id=${it.id}, title=${it.title}, parent_id=${it.parentId}, depth=${it.depth}, path=${it.path}" }
//            }
//        }
//    }
//
//    data class TreeNodeRecord(
//        val id: Long,
//        val title: String,
//        val parentId: Long,
//        val depth: Int,
//        val path: String,
//    ): Serializable
//
//    private fun <T: Any> JdbcTransaction.execCTE(stmt: String, transform: (ResultSet) -> T): List<T> {
//        if (stmt.isBlank()) return emptyList()
//        if (!stmt.trim().startsWith("WITH RECURSIVE", ignoreCase = true)) {
//            throw IllegalArgumentException("Not a CTE statement: $stmt")
//        }
//
//        val type = StatementType.SELECT
//
//        val cte = object: ReturningStatement<List<T>>(type, emptyList()) {
//            override fun arguments(): Iterable<Iterable<Pair<IColumnType<*>, Any?>>> = emptyList()
//
//            override fun prepareSQL(transaction: Transaction, prepared: Boolean): String {
//                return stmt
//            }
//
//            override fun PreparedStatementApi.executeInternal(transaction: JdbcTransaction): List<T> {
//                executeQuery()
//                return resultSet?.use {
//                    val results = mutableListOf<T>()
//                    while (it.next()) {
//                        results.add(transform(it))
//                    }
//                    results
//                } ?: emptyList()
//            }
//        }
//
//        val executable = ReturningBlockingExecutable(cte) {
//
//        }
//        return exec(executable) ?: emptyList()
//    }
//}
