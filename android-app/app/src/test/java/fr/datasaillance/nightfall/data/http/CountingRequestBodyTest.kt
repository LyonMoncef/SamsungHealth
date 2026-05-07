package fr.datasaillance.nightfall.data.http

// spec: Tests d'acceptation TA-06
// spec: section "CountingRequestBody" + "Architecture" + "D3 / D9"
// RED by construction: fr.datasaillance.nightfall.data.http.CountingRequestBody does not exist yet

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// This import will fail to resolve until production code is written:
// fr.datasaillance.nightfall.data.http.CountingRequestBody

class CountingRequestBodyTest {

    // spec: TA-06 — onProgress is called at least twice when writing in chunks
    @Test
    fun writeTo_onProgressCalledAtLeastTwice() {
        val content = ByteArray(1000) { 0x42.toByte() }
        val delegate = content.toRequestBody("text/csv".toMediaType())
        val progressValues = mutableListOf<Float>()

        val counting = CountingRequestBody(
            delegate = delegate,
            totalBytes = 1000L,
            onProgress = { progressValues.add(it) },
        )

        val sink = Buffer()
        counting.writeTo(sink)

        // spec: TA-06 — onProgress must be invoked at least twice (at least one intermediate + final)
        assert(progressValues.size >= 2) {
            "onProgress must be called at least twice during writeTo — spec: TA-06, called ${progressValues.size} times"
        }
    }

    // spec: TA-06 — last onProgress value is 1.0f
    @Test
    fun writeTo_lastProgressValueIsOne() {
        val content = ByteArray(1000) { 0x42.toByte() }
        val delegate = content.toRequestBody("text/csv".toMediaType())
        val progressValues = mutableListOf<Float>()

        val counting = CountingRequestBody(
            delegate = delegate,
            totalBytes = 1000L,
            onProgress = { progressValues.add(it) },
        )

        val sink = Buffer()
        counting.writeTo(sink)

        // spec: TA-06 — the last callback value must be exactly 1.0f
        val lastProgress = progressValues.last()
        assertEquals(
            "Last onProgress value must be 1.0f — spec: TA-06",
            1.0f,
            lastProgress,
            0.0001f,
        )
    }

    // spec: TA-06 — no onProgress value exceeds 1.0f
    @Test
    fun writeTo_noProgressValueExceedsOne() {
        val content = ByteArray(1000) { 0x42.toByte() }
        val delegate = content.toRequestBody("text/csv".toMediaType())
        val progressValues = mutableListOf<Float>()

        val counting = CountingRequestBody(
            delegate = delegate,
            totalBytes = 1000L,
            onProgress = { progressValues.add(it) },
        )

        val sink = Buffer()
        counting.writeTo(sink)

        // spec: TA-06 — progress is coerced via coerceIn(0f, 1f); no value must exceed 1.0f
        val exceeded = progressValues.filter { it > 1.0f }
        assertTrue(
            "No onProgress value must exceed 1.0f — spec: TA-06, violations: $exceeded",
            exceeded.isEmpty(),
        )
    }

    // spec: D9 / TA-06 — contentLength() returns totalBytes
    @Test
    fun contentLength_returnsTotalBytes() {
        val content = ByteArray(500) { 0x00.toByte() }
        val delegate = content.toRequestBody("text/csv".toMediaType())
        val totalBytes = 1234L  // intentionally different from content size to verify delegation

        val counting = CountingRequestBody(
            delegate = delegate,
            totalBytes = totalBytes,
            onProgress = {},
        )

        // spec: D9 — contentLength() must return the totalBytes constructor parameter, not delegate.contentLength()
        assertEquals(
            "contentLength() must return totalBytes passed to constructor — spec: D9 / TA-06",
            totalBytes,
            counting.contentLength(),
        )
    }

    // spec: TA-06 — contentType() delegates to the wrapped RequestBody
    @Test
    fun contentType_delegatesToWrappedBody() {
        val content = ByteArray(100) { 0x00.toByte() }
        val mediaType = "text/csv".toMediaType()
        val delegate = content.toRequestBody(mediaType)

        val counting = CountingRequestBody(
            delegate = delegate,
            totalBytes = 100L,
            onProgress = {},
        )

        // spec: TA-06 — contentType() must return the same MediaType as the delegate
        val returned = counting.contentType()
        assertEquals(
            "contentType() must delegate to wrapped RequestBody — spec: TA-06",
            mediaType.toString(),
            returned?.toString(),
        )
    }

    // spec: TA-06 — all bytes are written to the sink (no truncation)
    @Test
    fun writeTo_allBytesWrittenToSink() {
        val content = ByteArray(1000) { it.toByte() }
        val delegate = content.toRequestBody("text/csv".toMediaType())

        val counting = CountingRequestBody(
            delegate = delegate,
            totalBytes = 1000L,
            onProgress = {},
        )

        val sink = Buffer()
        counting.writeTo(sink)

        // spec: D3 — streaming must not truncate bytes; all content must be written
        assertEquals(
            "writeTo must write all bytes to sink — spec: D3 / TA-06",
            1000L,
            sink.size,
        )
    }

    // spec: TA-06 (edge) — progress values are monotonically non-decreasing
    @Test
    fun writeTo_progressIsMonotonicallyNonDecreasing() {
        val content = ByteArray(1000) { 0x42.toByte() }
        val delegate = content.toRequestBody("text/csv".toMediaType())
        val progressValues = mutableListOf<Float>()

        val counting = CountingRequestBody(
            delegate = delegate,
            totalBytes = 1000L,
            onProgress = { progressValues.add(it) },
        )

        val sink = Buffer()
        counting.writeTo(sink)

        // spec: TA-06 — each progress value must be >= the previous one (monotonic progress)
        for (i in 1 until progressValues.size) {
            assertTrue(
                "onProgress values must be non-decreasing — spec: TA-06, index $i: ${progressValues[i - 1]} → ${progressValues[i]}",
                progressValues[i] >= progressValues[i - 1],
            )
        }
    }
}
