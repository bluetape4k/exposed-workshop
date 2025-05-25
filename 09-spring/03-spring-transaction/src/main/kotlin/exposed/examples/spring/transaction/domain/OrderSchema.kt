package exposed.examples.spring.transaction.domain

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.*

object OrderSchema {

    object CustomerTable: UUIDTable("customers") {
        val name: Column<String> = varchar("name", 255).uniqueIndex()
        val mobile: Column<String?> = varchar("mobile", 255).nullable()
    }

    object OrderTable: UUIDTable("orders") {
        val customerId: Column<EntityID<UUID>> = reference("customer_id", CustomerTable)
        val productName: Column<String> = varchar("product_name", 255)
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

        var customer: CustomerEntity by CustomerEntity referencedOn OrderTable.customerId  // many-to-one
        var productName: String by OrderTable.productName

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("customer", customer)
            .add("productName", productName)
            .toString()
    }
}
