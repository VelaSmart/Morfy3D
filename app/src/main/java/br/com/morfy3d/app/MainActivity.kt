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
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val homeUrl = "https://morfy3d.com.br/"
    private val notificationRequestCode = 4102
    private val internalHosts = setOf("morfy3d.com.br", "www.morfy3d.com.br")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15+ aplica edge-to-edge obrigatoriamente em apps atuais.
        // Os insets abaixo mantêm o conteúdo do WebView fora das barras do sistema.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        createNotificationChannel()
        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(11, 15, 20))
        setContentView(webView)
        applyWindowInsets()
        configureWebView()
        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // Mantém a navegação existente ao recriar a Activity.
        } else {
            webView.loadUrl(resolveStartUrl(intent))
        }
        requestNotificationConsent()
    }

    private fun applyWindowInsets() {
        WindowCompat.getInsetsController(window, webView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(webView) { view, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(webView)
    }

    private fun resolveStartUrl(source: Intent?): String {
        val candidate = source?.data
        val target = if (
            candidate != null &&
            candidate.scheme == "https" &&
            candidate.host?.lowercase() in internalHosts
        ) {
            candidate
        } else {
            Uri.parse(homeUrl)
        }

        // Atualiza o documento principal a cada abertura do app sem apagar
        // todo o cache do WebView nem forçar o download de todos os recursos.
        val refreshed = target.buildUpon()
            .appendQueryParameter("_morfy_app_refresh", System.currentTimeMillis().toString())
            .build()
        return refreshed.toString()
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
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível abrir este endereço.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationConsent() {
        if (FirebaseApp.getApps(this).isEmpty()) return
        if (Build.VERSION.SDK_INT < 33) {
            subscribeToNewsIfConfigured()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            subscribeToNewsIfConfigured()
            return
        }
        val preferences = getSharedPreferences("morfy_notifications", MODE_PRIVATE)
        if (preferences.getBoolean("permission_prompted", false)) return
        AlertDialog.Builder(this)
            .setTitle("Novidades da MORFY")
            .setMessage("Deseja receber notificações quando novos produtos forem publicados?")
            .setNegativeButton("Agora não") { _, _ ->
                preferences.edit().putBoolean("permission_prompted", true).apply()
            }
            .setPositiveButton("Permitir") { _, _ ->
                preferences.edit().putBoolean("permission_prompted", true).apply()
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
            val channel = NotificationChannel("morfy_news", "Novos produtos MORFY", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos quando novos produtos forem publicados na MORFY 3D"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        webView.loadUrl(resolveStartUrl(intent))
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
}
