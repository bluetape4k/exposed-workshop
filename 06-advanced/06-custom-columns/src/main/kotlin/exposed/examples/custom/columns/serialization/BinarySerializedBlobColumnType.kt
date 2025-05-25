package exposed.examples.custom.columns.serialization

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

class BinarySerializedBlobColumnType<T: Any>(
    serializer: BinarySerializer = BinarySerializers.Fury,
): ColumnWithTransform<ExposedBlob, T>(BlobColumnType(), BinarySerializedBlobTransformer(serializer))

class BinarySerializedBlobTransformer<T>(
    private val serializer: BinarySerializer,
): ColumnTransformer<ExposedBlob, T> {
    /**
     * Entity Property 를 [BinarySerializer]를 이용해 직렬화하여 DB Blob Column 에 저장합니다.
     */
    override fun unwrap(value: T): ExposedBlob = ExposedBlob(serializer.serialize(value))

    /**
     * DB Blob Column 값을 [BinarySerializer]를 이용해 역직렬화 하여 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ExposedBlob): T = serializer.deserialize(value.bytes)!!
}
