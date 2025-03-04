package exposed.shared.samples

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object AccountTable: LongIdTable("accounts") {

    val bankName = varchar("name", 255)
    val accountNumber = varchar("account_number", 255)
    val balance = decimal("balance", 10, 2)

    val owner = reference("owner_id", OwnerTable.id)
}

object OwnerTable: LongIdTable("owners") {

    val name = varchar("name", 255)
    val ssn = varchar("ssn", 255)
    val email = varchar("email", 255).nullable()

    val accounts = reference("account_id", AccountTable.id, ReferenceOption.CASCADE)
}

class AccountEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<AccountEntity>(AccountTable)

    var bankName by AccountTable.bankName
    var accountNumber by AccountTable.accountNumber
    var balance by AccountTable.balance

    var owner by OwnerEntity referencedOn AccountTable.owner

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = toStringBuilder()
        .add("bankNname", bankName)
        .add("accountNumber", accountNumber)
        .add("balance", balance)
        .toString()
}

class OwnerEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<OwnerEntity>(OwnerTable)

    var name by OwnerTable.name
    var ssn by OwnerTable.ssn
    var email by OwnerTable.email

    val accounts by AccountEntity referrersOn OwnerTable.accounts

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = toStringBuilder()
        .add("name", name)
        .add("ssn", ssn)
        .add("email", email)
        .toString()
}
