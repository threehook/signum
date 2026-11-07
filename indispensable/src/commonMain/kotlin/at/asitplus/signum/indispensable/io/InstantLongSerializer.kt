package at.asitplus.signum.indispensable.io

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.floor
import kotlin.time.Instant

/**
 * Serializes [Instant] as whole seconds from the Unix epoch.
 *
 * In JSON this is a NumericDate encoded as a JSON number (RFC 7519, Section 2). Reading is lenient there: RFC 7519
 * allows "Non-integer values", which some implementations send (for example `"iat": 1794052302.015773`), so a fraction
 * is accepted and rounded down to whole seconds. Writing always produces whole seconds.
 * In CBOR this is a CWT NumericDate encoded as an untagged CBOR numeric date, with
 * CBOR tag 1 omitted (RFC 8392, Section 2; RFC 8949, Section 3.4.2).
 */
object InstantLongSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantLongSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant =
        if (decoder is JsonDecoder) Instant.fromEpochSeconds(decoder.decodeJsonElement().jsonPrimitive.epochSeconds())
        else Instant.fromEpochSeconds(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.epochSeconds)
    }

    /** A whole number as is, a fractional one rounded down; a string or anything else that is no number is rejected. */
    private fun JsonPrimitive.epochSeconds(): Long {
        if (isString) throw SerializationException("NumericDate must be a JSON number, but is the string '$content'")
        return longOrNull
            ?: doubleOrNull?.takeIf { it.isFinite() }?.let { floor(it).toLong() }
            ?: throw SerializationException("NumericDate must be a JSON number, but is '$content'")
    }
}
