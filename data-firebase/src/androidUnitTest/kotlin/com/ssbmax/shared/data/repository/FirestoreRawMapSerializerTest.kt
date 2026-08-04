package com.ssbmax.shared.data.repository

import dev.gitlive.firebase.internal.FirebaseDecoder
import dev.gitlive.firebase.internal.FirebaseEncoder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves [FirestoreRawMapSerializer] actually decodes/encodes a real Firestore-shaped document
 * without a live backend. [FirebaseDecoder]/[FirebaseEncoder] are constructible directly (public
 * classes, `dev.gitlive.firebase.internal`) and hold the exact raw structure GitLive's own
 * `DocumentSnapshot.data(strategy)`/`DocumentReference.set(strategy, data)` would pass them —
 * verified in `FirestoreRawMapSerializer`'s class doc — so building one by hand with a fixture map
 * exercises the real decode/encode path this fix relies on, the same way Android's
 * `DocumentSnapshot.getData()` would have produced it.
 */
class FirestoreRawMapSerializerTest {

    @Test
    fun `deserialize reads a nested Firestore-shaped document into Map with String keys`() {
        val firestoreShapedDocument: Map<String, Any?> = mapOf(
            "id" to "sub-1",
            "userId" to "user-1",
            "submittedAt" to 1_700_000_000_000L,
            "score" to 7.5,
            "passed" to true,
            "data" to mapOf(
                "analysisStatus" to "COMPLETED",
                "tags" to listOf("a", "b", "c")
            )
        )
        val decoder = FirebaseDecoder(firestoreShapedDocument)

        val result = FirestoreRawMapSerializer.deserialize(decoder)

        assertEquals("sub-1", result["id"])
        assertEquals("user-1", result["userId"])
        assertEquals(1_700_000_000_000L, result["submittedAt"])
        assertEquals(true, result["passed"])
        @Suppress("UNCHECKED_CAST")
        val nested = result["data"] as Map<String, Any?>
        assertEquals("COMPLETED", nested["analysisStatus"])
        @Suppress("UNCHECKED_CAST")
        val tags = nested["tags"] as List<String>
        assertEquals(listOf("a", "b", "c"), tags)
    }

    @Test
    fun `deserialize of a missing document with a null value returns an empty map not a crash`() {
        val decoder = FirebaseDecoder(null)
        assertEquals(emptyMap(), FirestoreRawMapSerializer.deserialize(decoder))
    }

    @Test
    fun `serialize then deserialize round-trips a raw map through FirebaseEncoder and FirebaseDecoder`() {
        val original: Map<String, Any> = mapOf(
            "status" to "SUBMITTED_PENDING_REVIEW",
            "batchId" to "batch-42",
            "gradingTimestamp" to 42L
        )

        val encoder = FirebaseEncoder(shouldEncodeElementDefault = true)
        FirestoreRawMapSerializer.serialize(encoder, original)
        val encodedValue = encoder.value

        val roundTripped = FirestoreRawMapSerializer.deserialize(FirebaseDecoder(encodedValue))

        assertEquals(original, roundTripped)
    }

    @Test
    fun `deserialize rejects a non-FirebaseDecoder instead of silently returning garbage`() {
        val notAFirebaseDecoder = object : kotlinx.serialization.encoding.Decoder {
            override val serializersModule = kotlinx.serialization.modules.EmptySerializersModule()
            override fun decodeBoolean() = throw NotImplementedError()
            override fun decodeByte() = throw NotImplementedError()
            override fun decodeChar() = throw NotImplementedError()
            override fun decodeDouble() = throw NotImplementedError()
            override fun decodeEnum(enumDescriptor: kotlinx.serialization.descriptors.SerialDescriptor) = throw NotImplementedError()
            override fun decodeFloat() = throw NotImplementedError()
            override fun decodeInline(descriptor: kotlinx.serialization.descriptors.SerialDescriptor) = throw NotImplementedError()
            override fun decodeInt() = throw NotImplementedError()
            override fun decodeLong() = throw NotImplementedError()
            override fun decodeNotNullMark() = throw NotImplementedError()
            override fun decodeNull(): Nothing? = throw NotImplementedError()
            override fun decodeShort() = throw NotImplementedError()
            override fun decodeString() = throw NotImplementedError()
            override fun beginStructure(descriptor: kotlinx.serialization.descriptors.SerialDescriptor) = throw NotImplementedError()
        }
        val thrown = runCatching { FirestoreRawMapSerializer.deserialize(notAFirebaseDecoder) }.exceptionOrNull()
        assertEquals("FirestoreRawMapSerializer only supports GitLive's FirebaseDecoder, got ${notAFirebaseDecoder::class}", thrown?.message)
    }
}
