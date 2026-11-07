package at.asitplus.signum.indispensable.josef

import at.asitplus.testballoon.matrix.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.time.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// RFC 7519, Section 2: a NumericDate is "A JSON numeric value representing the number of seconds from
// 1970-01-01T00:00:00Z UTC until the specified UTC date/time [...] Non-integer values can be represented."
val FractionalNumericDateTest by matrixSuite {

    // The claims of a client attestation PoP JWT as the EUDI Wallet for iOS sends them.
    val input = """
        {
          "iss": "eudiw-abca",
          "aud": "https://issuer.example",
          "iat": 1794052302.015773,
          "nbf": 1794052300.9,
          "exp": 1794052602.015774,
          "jti": "e75UDUifmmgMvsi2Y5xT"
        }
    """.trimIndent()

    "NumericDate claims with a fractional part are read, rounded down to whole seconds" {
        val parsed: JsonWebToken = Json.decodeFromString(input)

        parsed.issuedAt shouldBe Instant.fromEpochSeconds(1794052302)
        parsed.notBefore shouldBe Instant.fromEpochSeconds(1794052300)
        parsed.expiration shouldBe Instant.fromEpochSeconds(1794052602)
    }

    "NumericDate claims are still written as whole seconds" {
        val parsed: JsonWebToken = Json.decodeFromString(input)

        val written = Json.encodeToString(parsed)

        Json.decodeFromString<JsonWebToken>(written) shouldBe parsed
        written.contains("\"iat\":1794052302,") shouldBe true
    }

    "NumericDate claims with whole seconds are read as before" {
        val parsed: JsonWebToken = Json.decodeFromString("""{"iat":1300815780,"exp":1300902179}""")

        parsed.issuedAt shouldBe Instant.fromEpochSeconds(1300815780)
        parsed.expiration shouldBe Instant.fromEpochSeconds(1300902179)
    }

    "A NumericDate that is a string is still rejected" {
        shouldThrow<SerializationException> {
            Json.decodeFromString<JsonWebToken>("""{"iat":"1794052302.5"}""")
        }
    }
}
