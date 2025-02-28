package exposed.examples.custom.columns.serialization

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * 컬럼 값을 [BinarySerializer] 를 이용해 직렬화/역직렬화하여 Binary Column 에 저장할 수 있는 Column 을 생성합니다.
 */
fun <T: Any> Table.binarySerializedBinary(
    name: String,
    length: Int,
    serializer: BinarySerializer = BinarySerializers.Fury,
): Column<T> =
    registerColumn(name, BinarySerializedBinaryColumnType(length, serializer))

fun <T: Any> Table.binarySerializedBlob(
    name: String,
    serializer: BinarySerializer = BinarySerializers.Fury,
): Column<T> =
    registerColumn(
        name, BinarySerializedBlobColumnType(serializer)
    )
