package exposed.examples.jpa.ex02_customId

import org.jetbrains.exposed.sql.CharColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import java.io.Serializable


@JvmInline
value class Email(val value: String): Serializable {
    companion object {
        val EMPTY = Email("")
    }
}

fun Table.email(name: String, length: Int = 64): Column<Email> =
    registerColumn(name, EmailColumnType(length))

open class EmailColumnType(val length: Int = 64):
    ColumnWithTransform<String, Email>(VarCharColumnType(length), StringToEmailTransformer())

class StringToEmailTransformer: ColumnTransformer<String, Email> {
    override fun unwrap(email: Email): String = email.value
    override fun wrap(value: String): Email = Email(value)
}


@JvmInline
value class Ssn(val value: String): Serializable {
    companion object {
        val EMPTY = Ssn("")
        const val SSN_LENGTH = 14
    }
}

fun Table.ssn(name: String, length: Int = 14): Column<Ssn> =
    registerColumn(name, SsnColumnType(length))

open class SsnColumnType(
    val length: Int = Ssn.SSN_LENGTH,
): ColumnWithTransform<String, Ssn>(CharColumnType(length), StringToSsnTransformer())

class StringToSsnTransformer: ColumnTransformer<String, Ssn> {
    override fun unwrap(ssn: Ssn): String = ssn.value
    override fun wrap(value: String): Ssn = Ssn(value)
}
