package exposed.examples.custom.columns.compress

import io.bluetape4k.exposed.sql.compress.CompressedBinaryTransformer
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform

/**
 * 주어진 `compressor` 를 이용하여, 압축하여 DB ByteArray 컬럼에 저장하고,
 * 압축 해제하여 Column 값으로 읽어오는 Column Type 입니다.
 */
class CompressedBinaryColumnType(
    length: Int,
    compressor: Compressor = Compressors.LZ4,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), CompressedBinaryTransformer(compressor)) {

}

class CompressedBinaryTransformer(
    private val compressor: Compressor,
): ColumnTransformer<ByteArray, ByteArray> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: ByteArray): ByteArray = compressor.compress(value)

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ByteArray): ByteArray = compressor.decompress(value)
}
