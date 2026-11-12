package com.velasmart.deckramobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.net.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    private var host = ""
    private var paired = false
    private var currentPage = "Configuração"
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var pin: EditText
    private lateinit var hostInput: EditText
    private lateinit var updateButton: Button
    private lateinit var navBar: LinearLayout
    private lateinit var connectionPanel: LinearLayout
    private var latestState: JSONObject? = null
    private val bg = Color.rgb(14,15,24)
    private val panel = Color.rgb(23,25,39)
    private val purple = Color.rgb(137,101,255)
    private val muted = Color.rgb(157,160,184)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildShell()
        renderPage()
        checkForUpdates()
    }

    private fun buildShell() {
        val d=resources.displayMetrics.density
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding((16*d).toInt(),(8*d).toInt(),(16*d).toInt(),(8*d).toInt()) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val brand=ImageView(this).apply { setImageResource(R.drawable.deckra_logo); scaleType=ImageView.ScaleType.FIT_START; contentDescription="DECKRA" }
        header.addView(brand,LinearLayout.LayoutParams((148*d).toInt(),(50*d).toInt()))
        header.addView(label("HARDWARE FEITO PARA VOCÊ",9,muted),LinearLayout.LayoutParams(0,-2,1f))
        root.addView(header,LinearLayout.LayoutParams(-1,(52*d).toInt()))
        val statusRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        status=label("●  PC não conectado",13,Color.rgb(255,190,80))
        statusRow.addView(status,LinearLayout.LayoutParams(0,-2,1f))
        val toggle=TextView(this).apply { text="CONECTAR PC  ▾"; textSize=11f; setTextColor(Color.WHITE); gravity=Gravity.CENTER; setPadding(12,9,12,9); background=rounded(Color.rgb(42,36,67),18,purple) }
        statusRow.addView(toggle)
        root.addView(statusRow)
        connectionPanel=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(12,6,12,12); background=rounded(panel,14,Color.rgb(43,45,61)); visibility=View.GONE }
        hostInput=EditText(this).apply { hint="Endereço IP do computador"; setTextColor(Color.WHITE); setHintTextColor(muted); inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI; setSingleLine(true); textSize=14f }
        connectionPanel.addView(hostInput)
        pin=EditText(this).apply { hint="PIN de 6 dígitos exibido no PC"; setTextColor(Color.WHITE); setHintTextColor(muted); inputType=2; setSingleLine(true); textSize=14f }
        connectionPanel.addView(pin)
        val connectRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        connectRow.addView(action("PROCURAR") { discoverPc() },LinearLayout.LayoutParams(0,(44*d).toInt(),1f))
        connectRow.addView(action("CONECTAR") { connectPc() },LinearLayout.LayoutParams(0,(44*d).toInt(),1f).apply { leftMargin=(8*d).toInt() })
        connectionPanel.addView(connectRow)
        root.addView(connectionPanel,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(6*d).toInt() })
        toggle.setOnClickListener { connectionPanel.visibility=if(connectionPanel.visibility==View.VISIBLE) View.GONE else View.VISIBLE; toggle.text=if(connectionPanel.visibility==View.VISIBLE) "FECHAR  ▴" else "CONECTAR PC  ▾" }
        val scroll=ScrollView(this).apply { clipToPadding=false }
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,(8*d).toInt(),0,(14*d).toInt()) }
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val navScroll=HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false; overScrollMode=View.OVER_SCROLL_NEVER }
        navBar=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,(6*d).toInt(),0,(6*d).toInt()) }
        navScroll.addView(navBar)
        root.addView(navScroll,LinearLayout.LayoutParams(-1,(50*d).toInt()))
        renderNavigation()
        updateButton=action("VERIFICAR ATUALIZAÇÃO") { checkForUpdates() }
        updateButton.textSize=11f; updateButton.minHeight=(36*d).toInt()
        root.addView(updateButton,LinearLayout.LayoutParams(-1,(38*d).toInt()).apply { topMargin=(3*d).toInt() })
        setContentView(root)
    }

    private fun renderPage() {
        content.removeAllViews()
        content.addView(label(if(currentPage=="Configuração") "Visão geral" else currentPage,25,Color.WHITE))
        content.addView(label(when(currentPage) {
            "Configuração" -> "Configure seu controle e os canais de áudio do PC."
            "Aplicativos" -> "Controle o áudio de cada aplicativo do Windows."
            "Microfone" -> "Gerencie o dispositivo de entrada padrão."
            "Equalizador" -> "Estado do processamento de áudio."
            else -> "Conexão e dispositivos do seu DECKRA."
        },13,muted))
        when(currentPage) {
            "Configuração" -> {
                val s=latestState
                addPanel("Canais") {
                    addView(label("Saída de áudio",16,Color.WHITE))
                    addView(label(s?.optJSONObject("output")?.optString("name") ?: "Aguardando conexão",13,muted))
                    val outputVolume=((s?.optJSONObject("output")?.optDouble("volume",0.0) ?: 0.0)*100).toInt()
                    addView(label("Volume geral: $outputVolume%",22,Color.WHITE))
                    val masterSeek=SeekBar(this@MainActivity).apply { max=100; progress=outputVolume }
                    masterSeek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b:SeekBar?,v:Int,u:Boolean) {}
                        override fun onStartTrackingTouch(b:SeekBar?) {}
                        override fun onStopTrackingTouch(b:SeekBar?) { send("set_master_volume",JSONObject().put("value",(b?.progress ?: 0)/100f)) }
                    })
                    addView(masterSeek)
                    addView(actionRow("VOLUME +", { send("volume_up") }, "VOLUME −", { send("volume_down") }))
                    addView(actionRow("MUTAR", { send("volume_mute") }, "PLAY / PAUSE", { send("media_play_pause") }))
                    addView(actionRow("PRÓXIMA", { send("media_next") }, "ANTERIOR", { send("media_previous") }))
                }
                addPanel("Microfone") {
                    val mic=s?.optJSONObject("microphone")
                    addView(label(mic?.optString("name") ?: "Não conectado",13,muted))
                    addView(label("Volume: "+((mic?.optDouble("volume",0.0) ?: 0.0)*100).toInt()+"%",20,Color.WHITE))
                    val micSeek=SeekBar(this@MainActivity).apply { max=100; progress=((mic?.optDouble("volume",0.0) ?: 0.0)*100).toInt() }
                    micSeek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b:SeekBar?,v:Int,u:Boolean) {}
                        override fun onStartTrackingTouch(b:SeekBar?) {}
                        override fun onStopTrackingTouch(b:SeekBar?) { send("set_mic_volume",JSONObject().put("value",(b?.progress ?: 0)/100f)) }
                    })
                    addView(micSeek)
                    addView(action(if(mic?.optBoolean("mute")==true) "Ativar microfone" else "Silenciar microfone") { send("set_mic_mute",JSONObject().put("value",mic?.optBoolean("mute")!=true)) })
                }
                addPanel("Equalizador") {
                    val eq=s?.optJSONObject("equalizer")
                    addView(label(if(eq?.optBoolean("available")==true) "Disponível" else "Indisponível",15,Color.WHITE))
                    addView(label(eq?.optString("reason") ?: "Requer driver APO ou dispositivo virtual com processamento no Windows.",12,muted))
                }
                addPanel("Dispositivos") { addView(label(if(paired) "Conectado a $host" else "Procurando o DECKRA...",14,if(paired) Color.GREEN else muted)) }
            }
            "Teclas" -> {
                addPanel("Atalhos das teclas") {
                    addView(label("A interface segue a organização do DECKRA Desktop.",14,Color.WHITE))
                    addView(label("O serviço Bridge atual ainda não recebe configurações de teclas do controlador. Atribuições serão habilitadas quando o protocolo do firmware estiver integrado. Nenhuma tecla é simulada.",13,muted))
                }
            }
            "Aplicativos" -> {
                val apps=latestState?.optJSONArray("applications") ?: JSONArray()
                if(apps.length()==0) content.addView(label("Nenhuma sessão de áudio ativa foi encontrada. Abra um aplicativo e reproduza algum áudio.",14,muted))
                for(i in 0 until apps.length()) {
                    val app=apps.optJSONObject(i) ?: continue
                    addPanel(app.optString("name","Aplicativo")) {
                        addView(label("Volume: "+(app.optDouble("volume",0.0)*100).toInt()+"%",15,Color.WHITE))
                        val seek=SeekBar(this@MainActivity).apply { max=100; progress=(app.optDouble("volume",0.0)*100).toInt() }
                        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(b:SeekBar?,v:Int,u:Boolean) {}
                            override fun onStartTrackingTouch(b:SeekBar?) {}
                            override fun onStopTrackingTouch(b:SeekBar?) { send("set_app_volume",JSONObject().put("processId",app.optInt("processId")).put("value",b?.progress?.div(100f) ?: 0f)) }
                        })
                        addView(seek)
                        addView(action(if(app.optBoolean("mute")) "Ativar som" else "Silenciar") { send("set_app_mute",JSONObject().put("processId",app.optInt("processId")).put("value",!app.optBoolean("mute"))) })
                    }
                }
                content.addView(action("Atualizar lista de aplicativos") { send("get_state") })
            }
            "Microfone" -> {
                val mic=latestState?.optJSONObject("microphone")
                addPanel("Dispositivo de entrada") {
                    addView(label(mic?.optString("name") ?: "Conecte o PC para consultar",14,muted))
                    addView(label("Volume: "+((mic?.optDouble("volume",0.0) ?: 0.0)*100).toInt()+"%",22,Color.WHITE))
                    val seek=SeekBar(this@MainActivity).apply { max=100; progress=((mic?.optDouble("volume",0.0) ?: 0.0)*100).toInt() }
                    seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b:SeekBar?,v:Int,u:Boolean) {}
                        override fun onStartTrackingTouch(b:SeekBar?) {}
                        override fun onStopTrackingTouch(b:SeekBar?) { send("set_mic_volume",JSONObject().put("value",(b?.progress ?: 0)/100f)) }
                    })
                    addView(seek)
                    addView(action(if(mic?.optBoolean("mute")==true) "Ativar microfone" else "Silenciar microfone") { send("set_mic_mute",JSONObject().put("value",mic?.optBoolean("mute")!=true)) })
                }
            }
            "Equalizador" -> {
                val eq=latestState?.optJSONObject("equalizer")
                addPanel("Equalizador de áudio") {
                    addView(label(if(eq?.optBoolean("available")==true) "Disponível" else "Indisponível no momento",17,Color.WHITE))
                    addView(label(eq?.optString("reason") ?: "Conecte ao PC para consultar os recursos disponíveis.",13,muted))
                    addView(label("O Windows precisa de um driver APO ou dispositivo virtual com processamento para aplicar equalização global. Não exibimos controles falsos.",13,muted))
                }
            }
            else -> {
                addPanel("Conexão") {
                    addView(label("Computador: "+if(host.isBlank()) "não localizado" else host,14,Color.WHITE))
                    addView(label("Estado: "+if(paired) "Pareado" else "Desconectado",14,muted))
                    addView(action("Procurar computador novamente") { discoverPc() })
                    addView(action("Desconectar") { socket?.close(1000,"user disconnect"); paired=false; host=""; status.text="●  PC não conectado"; renderPage() })
                }
                addPanel("Sobre") { addView(label("DECKRA Mobile • Controle local do DECKRA Desktop",13,muted)) }
            }
        }
        if(!paired) content.addView(label("Para usar os controles, execute DeckraBridge.exe no Windows e conecte os dois dispositivos à mesma rede local.",13,Color.rgb(255,190,90)))
    }

    private fun renderNavigation() {
        if(!::navBar.isInitialized) return
        navBar.removeAllViews()
        listOf("Configuração","Aplicativos","Microfone","Equalizador","Teclas").forEach { page ->
            val selected=page==currentPage
            val tab=TextView(this).apply {
                text=when(page) { "Configuração" -> "INÍCIO"; "Aplicativos" -> "APPS"; "Microfone" -> "MIC"; "Equalizador" -> "EQ"; else -> "TECLAS" }
                textSize=10f; letterSpacing=.04f; gravity=Gravity.CENTER
                setTextColor(if(selected) Color.WHITE else muted); setPadding(14,10,14,10)
                background=rounded(if(selected) Color.rgb(48,39,78) else Color.TRANSPARENT,18,if(selected) purple else Color.TRANSPARENT)
                setOnClickListener { currentPage=page; renderNavigation(); renderPage() }
            }
            navBar.addView(tab,LinearLayout.LayoutParams(-2,(40*resources.displayMetrics.density).toInt()).apply { rightMargin=(5*resources.displayMetrics.density).toInt() })
        }
    }
    private fun rounded(color:Int,radius:Int,stroke:Int=Color.TRANSPARENT)=GradientDrawable().apply {
        setColor(color); cornerRadius=radius*resources.displayMetrics.density
        if(stroke!=Color.TRANSPARENT) setStroke((1*resources.displayMetrics.density).toInt().coerceAtLeast(1),stroke)
    }
    private fun addPanel(title:String, build:LinearLayout.()->Unit) {
        val d=resources.displayMetrics.density
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((14*d).toInt(),(11*d).toInt(),(14*d).toInt(),(11*d).toInt()); background=rounded(panel,16,Color.rgb(43,45,61)) }
        card.addView(label(title,17,Color.WHITE)); card.build()
        content.addView(card,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(9*d).toInt(); bottomMargin=(3*d).toInt() })
    }
    private fun actionRow(first:String, firstRun:()->Unit, second:String, secondRun:()->Unit)=LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL
        addView(action(first,firstRun),LinearLayout.LayoutParams(0,(40*resources.displayMetrics.density).toInt(),1f).apply { rightMargin=(5*resources.displayMetrics.density).toInt() })
        addView(action(second,secondRun),LinearLayout.LayoutParams(0,(40*resources.displayMetrics.density).toInt(),1f).apply { leftMargin=(5*resources.displayMetrics.density).toInt() })
    }
    private fun label(value:String,size:Int,color:Int)=TextView(this).apply { text=value; textSize=size.toFloat(); setTextColor(color); setPadding(3,5,3,5) }
    private fun action(value:String, run:()->Unit)=Button(this).apply {
        text=value; isAllCaps=false; textSize=13f; minHeight=(40*resources.displayMetrics.density).toInt(); minWidth=0
        setPadding(8,5,8,5); setTextColor(Color.WHITE); background=rounded(purple,12); stateListAnimator=null
        setOnClickListener { run() }
    }

    @Suppress("DEPRECATION")
    private fun installedVersionCode():Long=packageManager.getPackageInfo(packageName,0).versionCode.toLong()
    private fun checkForUpdates() {
        updateButton.isEnabled=false; updateButton.text="VERIFICANDO..."
        val request=Request.Builder().url("https://api.github.com/repos/VelaSmart/Morfy3D/releases/tags/deckra-mobile-latest").header("Accept","application/vnd.github+json").build()
        client.newCall(request).enqueue(object:Callback {
            override fun onFailure(call:Call,e:java.io.IOException){ runOnUiThread { updateButton.isEnabled=true; updateButton.text="VERIFICAR ATUALIZAÇÃO" } }
            override fun onResponse(call:Call,response:Response){ response.use {
                val body=it.body?.string() ?: return
                try {
                    val json=JSONObject(body); val code=Regex("versionCode=(\\d+)").find(json.optString("body"))?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    val assets=json.getJSONArray("assets"); var url=""
                    for(i in 0 until assets.length()) if(assets.getJSONObject(i).optString("name")=="deckra-mobile.apk") url=assets.getJSONObject(i).optString("browser_download_url")
                    runOnUiThread {
                        updateButton.isEnabled=true
                        if(code>installedVersionCode() && url.isNotBlank()) { updateButton.text="ATUALIZAR • $code"; updateButton.setOnClickListener { downloadAndInstall(url) } }
                        else { updateButton.text="APP ATUALIZADO"; updateButton.setOnClickListener { checkForUpdates() } }
                    }
                } catch(_:Exception){ runOnUiThread { updateButton.isEnabled=true; updateButton.text="VERIFICAR ATUALIZAÇÃO" } }
            } }
        })
    }
    private fun downloadAndInstall(url:String) {
        updateButton.isEnabled=false; updateButton.text="BAIXANDO..."
        client.newCall(Request.Builder().url(url).build()).enqueue(object:Callback {
            override fun onFailure(call:Call,e:java.io.IOException){ runOnUiThread { updateButton.isEnabled=true; updateButton.text="TENTAR NOVAMENTE" } }
            override fun onResponse(call:Call,response:Response){ response.use {
                if(!it.isSuccessful) { runOnUiThread { updateButton.isEnabled=true; updateButton.text="FALHA NO DOWNLOAD • TENTAR NOVAMENTE" }; return }
                val apk=File(cacheDir,"deckra-mobile-update.apk")
                if(apk.exists() && !apk.delete()) { runOnUiThread { updateButton.isEnabled=true; updateButton.text="LIMPAR ARQUIVO E TENTAR NOVAMENTE" }; return }
                val body=it.body ?: run { runOnUiThread { updateButton.isEnabled=true; updateButton.text="DOWNLOAD INVÁLIDO" }; return }
                val expectedLength=body.contentLength()
                body.byteStream().use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
                if(!apk.exists() || apk.length()==0L || (expectedLength>0 && apk.length()!=expectedLength)) { apk.delete(); runOnUiThread { updateButton.isEnabled=true; updateButton.text="DOWNLOAD INCOMPLETO • TENTAR NOVAMENTE" }; return }
                runOnUiThread {
                    if(!packageManager.canRequestPackageInstalls()) { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName"))); updateButton.isEnabled=true; updateButton.text="AUTORIZAR E INSTALAR"; updateButton.setOnClickListener { downloadAndInstall(url) }; return@runOnUiThread }
                    val uri=FileProvider.getUriForFile(this@MainActivity,"$packageName.fileprovider",apk)
                    startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).apply { data=uri; flags=Intent.FLAG_GRANT_READ_URI_PERMISSION })
                    updateButton.isEnabled=true; updateButton.text="INSTALADOR ABERTO"
                }
            } }
        })
    }

    private fun discoverPc() {
        status.text="●  Procurando computadores na rede..."
        thread {
            var found:InetAddress?=null
            val wifi=getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock=wifi.createMulticastLock("deckra-discovery").apply { setReferenceCounted(false); acquire() }
            try {
                DatagramSocket(null).use { ds ->
                    ds.reuseAddress=true; ds.broadcast=true; ds.soTimeout=650
                    val wifiAddress = NetworkInterface.getNetworkInterfaces().toList()
                        .filter { it.isUp && !it.isLoopback && (it.name.startsWith("wlan", true) || it.name.startsWith("wifi", true)) }
                        .flatMap { it.interfaceAddresses }
                        .firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
                    if(wifiAddress==null) throw IllegalStateException("Wi-Fi IPv4 não disponível. Conecte o celular à mesma rede do PC.")
                    ds.bind(InetSocketAddress(wifiAddress.address,0))
                    val targets=linkedSetOf<InetAddress>()
                    wifiAddress.broadcast?.let { targets.add(it) }
                    targets.add(InetAddress.getByName("255.255.255.255"))
                    val data="DECKRA_DISCOVER_V2".toByteArray()
                    for(attempt in 1..5) {
                        if(found!=null) break
                        for(target in targets) try { ds.send(DatagramPacket(data,data.size,target,37891)) } catch(_:Exception) {}
                        val deadline=System.currentTimeMillis()+900
                        while(System.currentTimeMillis()<deadline && found==null) {
                            try {
                                val buf=ByteArray(1024); val packet=DatagramPacket(buf,buf.size); ds.receive(packet)
                                val reply=String(packet.data,0,packet.length,Charsets.UTF_8)
                                val payload=runCatching { JSONObject(reply) }.getOrNull()
                                if(payload?.optString("type")=="deckra_bridge" && payload.optInt("port",0)==37892) found=packet.address
                            } catch(_:SocketTimeoutException) { break }
                        }
                    }
                }
            } catch(_:Exception) {} finally { try { lock.release() } catch(_:Exception) {} }
            runOnUiThread {
                if(found!=null) { host=found!!.hostAddress ?: ""; hostInput.setText(host); status.text="●  PC encontrado: $host"; Toast.makeText(this,"PC encontrado. Digite o PIN.",Toast.LENGTH_LONG).show() }
                else { status.text="●  PC não encontrado"; Toast.makeText(this,"Verifique se a Bridge está aberta e se o Firewall permite UDP 37891 e TCP 37892 em rede privada.",Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun connectPc() {
        val typedHost=hostInput.text.toString().trim()
        if(typedHost.isNotBlank()) host=typedHost
        if(host.isBlank() || runCatching { InetAddress.getByName(host) }.isFailure){ Toast.makeText(this,"Informe um IP válido ou procure o PC primeiro.",Toast.LENGTH_SHORT).show(); return }
        val code=pin.text.toString().trim()
        if(!code.matches(Regex("\\d{6}"))){ Toast.makeText(this,"O PIN deve conter 6 dígitos.",Toast.LENGTH_SHORT).show(); return }
        socket=client.newWebSocket(Request.Builder().url("ws://$host:37892/").build(),object:WebSocketListener() {
            override fun onOpen(webSocket:WebSocket,response:Response){ webSocket.send(JSONObject().put("type","pair").put("pin",code).toString()) }
            override fun onMessage(webSocket:WebSocket,text:String) {
                try {
                    val msg=JSONObject(text)
                    when(msg.optString("type")) {
                        "pair_result" -> { paired=msg.optBoolean("paired"); runOnUiThread { status.text=if(paired) "●  Conectado ao DECKRA • $host" else "●  PIN incorreto"; if(paired) send("get_state") } }
                        "state" -> { latestState=msg.optJSONObject("data"); runOnUiThread { renderPage() } }
                        "result" -> { val data=msg.optJSONObject("data"); if(data!=null && data.has("applications")) latestState=data; runOnUiThread { renderPage() } }
                        "error" -> runOnUiThread { Toast.makeText(this@MainActivity,msg.optString("message"),Toast.LENGTH_LONG).show() }
                    }
                } catch(_:Exception) {}
            }
            override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?){ paired=false; runOnUiThread { status.text="●  Falha na conexão"; Toast.makeText(this@MainActivity,t.message ?: "Erro de rede",Toast.LENGTH_LONG).show() } }
            override fun onClosed(webSocket:WebSocket,code:Int,reason:String){ paired=false; runOnUiThread { status.text="●  PC desconectado" } }
        })
    }
    private fun send(command:String,extra:JSONObject=JSONObject()) {
        if(!paired){ Toast.makeText(this,"Conecte ao PC primeiro.",Toast.LENGTH_SHORT).show(); return }
        extra.put("type","command").put("command",command)
        if(socket?.send(extra.toString())!=true) Toast.makeText(this,"Falha ao enviar comando.",Toast.LENGTH_SHORT).show()
    }
    override fun onDestroy(){ socket?.close(1000,"app closed"); client.dispatcher.executorService.shutdown(); super.onDestroy() }
}
