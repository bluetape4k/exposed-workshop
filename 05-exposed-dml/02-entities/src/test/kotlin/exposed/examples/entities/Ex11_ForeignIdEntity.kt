package exposed.examples.entities

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Foreign Key 를 가지는 엔티티를 다루는 예제입니다.
 *
 * one-to-one, one-to-many, many-to-one 관계를 모두 포함하고 있습니다.
 */
class Ex11_ForeignIdEntity: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS projects (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object Projects: LongIdTable("projects") {
        val name: Column<String> = varchar("name", 50)
    }

    /**
     * one-to-one relationship to [Projects]
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS project_configs (
     *      id BIGINT NOT NULL,
     *      setting BOOLEAN NOT NULL,
     *
     *      CONSTRAINT fk_project_configs_id__id FOREIGN KEY (id) REFERENCES projects(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object ProjectConfigs: IdTable<Long>("project_configs") {
        override val id: Column<EntityID<Long>> = reference("id", Projects)     // one-to-one relationship
        val setting: Column<Boolean> = bool("setting")
    }

    /**
     * Actor 테이블
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS actors (
     *      guild_id VARCHAR(13) PRIMARY KEY
     * )
     * ```
     */
    object Actors: IdTable<String>("actors") {
        override val id: Column<EntityID<String>> = varchar("guild_id", 13).entityId()
        override val primaryKey = PrimaryKey(id)
    }

    /**
     * many-to-one relationship to [Actors]
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS roles (
     *      id SERIAL PRIMARY KEY,
     *      guild_id VARCHAR(13) NOT NULL,
     *
     *      CONSTRAINT fk_roles_guild_id__guild_id FOREIGN KEY (guild_id) REFERENCES actors(guild_id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Roles: IntIdTable("roles") {
        val actor: Column<EntityID<String>> = reference("guild_id", Actors)  // many-to-one relationship
    }

    class Project(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Project>(Projects)

        var name by Projects.name

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class ProjectConfig(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<ProjectConfig>(ProjectConfigs)

        var setting by ProjectConfigs.setting

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("setting", setting)
            .toString()
    }

    class Actor(id: EntityID<String>): Entity<String>(id) {
        companion object: EntityClass<String, Actor>(Actors)

        val roles: SizedIterable<Role> by Role referrersOn Roles.actor  // one-to-many relationship

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().toString()
    }

    class Role(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Role>(Roles)

        var actor by Actor referencedOn Roles.actor  // many-to-one relationship

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().toString()
    }

    /**
     * one-to-one 관계인 [Project] 와 [ProjectConfig] 를 다루는 테스트
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `foreign id entity update`(testDB: TestDB) {
        withTables(
            testDB,
            Projects, ProjectConfigs,
            configure = { useNestedTransactions = true }
        ) {
            val project1 = transaction {
                /**
                 * ```sql
                 * INSERT INTO projects ("name") VALUES ('Space');
                 * INSERT INTO project_configs (id, setting) VALUES (1, TRUE);
                 * ```
                 */
                val project1 = Project.new { name = "Space" }
                // ProjectConfig is a one-to-one relationship with Project
                ProjectConfig.new(project1.id.value) { setting = true }

                project1
            }

            val project2 = transaction {
                /**
                 * ```sql
                 * INSERT INTO projects ("name") VALUES ('Earth');
                 * INSERT INTO project_configs (id, setting) VALUES (2, TRUE);
                 * ```
                 */
                val project2 = Project.new { name = "Earth" }
                ProjectConfig.new(project2.id.value) { setting = true }
                project2
            }

            transaction {
                // UPDATE project_configs SET setting=FALSE WHERE id = 1
                ProjectConfig.findById(project1.id)!!.setting = false
            }

            transaction {
                // SELECT project_configs.id, project_configs.setting FROM project_configs WHERE project_configs.id = 1
                ProjectConfig.findById(project1.id)!!.setting.shouldBeFalse()
            }
        }
    }

    /**
     * 참조된 엔티티를 기준으로 조회하기
     *
     * ```sql
     * -- Postgres
     * SELECT roles.id, roles.guild_id
     *   FROM roles
     *  WHERE roles.guild_id = '3746529';
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `referenced entities with identitical column names`(testDB: TestDB) {
        withTables(testDB, Actors, Roles) {
            val actorA = Actor.new("3746529") { }
            val roleA = Role.new { actor = actorA }
            val roleB = Role.new { actor = actorA }

            entityCache.clear()

            actorA.roles.toList() shouldContainSame listOf(roleA, roleB)

            // SELECT roles.id, roles.guild_id FROM roles;
            Role.all().toList() shouldContainSame listOf(roleA, roleB)
        }
    }
}
