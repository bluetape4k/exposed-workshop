package exposed.examples.jpa.relations.ex04_many_to_many

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * 그룹, 사용자를 many-to-many 관계로 매핑하는 멤버 테이블을 사용하는 예
 */
object MemberSchema {

    val memberTables = arrayOf(GroupTable, MemberTable, UserTable)

    /**
     * Group Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS "Group" (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      description TEXT NOT NULL,
     *      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     *      owner_id INT NOT NULL,
     *
     *      CONSTRAINT fk_group_owner_id__id FOREIGN KEY (owner_id)
     *          REFERENCES "User"(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     *
     * @see MemberTable
     * @see UserTable
     * @see Group
     */
    object GroupTable: IntIdTable() {
        val name = varchar("name", 50)
        val description = text("description")
        val createAt = datetime("created_at").defaultExpression(CurrentDateTime)

        val owner = reference("owner_id", UserTable)
    }


    /**
     * Member Table (UserTable, GroupTable의 Many-to-Many 관계를 나타내는 테이블)
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS "Member" (
     *      id SERIAL PRIMARY KEY,
     *      user_id INT NOT NULL,
     *      group_id INT NOT NULL,
     *      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     *
     *      CONSTRAINT fk_member_user_id__id FOREIGN KEY (user_id)
     *          REFERENCES "User"(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_member_group_id__id FOREIGN KEY (group_id)
     *          REFERENCES "Group"(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE "Member"
     *      ADD CONSTRAINT member_group_id_user_id_unique UNIQUE (group_id, user_id);
     * ```
     *
     * @see UserTable
     * @see GroupTable
     */
    object MemberTable: IntIdTable() {

        val userId = reference("user_id", UserTable)
        val groupId = reference("group_id", GroupTable)

        val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

        init {
            uniqueIndex(groupId, userId)
        }
    }

    /**
     * User Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS "User" (
     *      id SERIAL PRIMARY KEY,
     *      first_name VARCHAR(50) NOT NULL,
     *      last_name VARCHAR(50) NOT NULL,
     *      username VARCHAR(50) NOT NULL,
     *      status VARCHAR(32) DEFAULT 'UNKNOWN' NOT NULL,
     *      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * );
     * ```
     *
     * @see User
     * @see GroupTable
     * @see MemberTable
     */
    object UserTable: IntIdTable() {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val username = varchar("username", 50)

        // enumeration 타입 사용 (`enumeration`, `enumerationByName`, `customEnumeration`)
        val status = enumerationByName<UserStatus>("status", 32).default(UserStatus.UNKNOWN)
        val createAt = datetime("created_at").defaultExpression(CurrentDateTime)
    }

    class Group(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Group>(GroupTable)

        var name by GroupTable.name
        var description by GroupTable.description
        var owner by User referencedOn GroupTable.owner               // many-to-one
        var members by User.via(MemberTable.groupId, MemberTable.userId)    // many-to-many

        val createdAt by GroupTable.createAt

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("description", description)
            .add("createdAt", createdAt)
            .toString()
    }


    class User(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<User>(UserTable)

        var firstName by UserTable.firstName
        var lastName by UserTable.lastName
        var username by UserTable.username
        var status by UserTable.status

        val groups by Group.via(MemberTable.userId, MemberTable.groupId)  // many-to-many

        val createdAt by UserTable.createAt

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("username", username)
            .add("status", status)
            .add("createdAt", createdAt)
            .toString()
    }

    enum class UserStatus {
        UNKNOWN,
        ACTIVE,
        INACTIVE,
        BANNED;
    }

}
