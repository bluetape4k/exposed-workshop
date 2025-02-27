package exposed.examples.money

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.money.CompositeMoneyColumn
import org.jetbrains.exposed.sql.money.compositeMoney
import org.jetbrains.exposed.sql.money.nullable
import java.math.BigDecimal
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

internal const val AMOUNT_SCALE = 5

/**
 * `Money` 를 나타내는 [MonetaryAmount] 를 저장하는 테이블을 정의합니다.
 *
 * [MonetaryAmount] 는 [BigDecimal] 과 [CurrencyUnit] 으로 구성되어 있습니다.
 *
 * ```sql
 * -- Postgres
 * CREATE TABLE IF NOT EXISTS accounts (
 *      id SERIAL PRIMARY KEY,
 *      composite_money DECIMAL(8, 5) NULL,     -- currency amount
 *      "composite_money_C" VARCHAR(3) NULL     -- currency unit
 * );
 * ```
 */
internal object AccountTable: IntIdTable("Accounts") {
    val composite_money: CompositeMoneyColumn<BigDecimal?, CurrencyUnit?, MonetaryAmount?> =
        compositeMoney(8, AMOUNT_SCALE, "composite_money").nullable()
}

internal class AccountEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: EntityClass<Int, AccountEntity>(AccountTable)

    val money: MonetaryAmount? by AccountTable.composite_money

    val amount: BigDecimal? by AccountTable.composite_money.amount
    val currency: CurrencyUnit? by AccountTable.composite_money.currency

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = toStringBuilder()
        .add("money", money)
        .toString()
}
