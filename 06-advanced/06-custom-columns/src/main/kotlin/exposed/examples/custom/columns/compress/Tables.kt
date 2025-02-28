package exposed.examples.custom.columns.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * 엔티티 속성 값을 압축하여 VARBINARY Column 으로 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.compressedBinary(
    name: String,
    length: Int,
    compressor: Compressor = Compressors.LZ4,
): Column<ByteArray> =
    registerColumn(name, CompressedBinaryColumnType(length, compressor))

/**
 * 엔티티 속성 값을 압축하여 BLOB Column 으로 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.compressedBlob(
    name: String,
    compressor: Compressor = Compressors.LZ4,
): Column<ByteArray> =
    registerColumn(name, CompressedBlobColumnType(compressor))
