package io.github.qqclutchyqq.crossroad.android

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ContentFile(val path: String, val size: Long)

/** Synchronous dedicated-worker I/O, never a UI-thread network call. Read-only. */
class ContentRanges(
    private val directory: File,
    private val baseUrl: String,
    private val version: String,
    files: List<ContentFile>,
    private val connect: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val maxBlocks: Int = 256
) {
    private val filesByPath = files.associateBy { it.path }
    private val blockSize = 1048576L
    init {
        require(URL(baseUrl).protocol == "https")
        require(version.matches(Regex("[A-Za-z0-9._-]+")))
        require(files.all { file ->
            file.path.startsWith("Content/") && file.path.split('/').all {
                it.matches(Regex("[A-Za-z0-9._-]+")) && it != "." && it != ".."
            } && file.size > 0
        }) { "Unsafe Content manifest path" }
        require(maxBlocks > 0)
        directory.mkdirs()
    }

    @Synchronized fun read(path: String?, offsetText: String?, requestedVersion: String?): ByteArray {
        require(requestedVersion == version) { "Content version mismatch" }
        val file = filesByPath[path] ?: error("Unknown Content path")
        val offset = offsetText?.toLongOrNull() ?: error("Invalid offset")
        require(offset >= 0 && offset < file.size && offset % blockSize == 0L) { "Invalid range" }
        val end = minOf(file.size, offset + blockSize) - 1
        val length = (end - offset + 1).toInt()
        val pathKey = MessageDigest.getInstance("SHA-256").digest(file.path.toByteArray()).joinToString("") { "%02x".format(it) }
        val target = File(directory, "$pathKey-${offset}.block")
        if (target.isFile && target.length() == length.toLong()) {
            target.setLastModified(System.currentTimeMillis())
            return target.readBytes()
        }
        val connection = connect(URL(baseUrl + file.path))
        val bytes: ByteArray
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Range", "bytes=$offset-$end")
            connection.setRequestProperty("Accept-Encoding", "identity")
            check(connection.responseCode == 206) { "Content HTTP ${connection.responseCode}; expected partial response" }
            check(connection.getHeaderField("Content-Range") == "bytes $offset-$end/${file.size}") { "Content-Range mismatch" }
            // Never consume a full multi-GB response or trust Content-Length alone.
            bytes = connection.inputStream.use { input ->
                val result = ByteArray(length)
                var position = 0
                while (position < length) {
                    val count = input.read(result, position, length - position)
                    check(count > 0) { "Truncated Content block" }
                    position += count
                }
                check(input.read() == -1) { "Oversized Content block" }
                result
            }
        } finally { connection.disconnect() }
        // Cache failure must not prevent an otherwise valid online read.
        try {
            val entries = directory.listFiles()?.filter { it.extension == "block" }?.sortedBy { it.lastModified() }.orEmpty()
            entries.take((entries.size - maxBlocks + 1).coerceAtLeast(0)).forEach { check(it.delete()) }
            val temporary = File(directory, target.name + ".tmp")
            temporary.writeBytes(bytes)
            check(temporary.renameTo(target))
        } catch (_: Exception) { /* App-private cache may be full/evicted. Saves are separate. */ }
        return bytes
    }
}
