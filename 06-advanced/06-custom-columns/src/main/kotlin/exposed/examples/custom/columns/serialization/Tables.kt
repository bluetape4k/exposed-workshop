package exposed.examples.custom.columns.serialization

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

/**
 * 컬럼 값을 [BinarySerializer] 를 이용해 직렬화/역직렬화하여 Binary Column 에 저장할 수 있는 Column 을 생성합니다.
 */
fun <T: Any> Table.binarySerializedBinary(
    name: String,
    length: Int,
    serializer: BinarySerializer = BinarySerializers.Fory,
): Column<T> =
    registerColumn(name, BinarySerializedBinaryColumnType(length, serializer))

/**
 * 컬럼 값을 [BinarySerializer] 를 이용해 직렬화/역직렬화하여 Blob 에 저장할 수 있는 Column 을 생성합니다.
 */
fun <T: Any> Table.binarySerializedBlob(
    name: String,
    serializer: BinarySerializer = BinarySerializers.Fory,
): Column<T> =
    registerColumn(name, BinarySerializedBlobColumnType(serializer))
