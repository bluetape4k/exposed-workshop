package exposed.examples.json

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.json.jsonb

object JsonTestData {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS j_table (
     *      id SERIAL PRIMARY KEY,
     *      j_column JSON NOT NULL
     * )
     * ```
     */
    object JsonTable: IntIdTable("j_table") {
        val jsonColumn = json<DataHolder>("j_column", Json.Default)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS j_b_table (
     *      id SERIAL PRIMARY KEY,
     *      j_b_column JSONB NOT NULL
     * );
     *
     * -- MySQL
     * CREATE TABLE IF NOT EXISTS j_b_table (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      j_b_column JSON NOT NULL
     * )
     * ```
     */
    object JsonBTable: IntIdTable("j_b_table") {
        val jsonBColumn = jsonb<DataHolder>("j_b_column", Json.Default)
    }

    class JsonEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<JsonEntity>(JsonTable)

        var jsonColumn: DataHolder by JsonTable.jsonColumn

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("jsonColumn", jsonColumn)
            .toString()
    }

    class JsonBEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<JsonBEntity>(JsonBTable)

        var jsonBColumn: DataHolder by JsonBTable.jsonBColumn

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("jsonBColumn", jsonBColumn)
            .toString()
    }

    /**
     * Array 수형을 JSON으로 저장합니다.
     * 
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS j_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSON NOT NULL,
     *      numbers JSON NOT NULL
     * );
     * ```
     */
    object JsonArrayTable: IntIdTable("j_arrays") {
        val groups = json<UserGroup>("groups", Json.Default)
        val numbers = json<IntArray>("numbers", Json.Default)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS j_b_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSONB NOT NULL,
     *      numbers JSONB NOT NULL
     * );
     * ```
     */
    object JsonBArrayTable: IntIdTable("j_b_arrays") {
        val groups = jsonb<UserGroup>("groups", Json.Default)
        val numbers = jsonb<IntArray>("numbers", Json.Default)
    }
}

@Serializable
data class DataHolder(
    val user: User,
    val logins: Int,
    val active: Boolean,
    val team: String?,
)

@Serializable
data class UserGroup(
    val users: List<User>,
)

@Serializable
data class User(
    val name: String,
    val team: String?,
)
