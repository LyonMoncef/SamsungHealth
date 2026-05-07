---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/CountingRequestBody.kt
git_blob: 7091f8de03e5834851813753c187f5f263531c1c
last_synced: '2026-05-07T03:10:49Z'
loc: 38
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/CountingRequestBody.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/CountingRequestBody.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/CountingRequestBody.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `CountingRequestBody` (class) — lines 11-38
- `contentType` (function) — lines 17-17
- `contentLength` (function) — lines 19-19
- `writeTo` (function) — lines 21-27
- `CountingSink` (class) — lines 29-37
- `write` (function) — lines 32-36
