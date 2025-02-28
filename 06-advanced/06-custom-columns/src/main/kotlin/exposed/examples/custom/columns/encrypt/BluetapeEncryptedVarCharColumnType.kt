package exposed.examples.custom.columns.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.VarCharColumnType

class BluetapeEncryptedVarCharColumnType(
    length: Int,
    encryptor: Encryptor = Encryptors.AES,
): ColumnWithTransform<String, String>(VarCharColumnType(length), BluetapeStringEncryptionTransformer(encryptor))

class BluetapeStringEncryptionTransformer(
    private val encryptor: Encryptor,
): ColumnTransformer<String, String> {

    /**
     * Entity 문자열 속성을 암호화하여 문자열로 저장합니다.
     */
    override fun unwrap(value: String): String = encryptor.encrypt(value)

    /**
     * DB Column 값을 복호화하여 Entity의 문자열 속성으로 변환합니다.
     */
    override fun wrap(value: String): String = encryptor.decrypt(value)
}
