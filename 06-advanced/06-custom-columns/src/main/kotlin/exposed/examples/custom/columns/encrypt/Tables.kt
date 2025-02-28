package exposed.examples.custom.columns.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

fun Table.bluetapeEncryptedBinary(
    name: String,
    length: Int = 255,
    encryptor: Encryptor = Encryptors.AES,
): Column<ByteArray> =
    registerColumn(name, BluetapeEncryptedBinaryColumnType(length, encryptor))

fun Table.bluetapeEncryptedVarChar(
    name: String,
    length: Int = 255,
    encryptor: Encryptor = Encryptors.AES,
): Column<String> =
    registerColumn(name, BluetapeEncryptedVarCharColumnType(length, encryptor))
