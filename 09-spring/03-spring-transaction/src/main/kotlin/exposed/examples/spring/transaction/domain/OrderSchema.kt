package exposed.examples.spring.transaction.domain

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object OrderSchema {

    object CustomerTable: UUIDTable("customers") {
        val name = varchar("name", 255).uniqueIndex()
        val mobile = varchar("mobile", 255).nullable()
    }

    object OrderTable: UUIDTable("orders") {
        val customer = reference("customer", CustomerTable)
        val productName = varchar("product_name", 255)
    }

    class CustomerEntity(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<CustomerEntity>(CustomerTable)

        var name: String by CustomerTable.name
        var mobile: String? by CustomerTable.mobile

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("mobile", mobile)
            .toString()
    }

    class OrderEntity(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<OrderEntity>(OrderTable)

        var customer: CustomerEntity by CustomerEntity referencedOn OrderTable.customer  // many-to-one
        var productName: String by OrderTable.productName

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("customer", customer)
            .add("productName", productName)
            .toString()
    }
}
