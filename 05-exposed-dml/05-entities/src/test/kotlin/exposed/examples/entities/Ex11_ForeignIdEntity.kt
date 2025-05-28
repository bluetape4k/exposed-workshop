package exposed.examples.entities

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * 엔티티의 ID 가 다른 테이블의 ID를 참조 (Foreign Key) 하는 엔티티에 대한 예제
 *
 * one-to-one, one-to-many, many-to-one 관계를 모두 포함하고 있습니다.
 */
class Ex11_ForeignIdEntity: JdbcExposedTestBase() {

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
        val ownerId: Column<String> = varchar("owner_id", 50)
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
        val config: ProjectConfig by ProjectConfig backReferencedOn ProjectConfigs  // one-to-one relationship

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class ProjectConfig(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<ProjectConfig>(ProjectConfigs)

        val project by Project referencedOn ProjectConfigs  // one-to-one relationship
        var ownerId by ProjectConfigs.ownerId
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
        withTables(testDB, Projects, ProjectConfigs, configure = { useNestedTransactions = true }) {
            val project1 = transaction {
                /**
                 * ```sql
                 * INSERT INTO projects ("name") VALUES ('Space');
                 * INSERT INTO project_configs (id, owner_id, setting) VALUES (1, 'debop', TRUE);
                 * ```
                 */
                val project1 = Project.new { name = "Space" }
                // ProjectConfig is a one-to-one relationship with Project
                ProjectConfig.new(project1.id.value) {
                    ownerId = "debop"
                    setting = true
                }

                project1
            }

            val project2 = transaction {
                /**
                 * ```sql
                 * INSERT INTO projects ("name") VALUES ('Earth');
                 * INSERT INTO project_configs (id, owner_id, setting) VALUES (2, 'jane', TRUE);
                 * ```
                 */
                val project2 = Project.new { name = "Earth" }
                ProjectConfig.new(project2.id.value) {
                    ownerId = "jane"
                    setting = true
                }
                project2
            }

            transaction {
                // UPDATE project_configs SET setting=FALSE WHERE id = 1
                ProjectConfig.findById(project1.id)!!.setting = false
            }

            transaction {
                // SELECT project_configs.id, project_configs.setting FROM project_configs WHERE project_configs.id = 1
                val config = ProjectConfig.findById(project1.id)!!
                config.setting.shouldBeFalse()
                // SELECT projects.id, projects."name" FROM projects WHERE projects.id = 1
                config.project.name shouldBeEqualTo project1.name
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

            // SELECT roles.id, roles.guild_id FROM roles WHERE roles.guild_id = '3746529'
            actorA.roles.toList() shouldContainSame listOf(roleA, roleB)

            // SELECT roles.id, roles.guild_id FROM roles;
            val roles = Role.all().toList()
            roles shouldContainSame listOf(roleA, roleB)
            // SELECT actors.guild_id FROM actors WHERE actors.guild_id = '3746529'
            roles.all { it.actor == actorA }.shouldBeTrue()

        }
    }

    object Users: IntIdTable("users") {
        val name: Column<String> = varchar("user_name", 50)
    }

    object UserProfiles: IdTable<Int>("user_profiles") {
        override val id: Column<EntityID<Int>> = reference("user_id", Users)  // one-to-one relationship
        val bio = varchar("bio", 255)
    }

    class User(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<User>(Users)

        var name by Users.name
        val profile: UserProfile by UserProfile backReferencedOn UserProfiles  // one-to-one relationship

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class UserProfile(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserProfile>(UserProfiles)

        var bio by UserProfiles.bio
        var user by User referencedOn UserProfiles

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("bio", bio)
            .toString()
    }

    /**
     * one-to-one 관계인 [User] 와 [UserProfile] 를 다루는 테스트
     *
     * ```sql
     * -- Postgres
     * INSERT INTO users (user_name) VALUES ('Earth')
     * INSERT INTO user_profiles (user_id, bio) VALUES (1, 'Earth is a planet')
     *
     * SELECT users.id, users.user_name FROM users WHERE users.id = 1
     * SELECT user_profiles.user_id, user_profiles.bio FROM user_profiles WHERE user_profiles.user_id = 1
     * SELECT user_profiles.user_id, user_profiles.bio FROM user_profiles WHERE user_profiles.user_id = 1
     *
     * UPDATE user_profiles SET bio='Updated: Earth is a planet' WHERE user_id = 1
     * SELECT users.id, users.user_name FROM users WHERE users.id = 1
     * SELECT user_profiles.user_id, user_profiles.bio FROM user_profiles WHERE user_profiles.user_id = 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `user and user profile`(testDB: TestDB) {
        withTables(testDB, Users, UserProfiles) {
            val user = User.new { name = "Earth" }
            UserProfile.new(user.id.value) { bio = "Earth is a planet" }

            entityCache.clear()

            val loadedUser = User.findById(user.id)!!
            loadedUser.profile.bio shouldBeEqualTo "Earth is a planet"
            loadedUser.profile.user shouldBeEqualTo user

            entityCache.clear()

            val profile = UserProfile.findById(user.id)!!
            profile.bio = "Updated: Earth is a planet"

            entityCache.clear()

            val loadedUser2 = User.findById(user.id)!!.load(User::profile)
            loadedUser2.profile.bio shouldBeEqualTo "Updated: Earth is a planet"
            loadedUser2.profile.user shouldBeEqualTo user
        }
    }
}
