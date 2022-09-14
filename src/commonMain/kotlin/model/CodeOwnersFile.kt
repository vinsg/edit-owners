package model

import io.ktor.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okio.ByteString.Companion.decodeBase64

@Serializable
data class CodeOwnersFile(
    @Serializable(with = Base64ContentSerializer::class)
    val content: String,
    val sha: String
)

/**
 * Decode Base64 file content.
 */
object Base64ContentSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Content", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) =
        decoder.decodeString().decodeBase64()?.toByteArray()?.decodeToString()?.trim() ?: ""

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value.encodeBase64())
}
