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
 *      composite_money DECIMAL(8, 5) NULL,       -- amount
 *      "composite_money_C" VARCHAR(3) NULL       -- currency
 * )
 *
 * CREATE INDEX ix_money_amount ON accounts (composite_money)
 * ```
 *
 * ```sql
 * -- MySQL
 * CREATE TABLE IF NOT EXISTS Accounts (
 *      id INT AUTO_INCREMENT PRIMARY KEY,
 *      composite_money DECIMAL(8, 5) NULL,
 *      composite_money_C VARCHAR(3) NULL
 * )
 *
 * CREATE INDEX ix_amount ON Accounts (composite_money)
 * ```
 */
internal object AccountTable: IntIdTable("Accounts") {
    val composite_money: CompositeMoneyColumn<BigDecimal?, CurrencyUnit?, MonetaryAmount?> =
        compositeMoney(8, AMOUNT_SCALE, "composite_money").nullable()

    init {
        index("ix_money_amount", false, composite_money.amount)
    }
}

internal class AccountEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: EntityClass<Int, AccountEntity>(AccountTable)

    var money: MonetaryAmount? by AccountTable.composite_money

    // `money` 의 2가지 속성을 따로 접근하게 합니다.
    val amount: BigDecimal? by AccountTable.composite_money.amount
    val currency: CurrencyUnit? by AccountTable.composite_money.currency

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = toStringBuilder()
        .add("money", money)
        .toString()
}
