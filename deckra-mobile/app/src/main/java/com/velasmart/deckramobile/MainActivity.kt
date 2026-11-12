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
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(9,10,16)); setPadding((10*d).toInt(),(7*d).toInt(),(10*d).toInt(),(6*d).toInt()) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding((12*d).toInt(),0,(10*d).toInt(),0); background=rounded(Color.rgb(16,17,25),9,Color.rgb(53,53,67)) }
        val logo=ImageView(this).apply { setImageResource(R.drawable.deckra_logo); scaleType=ImageView.ScaleType.FIT_START; contentDescription="DECKRA" }
        header.addView(logo,LinearLayout.LayoutParams((118*d).toInt(),(38*d).toInt()))
        val divider=View(this).apply { setBackgroundColor(Color.rgb(65,65,81)) }
        header.addView(divider,LinearLayout.LayoutParams((1*d).toInt(),(24*d).toInt()).apply { leftMargin=(12*d).toInt(); rightMargin=(12*d).toInt() })
        header.addView(label("DECKRA CONTROL CENTER",10,muted),LinearLayout.LayoutParams(0,-2,1f))
        status=label("●  PC NÃO CONECTADO",10,Color.rgb(255,190,80))
        header.addView(status)
        val toggle=action("CONECTAR") { connectionPanel.visibility=if(connectionPanel.visibility==View.VISIBLE) View.GONE else View.VISIBLE }
        header.addView(toggle,LinearLayout.LayoutParams((92*d).toInt(),(34*d).toInt()).apply { leftMargin=(8*d).toInt() })
        root.addView(header,LinearLayout.LayoutParams(-1,(44*d).toInt()))
        connectionPanel=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding((8*d).toInt(),(3*d).toInt(),(8*d).toInt(),(3*d).toInt()); background=rounded(Color.rgb(18,20,29),9,Color.rgb(37,38,50)); visibility=View.GONE }
        hostInput=EditText(this).apply { hint="IP do PC"; setTextColor(Color.WHITE); setHintTextColor(muted); inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI; setSingleLine(true); textSize=12f }
        connectionPanel.addView(hostInput,LinearLayout.LayoutParams(0,(40*d).toInt(),1f))
        pin=EditText(this).apply { hint="PIN de 6 dígitos"; setTextColor(Color.WHITE); setHintTextColor(muted); inputType=2; setSingleLine(true); textSize=12f }
        connectionPanel.addView(pin,LinearLayout.LayoutParams(0,(40*d).toInt(),1f).apply { leftMargin=(7*d).toInt() })
        connectionPanel.addView(action("PROCURAR") { discoverPc() },LinearLayout.LayoutParams((82*d).toInt(),(34*d).toInt()).apply { leftMargin=(7*d).toInt() })
        connectionPanel.addView(action("PAREAR") { connectPc() },LinearLayout.LayoutParams((82*d).toInt(),(34*d).toInt()).apply { leftMargin=(5*d).toInt() })
        root.addView(connectionPanel,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(5*d).toInt() })
        val body=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val sidebar=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((6*d).toInt(),(12*d).toInt(),(6*d).toInt(),(5*d).toInt()); background=rounded(Color.rgb(16,18,27),9,Color.rgb(36,38,51)) }
        navBar=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        sidebar.addView(navBar,LinearLayout.LayoutParams(-1,0,1f))
        sidebar.addView(label("DECKRA MOBILE",8,muted).apply { gravity=Gravity.CENTER })
        body.addView(sidebar,LinearLayout.LayoutParams((142*d).toInt(),-1).apply { rightMargin=(8*d).toInt() })
        val scroll=ScrollView(this).apply { clipToPadding=false; isFillViewport=true }
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((5*d).toInt(),(7*d).toInt(),(5*d).toInt(),(8*d).toInt()) }
        scroll.addView(content)
        body.addView(scroll,LinearLayout.LayoutParams(0,-1,1f))
        root.addView(body,LinearLayout.LayoutParams(-1,0,1f).apply { topMargin=(7*d).toInt() })
        updateButton=action("VERIFICAR ATUALIZAÇÃO") { checkForUpdates() }.apply { textSize=9f }
        root.addView(updateButton,LinearLayout.LayoutParams(-1,(28*d).toInt()).apply { topMargin=(5*d).toInt() })
        renderNavigation()
        setContentView(root)
    }

    private fun renderPage() {
        content.removeAllViews()
        val d=resources.displayMetrics.density
        val pageTitle=label(when(currentPage) {
            "Configuração" -> "Configure seu controle"
            "Aplicativos" -> "Aplicativos"
            else -> "Atalhos das teclas"
        },23,Color.rgb(241,241,248))
        pageTitle.typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD)
        content.addView(pageTitle)
        when(currentPage) {
            "Configuração" -> {
                val columns=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.TOP }
                val left=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
                val right=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
                columns.addView(left,LinearLayout.LayoutParams(0,-2,1f).apply { rightMargin=(7*d).toInt() })
                columns.addView(right,LinearLayout.LayoutParams(0,-2,1f).apply { leftMargin=(7*d).toInt() })
                val state=latestState
                val output=state?.optJSONObject("output")
                val volume=((output?.optDouble("volume",0.0) ?: 0.0)*100).toInt().coerceIn(0,100)
                val vol=createCard("◖))","Volume por aplicativo",d)
                vol.second.addView(label("VOLUME GERAL",10,Color.rgb(155,122,255)))
                vol.second.addView(label(output?.optString("name") ?: "Aguardando conexão",10,muted))
                val dial=TextView(this).apply { text=volume.toString()+"%"; textSize=27f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); typeface=android.graphics.Typeface.DEFAULT_BOLD; background=rounded(Color.rgb(39,34,64),70,purple) }
                vol.second.addView(dial,LinearLayout.LayoutParams((88*d).toInt(),(88*d).toInt()).apply { gravity=Gravity.CENTER; topMargin=(4*d).toInt(); bottomMargin=(4*d).toInt() })
                val master=SeekBar(this).apply { max=100; progress=volume; progressTintList=android.content.res.ColorStateList.valueOf(purple); thumbTintList=android.content.res.ColorStateList.valueOf(Color.rgb(162,138,255)) }
                master.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean) { dial.text=value.toString()+"%" }
                    override fun onStartTrackingTouch(bar:SeekBar?) {}
                    override fun onStopTrackingTouch(bar:SeekBar?) { send("set_master_volume",JSONObject().put("value",(bar?.progress ?: 0)/100f)) }
                })
                vol.second.addView(master)
                vol.second.addView(label("Arraste para ajustar o volume",9,muted).apply { gravity=Gravity.CENTER })
                vol.second.addView(actionRow("VOLUME +",{ send("volume_up") },"VOLUME −",{ send("volume_down") }))
                vol.second.addView(actionRow("MUTAR",{ send("volume_mute") },"PLAY / PAUSE",{ send("media_play_pause") }))
                vol.second.addView(actionRow("PRÓXIMA",{ send("media_next") },"ANTERIOR",{ send("media_previous") }))
                left.addView(vol.first)

                val mic=state?.optJSONObject("microphone")
                val micCard=createCard("◖))","Microfone",d)
                micCard.second.addView(label(mic?.optString("name") ?: "Entrada não conectada",10,muted))
                val micVolume=((mic?.optDouble("volume",0.0) ?: 0.0)*100).toInt().coerceIn(0,100)
                micCard.second.addView(label("Volume: "+micVolume+"%",11,Color.WHITE))
                val micSeek=SeekBar(this).apply { max=100; progress=micVolume; progressTintList=android.content.res.ColorStateList.valueOf(purple); thumbTintList=android.content.res.ColorStateList.valueOf(Color.rgb(162,138,255)) }
                micSeek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean) {}
                    override fun onStartTrackingTouch(bar:SeekBar?) {}
                    override fun onStopTrackingTouch(bar:SeekBar?) { send("set_mic_volume",JSONObject().put("value",(bar?.progress ?: 0)/100f)) }
                })
                micCard.second.addView(micSeek)
                micCard.second.addView(action(if(mic?.optBoolean("mute")==true) "ATIVAR MICROFONE" else "SILENCIAR MICROFONE") { send("set_mic_mute",JSONObject().put("value",mic?.optBoolean("mute")!=true)) })
                left.addView(micCard.first,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(7*d).toInt() })

                val keys=createCard("▦","Atalhos das teclas",d)
                val keyGrid=GridLayout(this).apply { columnCount=3; rowCount=2 }
                for(i in 1..6) {
                    val key=TextView(this).apply { text=i.toString().padStart(2,'0'); gravity=Gravity.CENTER; textSize=13f; setTextColor(muted); background=rounded(Color.rgb(35,37,49),9,Color.rgb(52,54,66)) }
                    keyGrid.addView(key,GridLayout.LayoutParams().apply { width=0; height=(42*d).toInt(); columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins((3*d).toInt(),(3*d).toInt(),(3*d).toInt(),(3*d).toInt()) })
                }
                keys.second.addView(keyGrid)
                keys.second.addView(label("CONTROLADOR NÃO INTEGRADO",10,Color.rgb(162,138,255)))
                keys.second.addView(label("As atribuições serão habilitadas após a integração do firmware e do protocolo do controlador. Estes elementos são apenas indicadores visuais.",10,muted))
                val eq=state?.optJSONObject("equalizer")
                keys.second.addView(label("EQUALIZADOR",10,Color.rgb(155,122,255)))
                keys.second.addView(label(if(eq?.optBoolean("available")==true) "Disponível" else "Requer driver APO ou dispositivo virtual no Windows.",9,muted))
                right.addView(keys.first)
                content.addView(columns,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(8*d).toInt() })
            }
            "Aplicativos" -> {
                val apps=latestState?.optJSONArray("applications") ?: JSONArray()
                if(apps.length()==0) {
                    val empty=createCard("♫","Volume por aplicativo",d)
                    empty.second.addView(label("Nenhuma sessão de áudio ativa foi encontrada. Abra um aplicativo e reproduza algum áudio no Windows.",11,muted))
                    content.addView(empty.first)
                }
                for(i in 0 until apps.length()) {
                    val app=apps.optJSONObject(i) ?: continue
                    val card=createCard("♫",app.optString("name","Aplicativo"),d)
                    card.second.addView(label("VOLUME "+(app.optDouble("volume",0.0)*100).toInt()+"%",10,Color.rgb(155,122,255)))
                    val seek=SeekBar(this).apply { max=100; progress=(app.optDouble("volume",0.0)*100).toInt(); progressTintList=android.content.res.ColorStateList.valueOf(purple); thumbTintList=android.content.res.ColorStateList.valueOf(Color.rgb(162,138,255)) }
                    seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean) {}
                        override fun onStartTrackingTouch(bar:SeekBar?) {}
                        override fun onStopTrackingTouch(bar:SeekBar?) { send("set_app_volume",JSONObject().put("processId",app.optInt("processId")).put("value",bar?.progress?.div(100f) ?: 0f)) }
                    })
                    card.second.addView(seek)
                    card.second.addView(action(if(app.optBoolean("mute")) "ATIVAR SOM" else "SILENCIAR") { send("set_app_mute",JSONObject().put("processId",app.optInt("processId")).put("value",!app.optBoolean("mute"))) })
                    content.addView(card.first,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(7*d).toInt() })
                }
                content.addView(action("ATUALIZAR LISTA") { send("get_state") })
            }
            else -> {
                val card=createCard("▦","Configuração das teclas",d)
                card.second.addView(label("O firmware e o protocolo do controlador ainda não estão integrados à Bridge Windows. A configuração de teclas será habilitada quando essa integração estiver disponível.",11,muted))
                content.addView(card.first)
            }
        }
        if(!paired) content.addView(label("Conecte o DECKRA Bridge no Windows e pareie pelo cabeçalho.",10,Color.rgb(255,190,90)))
    }

    private fun renderNavigation() {
        if(!::navBar.isInitialized) return
        navBar.removeAllViews()
        listOf("Configuração","Aplicativos","Teclas").forEachIndexed { index,page ->
            val selected=page==currentPage
            val item=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                gravity=Gravity.CENTER_VERTICAL
                setPadding(9,9,5,9)
                background=rounded(if(selected) Color.rgb(36,32,62) else Color.TRANSPARENT,8,if(selected) purple else Color.TRANSPARENT)
                addView(label(listOf("⚙","▦","▤")[index],18,if(selected) Color.rgb(152,121,255) else muted))
                addView(label(page,11,if(selected) Color.rgb(240,237,255) else muted),LinearLayout.LayoutParams(-1,-2).apply { leftMargin=7 })
                setOnClickListener { currentPage=page; renderNavigation(); renderPage() }
            }
            navBar.addView(item,LinearLayout.LayoutParams(-1,(47*resources.displayMetrics.density).toInt()).apply { bottomMargin=5 })
        }
    }

    private fun rounded(color:Int,radius:Int,stroke:Int=Color.TRANSPARENT)=GradientDrawable().apply {
        setColor(color); cornerRadius=radius*resources.displayMetrics.density
        if(stroke!=Color.TRANSPARENT) setStroke((1*resources.displayMetrics.density).toInt().coerceAtLeast(1),stroke)
    }
    private fun createCard(icon:String,title:String,density:Float):Pair<LinearLayout,LinearLayout> {
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=rounded(Color.rgb(18,20,29),11,Color.rgb(37,38,50)) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding((10*density).toInt(),0,(8*density).toInt(),0) }
        header.addView(label(icon,20,Color.rgb(143,112,255)))
        header.addView(label(title,14,Color.rgb(238,238,246)),LinearLayout.LayoutParams(0,-2,1f).apply { leftMargin=(8*density).toInt() })
        card.addView(header,LinearLayout.LayoutParams(-1,(43*density).toInt()))
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((10*density).toInt(),(7*density).toInt(),(10*density).toInt(),(9*density).toInt()) }
        card.addView(body)
        return Pair(card,body)
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
