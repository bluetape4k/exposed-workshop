package alternative.vertx.sqlclient.example.model

import io.vertx.sqlclient.templates.RowMapper
import io.vertx.sqlclient.templates.TupleMapper
import java.io.Serializable

data class Customer(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val mobile: String? = null,
    val age: Int? = null,
): Serializable

val CustomerRowMapper: RowMapper<Customer> = RowMapper { row ->
    Customer(
        id = row.getLong("id"),
        firstName = row.getString("first_name"),
        lastName = row.getString("last_name"),
        email = row.getString("email"),
        mobile = row.getString("mobile"),
        age = row.getInteger("age"),
    )
}

val CustomerTupleMapper: TupleMapper<Customer> = TupleMapper.mapper { customer ->
    // 직접 매핑하는 방식
    mapOf(
        "id" to customer.id,
        "first_name" to customer.firstName,
        "last_name" to customer.lastName,
        "email" to customer.email,
        "mobile" to customer.mobile,
        "age" to customer.age,
    )

    // Reflection 을 이용하는 방식
    // Customer::class.memberProperties.associate {
    //    it.name to it.get(customer)
    // }
}
