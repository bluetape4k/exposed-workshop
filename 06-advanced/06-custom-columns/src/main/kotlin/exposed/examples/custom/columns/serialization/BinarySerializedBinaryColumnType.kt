package exposed.examples.custom.columns.serialization

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform

class BinarySerializedBinaryColumnType<T: Any>(
    length: Int,
    serializer: BinarySerializer = BinarySerializers.Fury,
): ColumnWithTransform<ByteArray, T>(BinaryColumnType(length), BinarySerializedBinaryTransformer(serializer))

class BinarySerializedBinaryTransformer<T>(
    private val serializer: BinarySerializer,
): ColumnTransformer<ByteArray, T> {
    /**
     * Entity Property 를 [BinarySerializer]를 이용해 직렬화하여 DB Binary Column 에 저장합니다.
     */
    override fun unwrap(value: T): ByteArray = serializer.serialize(value)

    /**
     * DB Column 값을 [BinarySerializer]를 이용해 역직렬화 하여 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ByteArray): T = serializer.deserialize(value)!!
}
