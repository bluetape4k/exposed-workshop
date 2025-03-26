package exposed.examples.jpa.ex05_relations.ex04_many_to_many

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table

/**
 * 은행 계좌 - 계좌 소유자에 대한 Many-to-Many 관계를 나타내는 스키마
 */
object BankSchema {

    val allTables = arrayOf(BankAccountTable, AccountOwnerTable, OwnerAccountMapTable)

    /**
     * 은행 계좌 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS bank_account (
     *      id SERIAL PRIMARY KEY,
     *      "number" VARCHAR(255) NOT NULL
     * );
     *
     * ALTER TABLE bank_account
     *   ADD CONSTRAINT bank_account_number_unique UNIQUE ("number");
     * ```
     */
    object BankAccountTable: IntIdTable("bank_account") {
        val number = varchar("number", 255).uniqueIndex()
    }

    /**
     * 계좌 소유자 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS account_owner (
     *      id SERIAL PRIMARY KEY,
     *      ssn VARCHAR(255) NOT NULL
     * );
     *
     * ALTER TABLE account_owner
     *   ADD CONSTRAINT account_owner_ssn_unique UNIQUE (ssn);
     * ```
     */
    object AccountOwnerTable: IntIdTable("account_owner") {
        val ssn = varchar("ssn", 255).uniqueIndex()
    }

    /**
     * 은행 계좌 - 계좌 소유자에 대한 Many-to-Many Mapping Table
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS owner_account_map (
     *      owner_id INT NOT NULL,
     *      account_id INT NOT NULL,
     *
     *      CONSTRAINT fk_owner_account_map_owner_id__id FOREIGN KEY (owner_id)
     *          REFERENCES account_owner(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_owner_account_map_account_id__id FOREIGN KEY (account_id)
     *          REFERENCES bank_account(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE owner_account_map
     *      ADD CONSTRAINT owner_account_map_owner_id_account_id_unique UNIQUE (owner_id, account_id);
     * ```
     */
    object OwnerAccountMapTable: Table("owner_account_map") {
        val ownerId = reference("owner_id", AccountOwnerTable)
        val accountId = reference("account_id", BankAccountTable)

        init {
            uniqueIndex(ownerId, accountId)
        }
    }

    /**
     * 은행 계좌
     */
    class BankAccount(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<BankAccount>(BankAccountTable)

        var number: String by BankAccountTable.number
        val owners: SizedIterable<AccountOwner> by AccountOwner via OwnerAccountMapTable  // many-to-many

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("number", number)
            .toString()
    }

    /**
     * 계좌 소유자
     */
    class AccountOwner(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<AccountOwner>(AccountOwnerTable)

        var ssn: String by AccountOwnerTable.ssn
        val accounts: SizedIterable<BankAccount> by BankAccount via OwnerAccountMapTable // many-to-many

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("ssn", ssn)
            .toString()
    }
}
