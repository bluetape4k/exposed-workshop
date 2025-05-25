package exposed.examples.jpa.ex03_customId

import org.jetbrains.exposed.v1.core.CharColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType
import java.io.Serializable

/**
 * `value class` 를 이용하여, 컬럼의 타입에 타입 안정성을 제공할 수 있습니다.
 */
@JvmInline
value class Email(val value: String = EMPTY.value): Comparable<Email>, Serializable {
    companion object {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".toRegex()
        val EMPTY = Email("")
    }

    val isValid: Boolean
        get() = value.isNotEmpty() && emailRegex.matches(value)

    val isEmpty: Boolean
        get() = value.isEmpty()

    override fun compareTo(other: Email): Int = value.compareTo(other.value)
}

fun Table.email(name: String, length: Int = 64): Column<Email> =
    registerColumn(name, EmailColumnType(length))

open class EmailColumnType(length: Int = 64):
    ColumnWithTransform<String, Email>(VarCharColumnType(length), StringToEmailTransformer())

class StringToEmailTransformer: ColumnTransformer<String, Email> {
    override fun unwrap(value: Email): String = value.value
    override fun wrap(value: String): Email = Email(value)
}


@JvmInline
value class Ssn(val value: String): Serializable, Comparable<Ssn> {
    companion object {
        val ssnRegex = "^(\\d{6})(\\d{7})$".toRegex()
        val EMPTY = Ssn("")
        const val SSN_LENGTH = 14
    }

    val isValid: Boolean
        get() = value.isNotEmpty() && ssnRegex.matches(value)

    override fun compareTo(other: Ssn): Int = value.compareTo(other.value)
}

fun Table.ssn(name: String, length: Int = 14): Column<Ssn> =
    registerColumn(name, SsnColumnType(length))

open class SsnColumnType(
    length: Int = Ssn.SSN_LENGTH,
): ColumnWithTransform<String, Ssn>(CharColumnType(length), StringToSsnTransformer())

class StringToSsnTransformer: ColumnTransformer<String, Ssn> {
    override fun unwrap(value: Ssn): String = value.value
    override fun wrap(value: String): Ssn = Ssn(value)
}
