package exposed.examples.spring.transaction.service

import exposed.examples.spring.transaction.domain.OrderSchema.CustomerEntity
import exposed.examples.spring.transaction.domain.OrderSchema.CustomerTable
import exposed.examples.spring.transaction.domain.OrderSchema.OrderEntity
import exposed.examples.spring.transaction.domain.OrderSchema.OrderTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OrderService {

    companion object: KLogging()

    fun init() {
        log.info { "스키마 생성. ${CustomerTable.tableName}, ${OrderTable.tableName}" }
        SchemaUtils.create(CustomerTable, OrderTable)
    }

    fun createCustomer(name: String, mobile: String? = null): CustomerEntity {
        log.debug { "고객 생성. name=$name" }
        return CustomerEntity.new {
            this.name = name
            this.mobile = mobile
        }
    }

    fun createOrder(customer: CustomerEntity, productName: String): OrderEntity {
        log.debug { "주문 생성. customer=${customer.name}, productName=$productName" }

        return OrderEntity.new {
            this.customer = customer
            this.productName = productName
        }
    }

    fun doBoth(customerName: String, productName: String, customerMobile: String? = null): OrderEntity {
        val customer = createCustomer(customerName, customerMobile)
        return createOrder(customer, productName)
    }

    fun findOrderByProductName(productName: String): OrderEntity? {
        return OrderEntity.find { OrderTable.productName eq productName }.firstOrNull()
    }

    fun transaction(block: () -> Unit) {
        block()
    }

    fun cleanUp() {
        log.info { "스키마 삭제. ${CustomerTable.tableName}, ${OrderTable.tableName}" }
        SchemaUtils.drop(CustomerTable, OrderTable)
    }

}
