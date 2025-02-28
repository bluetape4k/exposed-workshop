package exposed.examples.custom.columns.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.sql.BinaryColumnType
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform

class BluetapeEncryptedBinaryColumnType(
    length: Int,
    encryptor: Encryptor = Encryptors.AES,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), BluetapeBinaryEncryptionTransformer(encryptor))

class BluetapeBinaryEncryptionTransformer(
    private val encryptor: Encryptor,
): ColumnTransformer<ByteArray, ByteArray> {
    /**
     * Entity ByteArray 속성을 암호화하여 ByteArray 로 저장합니다.
     */
    override fun unwrap(value: ByteArray): ByteArray = encryptor.encrypt(value)

    /**
     * DB Column 값을 복호화하여 Entity의 ByteArray 속성으로 변환합니다.
     */
    override fun wrap(value: ByteArray): ByteArray = encryptor.decrypt(value)
}
