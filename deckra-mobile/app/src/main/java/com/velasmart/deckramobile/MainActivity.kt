package com.velasmart.deckramobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import android.os.Bundle
import android.graphics.Color
import android.widget.*
import okhttp3.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    private var host = ""
    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var pin: EditText
    private lateinit var root: LinearLayout
    private lateinit var updateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 15, 25)
        window.navigationBarColor = Color.rgb(12, 15, 25)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 20); setBackgroundColor(Color.rgb(12,15,25)) }
        fun text(value: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply { text=value; textSize=size; setTextColor(color); setPadding(0,8,0,8) }
        root.addView(text("DECKRA", 30f))
        root.addView(text("CONTROLE REMOTO • REDE LOCAL", 12f, Color.LTGRAY))
        status = text("PC não conectado", 16f, Color.rgb(255,190,90)); root.addView(status)
        root.addView(Button(this).apply { text="PROCURAR MEU PC"; setOnClickListener { discoverPc() } })
        pin = EditText(this).apply { hint="Código de pareamento do Windows"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); inputType=2 }; root.addView(pin)
        root.addView(Button(this).apply { text="CONECTAR"; setOnClickListener { connectPc() } })
        root.addView(text("CONTROLES", 18f))
        root.addView(Button(this).apply { text="Volume +"; setOnClickListener { send("volume_up") } })
        root.addView(Button(this).apply { text="Volume −"; setOnClickListener { send("volume_down") } })
        root.addView(Button(this).apply { text="Silenciar / ativar"; setOnClickListener { send("volume_mute") } })
        root.addView(text("ATALHOS", 18f))
        root.addView(Button(this).apply { text="Reproduzir / pausar"; setOnClickListener { send("media_play_pause") } })
        root.addView(Button(this).apply { text="Próxima faixa"; setOnClickListener { send("media_next") } })
        root.addView(Button(this).apply { text="Faixa anterior"; setOnClickListener { send("media_previous") } })
        log = text("Aguardando conexão com o DECKRA Bridge no Windows.", 12f, Color.LTGRAY)
        root.addView(log)
        updateButton = Button(this).apply { text="VERIFICAR ATUALIZAÇÃO"; setOnClickListener { checkForUpdates() } }
        root.addView(updateButton, 5)
        setContentView(ScrollView(this).apply { addView(root) })
        checkForUpdates()
    }


    @Suppress("DEPRECATION")
    private fun installedVersionCode(): Long = packageManager.getPackageInfo(packageName, 0).versionCode.toLong()

    private fun checkForUpdates() {
        runOnUiThread { updateButton.isEnabled = false; updateButton.text = "VERIFICANDO ATUALIZAÇÕES..." }
        val request = Request.Builder()
            .url("https://api.github.com/repos/VelaSmart/Morfy3D/releases/tags/deckra-mobile-latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread { updateButton.isEnabled = true; updateButton.text = "VERIFICAR ATUALIZAÇÃO"; log.text = "Não foi possível consultar atualizações." }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val release = it.body?.string()
                    if (!it.isSuccessful || release == null) {
                        runOnUiThread { updateButton.isEnabled = true; updateButton.text = "VERIFICAR ATUALIZAÇÃO"; log.text = "Atualizações indisponíveis no momento." }
                        return
                    }
                    try {
                        val json = JSONObject(release)
                        val body = json.optString("body")
                        val remoteCode = Regex("versionCode=(\\d+)").find(body)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        val currentCode = installedVersionCode()
                        val apkUrl = json.getJSONArray("assets").let { assets ->
                            (0 until assets.length()).map { index -> assets.getJSONObject(index) }
                                .firstOrNull { asset -> asset.optString("name") == "deckra-mobile.apk" }
                                ?.optString("browser_download_url")
                        }
                        if (remoteCode > currentCode && !apkUrl.isNullOrBlank()) {
                            runOnUiThread {
                                updateButton.isEnabled = true
                                updateButton.text = "ATUALIZAR PARA A VERSÃO $remoteCode"
                                log.text = "Nova versão disponível. Toque em ATUALIZAR para baixar e instalar."
                                updateButton.setOnClickListener { downloadAndInstall(apkUrl) }
                            }
                        } else {
                            runOnUiThread {
                                updateButton.isEnabled = true
                                updateButton.text = "SEU APP ESTÁ ATUALIZADO"
                                updateButton.setOnClickListener { checkForUpdates() }
                            }
                        }
                    } catch (_: Exception) {
                        runOnUiThread { updateButton.isEnabled = true; updateButton.text = "VERIFICAR ATUALIZAÇÃO"; log.text = "Não foi possível interpretar a versão publicada." }
                    }
                }
            }
        })
    }

    private fun downloadAndInstall(url: String) {
        updateButton.isEnabled = false
        updateButton.text = "BAIXANDO ATUALIZAÇÃO..."
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread { updateButton.isEnabled = true; updateButton.text = "TENTAR ATUALIZAR NOVAMENTE"; log.text = "Falha ao baixar o APK." }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnUiThread { updateButton.isEnabled = true; updateButton.text = "TENTAR ATUALIZAR NOVAMENTE"; log.text = "Download da atualização falhou." }
                        return
                    }
                    val apk = File(cacheDir, "deckra-mobile-update.apk")
                    it.body?.byteStream()?.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
                    runOnUiThread {
                        if (!packageManager.canRequestPackageInstalls()) {
                            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                            updateButton.isEnabled = true
                            updateButton.text = "TOQUE PARA INSTALAR APÓS AUTORIZAR"
                            log.text = "Permita instalações para DECKRA Mobile e toque novamente em atualizar."
                            updateButton.setOnClickListener { downloadAndInstall(url) }
                            return@runOnUiThread
                        }
                        val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", apk)
                        val install = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                            data = uri
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        startActivity(install)
                        updateButton.isEnabled = true
                        updateButton.text = "ATUALIZAÇÃO BAIXADA"
                    }
                }
            }
        })
    }

    private fun discoverPc() {
        status.text = "Procurando computadores na mesma rede..."
        Thread {
            try {
                DatagramSocket().use { s ->
                    s.broadcast = true; s.soTimeout = 1800
                    val data = "DECKRA_DISCOVER".toByteArray()
                    s.send(DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), 37891))
                    val buf = ByteArray(512); val packet = DatagramPacket(buf, buf.size)
                    s.receive(packet); host = packet.address.hostAddress ?: ""
                    runOnUiThread { status.text = "PC encontrado: $host"; log.text = "Informe o PIN exibido pelo DECKRA Bridge no Windows." }
                }
            } catch (_: Exception) { runOnUiThread { status.text = "Nenhum PC encontrado"; log.text = "Confirme se o DECKRA Bridge está aberto e se ambos estão no mesmo Wi-Fi." } }
        }.start()
    }

    private fun connectPc() {
        if (host.isBlank()) { log.text="Procure o PC primeiro."; return }
        val request = Request.Builder().url("ws://$host:37892").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("type","pair").put("pin",pin.text.toString()).toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    val msg = JSONObject(text)
                    if (msg.optBoolean("paired")) status.text="Conectado ao DECKRA no PC"
                    else log.text=msg.optString("message", text)
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread { status.text="Falha na conexão"; log.text=t.message ?: "Erro de rede" }
            }
        })
    }

    private fun send(command: String) {
        val ok = socket?.send(JSONObject().put("type","command").put("command",command).toString()) ?: false
        if (!ok) log.text="Conecte ao PC antes de enviar comandos."
    }

    override fun onDestroy() { socket?.close(1000,"app closed"); client.dispatcher.executorService.shutdown(); super.onDestroy() }
}
