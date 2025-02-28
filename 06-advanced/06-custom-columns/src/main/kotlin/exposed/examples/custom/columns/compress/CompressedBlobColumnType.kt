package exposed.examples.custom.columns.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.sql.BlobColumnType
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

/**
 * 주어진 `compressor` 를 이용하여, 압축하여 DB Blob 컬럼에 저장하고,
 * 압축 해제하여 Column 값으로 읽어오는 Column Type 입니다.
 */
class CompressedBlobColumnType(
    Compressor: Compressor = Compressors.LZ4,
): ColumnWithTransform<ExposedBlob, ByteArray>(BlobColumnType(), CompressedBlobTransformer(Compressor))

class CompressedBlobTransformer(
    private val compressor: Compressor,
): ColumnTransformer<ExposedBlob, ByteArray> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: ByteArray): ExposedBlob = ExposedBlob(compressor.compress(value))

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ExposedBlob): ByteArray = compressor.decompress(value.bytes)
}
