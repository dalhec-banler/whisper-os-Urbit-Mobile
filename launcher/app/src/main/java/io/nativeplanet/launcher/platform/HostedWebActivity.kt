package io.nativeplanet.launcher.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HostedWebActivity : Activity() {
    private lateinit var webView: WebView
    private var retriedAuthRedirect = false
    private var appTitle = "Urbit"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Urbit" }
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }
        appTitle = title

        CookieManager.getInstance().setAcceptCookie(true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PAPER)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(INK)
            setPadding(24, 16, 18, 16)
        }

        val close = TextView(this).apply {
            text = "‹"
            textSize = 30f
            setTextColor(PAPER)
            gravity = Gravity.CENTER
            setPadding(18, 0, 18, 0)
            setOnClickListener {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        }
        toolbar.addView(close, LinearLayout.LayoutParams(64, 64))

        val label = TextView(this).apply {
            text = title
            textSize = 15f
            letterSpacing = 0.08f
            setTextColor(PAPER)
            isAllCaps = true
            gravity = Gravity.CENTER_VERTICAL
        }
        toolbar.addView(
            label,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        val done = TextView(this).apply {
            text = "HOME"
            textSize = 12f
            letterSpacing = 0.1f
            setTextColor(PAPER_DIM)
            gravity = Gravity.CENTER
            setPadding(18, 0, 18, 0)
            setOnClickListener { finishToHome() }
        }
        toolbar.addView(done, LinearLayout.LayoutParams(154, 64))

        webView = WebView(this).apply {
            setBackgroundColor(PAPER)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = NativePlanetWebClient()
        }

        root.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        setContentView(root)
        establishLocalSessionThenLoad(url)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun finishToHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private inner class NativePlanetWebClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val uri = request.url
            return if (uri.host == LOCAL_HOST) {
                false
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            val uri = Uri.parse(url)
            if (retriedAuthRedirect || uri.host != LOCAL_HOST || uri.path != "/~/login") {
                return
            }

            val redirect = uri.getQueryParameter("redirect")
            if (!redirect.isNullOrBlank() && redirect.startsWith("/")) {
                retriedAuthRedirect = true
                CookieManager.getInstance().flush()
                view.postDelayed({
                    view.loadUrl(LOCAL_EYRE_ORIGIN + redirect)
                }, AUTH_RETRY_DELAY_MS)
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                showUnavailable("not available on this moon yet")
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            if (request.isForMainFrame) {
                showUnavailable("could not open local app")
            }
        }
    }

    private fun showUnavailable(message: String) {
        val html = """
            <!doctype html>
            <html>
              <head>
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <style>
                  body {
                    margin: 0;
                    background: #f4f1eb;
                    color: #171613;
                    font-family: sans-serif;
                  }
                  main {
                    padding: 56px 28px;
                  }
                  .eyebrow {
                    color: #8b857a;
                    font: 700 11px monospace;
                    letter-spacing: 0.16em;
                    text-transform: uppercase;
                  }
                  h1 {
                    margin: 20px 0 12px;
                    font: 400 36px serif;
                    line-height: 1.02;
                  }
                  p {
                    color: #6f695f;
                    font-size: 16px;
                    line-height: 1.5;
                    max-width: 28em;
                  }
                </style>
              </head>
              <body>
                <main>
                  <div class="eyebrow">── ${escapeHtml(appTitle)}</div>
                  <h1>Not installed yet.</h1>
                  <p>${escapeHtml(message)} When the parent ship publishes this app for mobile, it will open here automatically.</p>
                </main>
              </body>
            </html>
        """.trimIndent()
        webView.loadDataWithBaseURL(LOCAL_EYRE_ORIGIN, html, "text/html", "UTF-8", null)
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun establishLocalSessionThenLoad(url: String) {
        Thread {
            val authenticated = runCatching {
                val code = fetchWebLoginCode() ?: return@runCatching false
                establishEyreCookie(code)
            }.getOrElse {
                Log.w(TAG, "Local web session bootstrap failed: ${it.javaClass.simpleName}")
                false
            }

            runOnUiThread {
                if (!authenticated) {
                    Log.w(TAG, "Opening hosted app without pre-authenticated local session")
                }
                webView.loadUrl(url)
            }
        }.start()
    }

    private fun fetchWebLoginCode(): String? {
        val result = contentResolver.call(CONTROLLER_URI, "getWebLoginCode", null, null)
        val raw = result?.getString("json") ?: return null
        val json = JSONObject(raw)
        if (!json.optBoolean("ok", false)) {
            return null
        }
        return json.optString("code").takeIf { it.isNotBlank() }
    }

    private fun establishEyreCookie(code: String): Boolean {
        val connection = (URL("$LOCAL_EYRE_ORIGIN/~/login").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            instanceFollowRedirects = false
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

        return try {
            val encoded = "password=" + URLEncoder.encode(code, StandardCharsets.UTF_8.name())
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(encoded)
            }

            val status = connection.responseCode
            val cookies = connection.headerFields["Set-Cookie"].orEmpty()
            if (cookies.isEmpty() || status !in 200..399) {
                false
            } else {
                val manager = CookieManager.getInstance()
                cookies.forEach { cookie ->
                    manager.setCookie(LOCAL_EYRE_ORIGIN, cookie)
                }
                manager.flush()
                true
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "NativePlanetHostedWeb"
        private const val EXTRA_TITLE = "io.nativeplanet.launcher.extra.TITLE"
        private const val EXTRA_URL = "io.nativeplanet.launcher.extra.URL"
        private const val LOCAL_HOST = "127.0.0.1"
        const val LOCAL_EYRE_ORIGIN = "http://127.0.0.1:8080"
        private const val AUTH_RETRY_DELAY_MS = 150L
        private const val HTTP_TIMEOUT_MS = 5000
        private val CONTROLLER_URI: Uri = Uri.parse("content://io.nativeplanet.controller")
        private const val INK = Color.BLACK
        private const val PAPER = 0xfff4f1eb.toInt()
        private const val PAPER_DIM = 0xffc9c4ba.toInt()

        fun intent(context: Context, title: String, url: String): Intent {
            return Intent(context, HostedWebActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_URL, url)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
