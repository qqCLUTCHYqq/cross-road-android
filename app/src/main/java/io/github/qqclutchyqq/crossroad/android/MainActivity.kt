package io.github.qqclutchyqq.crossroad.android

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val host = "appassets.androidplatform.net"
    private val home = "https://appassets.androidplatform.net/assets/web/index.html"
    private fun local(uri: Uri) = uri.scheme == "https" && uri.host == host && uri.port == -1 && uri.path?.startsWith("/assets/web/") == true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val manifest = JSONObject(assets.open("web/content-manifest.json").bufferedReader().use { it.readText() })
        val array = manifest.getJSONArray("files")
        val files = (0 until array.length()).map { array.getJSONObject(it).let { f -> ContentFile(f.getString("path"), f.getLong("size")) } }
        val version = manifest.getString("version")
        val ranges = ContentRanges(File(cacheDir, "content-$version"), manifest.getString("baseURL"), version, files)
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xff000000.toInt()) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        val bar = LinearLayout(this)
        bar.addView(Button(this).apply { text = "Restart"; setOnClickListener {
            AlertDialog.Builder(this@MainActivity).setMessage("Restart the runtime? Only progress already saved by the game will remain.")
                .setNegativeButton("Cancel", null).setPositiveButton("Restart") { _, _ -> web.loadUrl(home) }.show()
        } }, LinearLayout.LayoutParams(0, 48.dp(), 1f))
        bar.addView(Button(this).apply { text = "Diagnostics"; setOnClickListener { web.evaluateJavascript("window.crossroadShowDiagnostics?.()", null) } }, LinearLayout.LayoutParams(0, 48.dp(), 1f))
        root.addView(bar)
        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        web.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d("CrossRoad", message.message().take(2000)); return true
            }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !local(request.url)
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val uri = request.url
                if (!local(uri) || request.method != "GET") return response(403, "Blocked", "text/plain", "External navigation/resource blocked".toByteArray())
                return try {
                    if (uri.path == "/assets/web/__content") {
                        val bytes = ranges.read(uri.getQueryParameter("path"), uri.getQueryParameter("offset"), uri.getQueryParameter("version"))
                        response(200, "OK", "application/octet-stream", bytes)
                    } else loader.shouldInterceptRequest(uri) ?: response(404, "Not Found", "text/plain", byteArrayOf())
                } catch (error: Exception) {
                    Log.w("CrossRoad", "Content request failed: ${error.message}")
                    response(502, "Content unavailable", "text/plain", "Content unavailable. Check connection and restart.".toByteArray())
                }
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                AlertDialog.Builder(this@MainActivity).setMessage("Android stopped the WebView. Reopen Cross Road. Previously saved progress is retained.")
                    .setPositiveButton("Close") { _, _ -> finish() }.setCancelable(false).show()
                (view.parent as? LinearLayout)?.removeView(view); view.destroy(); return true
            }
        }
        // Fixed origin and default app profile: do not clear storage on launch/restart.
        val probe = BuildConfig.DEBUG && intent.getBooleanExtra("bootProbe", false)
        web.loadUrl(if (probe) home.replace("index.html", "probe.html") else home)
    }
    private fun response(code: Int, reason: String, type: String, bytes: ByteArray) = WebResourceResponse(type, "UTF-8", code, reason,
        mapOf("Cache-Control" to "no-store", "Content-Length" to bytes.size.toString()), ByteArrayInputStream(bytes))
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
    override fun onPause() {
        web.evaluateJavascript("window.crossroadSetBackground?.(true)", null)
        web.onPause()
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) { web.onResume(); web.evaluateJavascript("window.crossroadSetBackground?.(false)", null) }
    }
    @Deprecated("Activity back compatibility")
    override fun onBackPressed() {
        // Do not navigate to arbitrary history or silently discard an active game.
        AlertDialog.Builder(this).setMessage("Leave Cross Road? Progress must be saved by the game first.")
            .setNegativeButton("Stay", null).setPositiveButton("Leave") { _, _ -> finish() }.show()
    }
    override fun onDestroy() { if (::web.isInitialized) web.destroy(); super.onDestroy() }
}
