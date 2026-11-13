package br.com.morfy3d.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private val homeUrl = "https://morfy3d.com.br/"
    private val notificationRequestCode = 4102
    private val internalHosts = setOf("morfy3d.com.br", "www.morfy3d.com.br")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 15, 20)
        window.navigationBarColor = Color.rgb(11, 15, 20)
        createNotificationChannel()
        setContentView(createLayout())
        configureWebView()
        if (savedInstanceState == null) webView.loadUrl(homeUrl) else webView.restoreState(savedInstanceState)
        requestNotificationConsent()
    }

    private fun createLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(11, 15, 20))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.rgb(11, 15, 20))
        }
        val brand = TextView(this).apply {
            text = "MORFY 3D"
            textSize = 16f
            setTextColor(Color.WHITE)
            letterSpacing = .12f
            gravity = Gravity.CENTER_VERTICAL
        }
        toolbar.addView(brand, LinearLayout.LayoutParams(0, dp(44), 1f))
        toolbar.addView(toolbarButton("Compartilhar") { shareCurrentPage() })
        toolbar.addView(toolbarButton("Atualizar") { webView.reload() })
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(60)))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(0, 180, 220))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(35, 45, 55))
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(2)))
        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        return root
    }

    private fun toolbarButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(29, 39, 51))
            setOnClickListener { action() }
        }

    private fun configureWebView() {
        WebView.setWebContentsDebuggingEnabled(false)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
        }
        webView.setBackgroundColor(Color.rgb(11, 15, 20))
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme != "https") return true
                val host = uri.host?.lowercase() ?: return true
                if (host in internalHosts) return false
                openExternal(uri)
                return true
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                Toast.makeText(this@MainActivity, "Conexão segura indisponível. Tente novamente mais tarde.", Toast.LENGTH_LONG).show()
            }

            override fun onSafeBrowsingHit(view: WebView, request: WebResourceRequest, threatType: Int, callback: SafeBrowsingResponse) {
                callback.backToSafety(true)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível abrir este endereço.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCurrentPage() {
        val url = if (::webView.isInitialized && webView.url?.startsWith("https://") == true) webView.url!! else homeUrl
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MORFY 3D")
            putExtra(Intent.EXTRA_TEXT, "Confira na MORFY 3D: $url")
        }
        startActivity(Intent.createChooser(send, "Compartilhar MORFY 3D"))
    }

    private fun requestNotificationConsent() {
        if (Build.VERSION.SDK_INT < 33) {
            subscribeToNewsIfConfigured()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            subscribeToNewsIfConfigured()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Novidades da MORFY")
            .setMessage("Deseja receber notificações sobre novidades e atualizações da MORFY 3D?")
            .setNegativeButton("Agora não", null)
            .setPositiveButton("Permitir") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationRequestCode)
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            subscribeToNewsIfConfigured()
        }
    }

    private fun subscribeToNewsIfConfigured() {
        if (FirebaseApp.getApps(this).isEmpty()) return
        FirebaseMessaging.getInstance().subscribeToTopic("morfy-news")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("morfy_news", "Novidades MORFY", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Novidades e atualizações da MORFY 3D"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
