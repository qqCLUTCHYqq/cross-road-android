package io.github.qqclutchyqq.crossroad.android

import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

class ContentRangesTest {
    @Test fun acceptsNestedManifestEntriesButRejectsTraversal() {
        val directory = Files.createTempDirectory("manifest-test").toFile()
        try {
            ContentRanges(directory, "https://example.invalid/", "v1", listOf(ContentFile("Content/misc_mp4/info/version", 9), ContentFile("Content/extra_mp4/darkBook", 29962)))
            assertThrows(IllegalArgumentException::class.java) { ContentRanges(directory, "https://example.invalid/", "v1", listOf(ContentFile("Content/../secret", 9))) }
        } finally { directory.deleteRecursively() }
    }
    @Test fun validatesRangesAndCachesWithoutDownloadingFullFiles() {
        val directory = Files.createTempDirectory("content-test").toFile()
        var requests = 0
        var status = 206
        var range = "bytes 0-1048575/1048578"
        var body = ByteArray(1048576) { 7 }
        val adapter = ContentRanges(directory, "https://example.invalid/", "v1", listOf(ContentFile("Content/main.obb", 1048578)), {
            requests++
            object : HttpURLConnection(it) {
                override fun connect() {}
                override fun disconnect() {}
                override fun usingProxy() = false
                override fun getResponseCode() = status
                override fun getHeaderField(name: String?) = if (name == "Content-Range") range else null
                override fun getInputStream() = body.inputStream()
            }
        }, 1)
        try {
            assertEquals(1048576, adapter.read("Content/main.obb", "0", "v1").size)
            adapter.read("Content/main.obb", "0", "v1"); assertEquals(1, requests)
            range = "bytes 1048576-1048577/1048578"; body = byteArrayOf(1, 2)
            assertArrayEquals(body, adapter.read("Content/main.obb", "1048576", "v1"))
            assertEquals(1, directory.listFiles()!!.count { it.extension == "block" })
            assertThrows(Exception::class.java) { adapter.read("Content/../secret", "0", "v1") }
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "1", "v1") }
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "0", "wrong") }
            status = 200
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "0", "v1") }
            status = 206; range = "bytes 0-1048575/1048578"
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "0", "v1") }
        } finally { directory.deleteRecursively() }
    }
}
package io.github.qqclutchyqq.crossroad.android

import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

class ContentRangesTest {
    @Test fun validatesRangesAndCachesWithoutDownloadingFullFiles() {
        val directory = Files.createTempDirectory("content-test").toFile()
        var requests = 0
        var status = 206
        var range = "bytes 0-1048575/1048578"
        var body = ByteArray(1048576) { 7 }
        val adapter = ContentRanges(directory, "https://example.invalid/", "v1", listOf(ContentFile("Content/main.obb", 1048578)), {
            requests++
            object : HttpURLConnection(it) {
                override fun connect() {}
                override fun disconnect() {}
                override fun usingProxy() = false
                override fun getResponseCode() = status
                override fun getHeaderField(name: String?) = if (name == "Content-Range") range else null
                override fun getInputStream() = body.inputStream()
            }
        }, 1)
        try {
            assertEquals(1048576, adapter.read("Content/main.obb", "0", "v1").size)
            adapter.read("Content/main.obb", "0", "v1"); assertEquals(1, requests)
            range = "bytes 1048576-1048577/1048578"; body = byteArrayOf(1, 2)
            assertArrayEquals(body, adapter.read("Content/main.obb", "1048576", "v1"))
            assertEquals(1, directory.listFiles()!!.count { it.extension == "block" })
            assertThrows(Exception::class.java) { adapter.read("Content/../secret", "0", "v1") }
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "1", "v1") }
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "0", "wrong") }
            status = 200
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "0", "v1") }
            status = 206; range = "bytes 0-1048575/1048578"
            assertThrows(Exception::class.java) { adapter.read("Content/main.obb", "0", "v1") }
        } finally { directory.deleteRecursively() }
    }
}
