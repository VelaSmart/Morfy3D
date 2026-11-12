package com.velasmart.deckramobile

import android.app.Activity
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
        setContentView(ScrollView(this).apply { addView(root) })
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
