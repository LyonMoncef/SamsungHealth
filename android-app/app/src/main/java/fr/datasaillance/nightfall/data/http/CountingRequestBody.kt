package fr.datasaillance.nightfall.data.http

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

class CountingRequestBody(
    private val delegate: RequestBody,
    private val totalBytes: Long,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = totalBytes

    override fun writeTo(sink: BufferedSink) {
        onProgress(0f)
        val counted = CountingSink(sink)
        val buffered = counted.buffer()
        delegate.writeTo(buffered)
        buffered.flush()
    }

    inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            onProgress((bytesWritten.toFloat() / totalBytes).coerceIn(0f, 1f))
        }
    }
}
