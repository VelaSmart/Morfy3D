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
    private var currentPage = "Início"
    private var themeMode = "system"
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var pin: EditText
    private lateinit var hostInput: EditText
    private lateinit var updateButton: Button
    private lateinit var navBar: LinearLayout
    private lateinit var connectionPanel: LinearLayout
    private var latestState: JSONObject? = null
    private var bg = Color.rgb(14,15,24)
    private var surface = Color.rgb(16,18,27)
    private var panel = Color.rgb(23,25,39)
    private var card = Color.rgb(18,20,29)
    private var stroke = Color.rgb(43,45,61)
    private var primaryText = Color.rgb(241,241,248)
    private var secondaryText = Color.rgb(157,160,184)
    private var purple = Color.rgb(137,101,255)
    private var muted = Color.rgb(157,160,184)
    private var isLightTheme = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadThemePreference()
        applyTheme()
        buildShell()
        renderPage()
    }

    private fun buildShell() {
        val d=resources.displayMetrics.density
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding((4*d).toInt(),(4*d).toInt(),(4*d).toInt(),(4*d).toInt()) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding((12*d).toInt(),0,(10*d).toInt(),0); background=rounded(surface,9,stroke) }
        val logo=ImageView(this).apply { setImageResource(R.drawable.deckra_logo); scaleType=ImageView.ScaleType.FIT_CENTER; adjustViewBounds=true; contentDescription="DECKRA" }
        header.addView(logo,LinearLayout.LayoutParams((82*d).toInt(),(34*d).toInt()))
        val divider=View(this).apply { setBackgroundColor(Color.rgb(65,65,81)) }
        header.addView(divider,LinearLayout.LayoutParams((1*d).toInt(),(24*d).toInt()).apply { leftMargin=(12*d).toInt(); rightMargin=(12*d).toInt() })
        header.addView(label("CONTROL CENTER",8,muted),LinearLayout.LayoutParams(0,-2,1f))
        status=label("●  PC NÃO CONECTADO",10,Color.rgb(255,190,80))
        header.addView(status)
        val toggle=action("CONECTAR") { connectionPanel.visibility=if(connectionPanel.visibility==View.VISIBLE) View.GONE else View.VISIBLE }
        header.addView(toggle,LinearLayout.LayoutParams((72*d).toInt(),(32*d).toInt()).apply { leftMargin=(8*d).toInt() })
        root.addView(header,LinearLayout.LayoutParams(-1,(44*d).toInt()))
        connectionPanel=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding((8*d).toInt(),(3*d).toInt(),(8*d).toInt(),(3*d).toInt()); background=rounded(surface,9,stroke); visibility=View.GONE }
        hostInput=EditText(this).apply { hint="IP do PC"; setTextColor(primaryText); setHintTextColor(secondaryText); inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI; setSingleLine(true); textSize=12f }
        connectionPanel.addView(hostInput,LinearLayout.LayoutParams(0,(40*d).toInt(),1f))
        pin=EditText(this).apply { hint="PIN de 6 dígitos"; setTextColor(Color.WHITE); setHintTextColor(muted); inputType=2; setSingleLine(true); textSize=12f }
        connectionPanel.addView(pin,LinearLayout.LayoutParams(0,(40*d).toInt(),1f).apply { leftMargin=(7*d).toInt() })
        connectionPanel.addView(action("PROCURAR") { discoverPc() },LinearLayout.LayoutParams((82*d).toInt(),(34*d).toInt()).apply { leftMargin=(7*d).toInt() })
        connectionPanel.addView(action("PAREAR") { connectPc() },LinearLayout.LayoutParams((82*d).toInt(),(34*d).toInt()).apply { leftMargin=(5*d).toInt() })
        root.addView(connectionPanel,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(5*d).toInt() })
        val body=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val sidebar=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((3*d).toInt(),(8*d).toInt(),(3*d).toInt(),(4*d).toInt()); background=rounded(surface,9,stroke) }
        navBar=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        sidebar.addView(navBar,LinearLayout.LayoutParams(-1,0,1f))
        sidebar.addView(label("DECKRA MOBILE",8,muted).apply { gravity=Gravity.CENTER })
        body.addView(sidebar,LinearLayout.LayoutParams((94*d).toInt(),-1).apply { rightMargin=(8*d).toInt() })
        val pageFrame=FrameLayout(this).apply { clipChildren=true; clipToPadding=true }
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((5*d).toInt(),(7*d).toInt(),(5*d).toInt(),(5*d).toInt()) }
        pageFrame.addView(content,FrameLayout.LayoutParams(-1,-1))
        body.addView(pageFrame,LinearLayout.LayoutParams(0,-1,1f))
        root.addView(body,LinearLayout.LayoutParams(-1,0,1f).apply { topMargin=(7*d).toInt() })
        renderNavigation()
        setContentView(root)
    }

    private fun renderPage() {
        content.removeAllViews()
        val d=resources.displayMetrics.density
        if(currentPage!="Início") content.addView(label(currentPage,23,primaryText).apply { typeface=android.graphics.Typeface.DEFAULT_BOLD })
        when(currentPage) {
            "Início" -> renderHome(d)
            "Aplicativos" -> renderApplications(d)
            "Microfone" -> renderMicrophone(d)
            "Teclas" -> renderKeys(d)
            else -> renderSettings(d)
        }
        if(!paired) content.addView(label("Conecte o DECKRA Bridge no Windows e pareie pelo cabeçalho.",10,Color.rgb(255,170,65)))
    }

    private fun renderHome(d:Float) {
        val apps=latestState?.optJSONArray("applications") ?: JSONArray()
        val channels=createCard("♫","Canais de áudio",d)
        channels.first.getChildAt(0).layoutParams=channels.first.getChildAt(0).layoutParams.apply { height=(34*d).toInt() }
        channels.second.setPadding((7*d).toInt(),(3*d).toInt(),(7*d).toInt(),(4*d).toInt())
        channels.second.addView(label("Controle individual dos aplicativos ativos",9,secondaryText))
        if(apps.length()==0) {
            channels.second.addView(label("Nenhum aplicativo com áudio ativo. Abra um programa no PC para controlá-lo aqui.",11,secondaryText))
        } else {
            val grid=GridLayout(this).apply { columnCount=4; rowCount=(apps.length()+3)/4; useDefaultMargins=false }
            for(i in 0 until apps.length()) {
                val app=apps.optJSONObject(i) ?: continue
                val tile=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL
                    gravity=Gravity.CENTER
                    setPadding((4*d).toInt(),(3*d).toInt(),(4*d).toInt(),(3*d).toInt())
                    background=rounded(surface,10,stroke)
                }
                val initial=(app.optDouble("volume",1.0)*100).toInt().coerceIn(0,100)
                tile.addView(RotaryDialView(this,initial) { volume ->
                    send("set_app_volume",JSONObject().put("processId",app.optInt("processId")).put("value",volume/100f))
                },LinearLayout.LayoutParams((58*d).toInt(),(58*d).toInt()))
                tile.addView(label(app.optString("name","Aplicativo"),10,primaryText).apply {
                    gravity=Gravity.CENTER
                    maxLines=1
                    ellipsize=android.text.TextUtils.TruncateAt.END
                },LinearLayout.LayoutParams(-1,-2).apply { topMargin=(2*d).toInt() })
                tile.addView(action(if(app.optBoolean("mute")) "ATIVAR SOM" else "MUTAR") {
                    send("set_app_mute",JSONObject().put("processId",app.optInt("processId")).put("value",!app.optBoolean("mute")))
                }.apply { textSize=9f; minHeight=(23*d).toInt(); setPadding(2,0,2,0) },
                    LinearLayout.LayoutParams(-1,(23*d).toInt()).apply { topMargin=(2*d).toInt() })
                grid.addView(tile,GridLayout.LayoutParams().apply {
                    width=0
                    height=(91*d).toInt()
                    columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
                    setMargins((2*d).toInt(),(3*d).toInt(),(2*d).toInt(),(3*d).toInt())
                })
            }
            channels.second.addView(grid)
        }
        content.addView(channels.first)
    }

    private fun renderApplications(d:Float) {
        val apps=latestState?.optJSONArray("applications") ?: JSONArray()
        if(apps.length()==0) content.addView(label("Nenhum aplicativo com áudio ativo.",11,secondaryText))
        for(i in 0 until apps.length()) {
            val app=apps.optJSONObject(i) ?: continue
            val item=createCard("♫",app.optString("name","Aplicativo"),d)
            val volume=(app.optDouble("volume",1.0)*100).toInt().coerceIn(0,100)
            item.second.addView(RotaryDialView(this,volume) { value ->
                send("set_app_volume",JSONObject().put("processId",app.optInt("processId")).put("value",value/100f))
            },LinearLayout.LayoutParams((88*d).toInt(),(88*d).toInt()).apply { gravity=Gravity.CENTER_HORIZONTAL })
            item.second.addView(action(if(app.optBoolean("mute")) "ATIVAR SOM" else "SILENCIAR") {
                send("set_app_mute",JSONObject().put("processId",app.optInt("processId")).put("value",!app.optBoolean("mute")))
            })
            content.addView(item.first,LinearLayout.LayoutParams(-1,-2).apply { topMargin=(5*d).toInt() })
        }
        content.addView(action("ATUALIZAR LISTA") { send("get_state") })
    }

    private fun renderMicrophone(d:Float) {
        val mic=latestState?.optJSONObject("microphone")
        val item=createCard("♩","Entrada de áudio",d)
        item.second.addView(label(mic?.optString("name") ?: "Microfone não identificado",11,secondaryText))
        val volume=((mic?.optDouble("volume",0.0) ?: 0.0)*100).toInt().coerceIn(0,100)
        item.second.addView(RotaryDialView(this,volume) { value ->
            send("set_mic_volume",JSONObject().put("value",value/100f))
        },LinearLayout.LayoutParams((100*d).toInt(),(100*d).toInt()).apply { gravity=Gravity.CENTER_HORIZONTAL })
        item.second.addView(action(if(mic?.optBoolean("mute")==true) "ATIVAR MICROFONE" else "SILENCIAR MICROFONE") {
            send("set_mic_mute",JSONObject().put("value",mic?.optBoolean("mute")!=true))
        })
        content.addView(item.first)
    }

    private fun renderKeys(d:Float) {
        val keys=createCard("▦","Atalhos das teclas",d)
        val grid=GridLayout(this).apply { columnCount=3; rowCount=2 }
        for(i in 1..6) {
            val key=TextView(this).apply { text=i.toString().padStart(2,'0'); gravity=Gravity.CENTER; textSize=15f; setTextColor(secondaryText); background=rounded(panel,9,stroke) }
            grid.addView(key,GridLayout.LayoutParams().apply { width=0; height=(52*d).toInt(); columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(3,3,3,3) })
        }
        keys.second.addView(grid)
        keys.second.addView(label("CONTROLADOR NÃO INTEGRADO",10,purple))
        keys.second.addView(label("As atribuições serão habilitadas após a integração do firmware e do protocolo do controlador. Estes elementos são apenas indicadores visuais.",10,secondaryText))
        val eq=latestState?.optJSONObject("equalizer")
        keys.second.addView(label("EQUALIZADOR",10,purple))
        keys.second.addView(label(if(eq?.optBoolean("available")==true) "Disponível na Bridge." else "Requer driver APO ou dispositivo virtual no Windows.",10,secondaryText))
        content.addView(keys.first)
    }

    private fun renderSettings(d:Float) {
        val appearance=createCard("◐","Aparência",d)
        appearance.second.addView(label("Por padrão, o aplicativo acompanha o tema do celular.",10,secondaryText))
        val group=RadioGroup(this).apply { orientation=RadioGroup.VERTICAL }
        listOf("system" to "Automático (seguir sistema)","dark" to "Escuro","light" to "Claro").forEach { (mode,title) ->
            val radio=RadioButton(this).apply { id=View.generateViewId(); tag=mode; text=title; setTextColor(primaryText); buttonTintList=android.content.res.ColorStateList.valueOf(purple) }
            group.addView(radio); if(themeMode==mode) group.check(radio.id)
        }
        group.setOnCheckedChangeListener { g,id ->
            themeMode=g.findViewById<RadioButton>(id)?.tag?.toString() ?: "system"
            getSharedPreferences("deckra_preferences",Context.MODE_PRIVATE).edit().putString("theme_mode",themeMode).apply()
            applyTheme(); renderNavigation(); renderPage()
        }
        appearance.second.addView(group); content.addView(appearance.first)
        val updates=createCard("↻","Atualizações",d)
        updates.second.addView(label("Verifique e instale novas versões do DECKRA Mobile.",10,secondaryText))
        updateButton=action("VERIFICAR ATUALIZAÇÃO") { checkForUpdates() }.apply { textSize=11f }
        updates.second.addView(updateButton); content.addView(updates.first)
        val about=createCard("ⓘ","Sobre o aplicativo",d)
        about.second.addView(label("DECKRA Mobile • Controle de áudio para Windows",10,secondaryText))
        content.addView(about.first)
    }

    private fun renderNavigation() {
        if(!::navBar.isInitialized) return
        navBar.removeAllViews()
        val pages=listOf("Início","Aplicativos","Microfone","Teclas","Configurações")
        val icons=listOf("⌂","▦","♩","▤","⚙")
        pages.forEachIndexed { index,page ->
            val selected=page==currentPage
            val item=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(8,9,4,9)
                background=rounded(if(selected) if(isLightTheme) Color.rgb(235,228,255) else Color.rgb(36,32,62) else Color.TRANSPARENT,8,if(selected) purple else Color.TRANSPARENT)
                addView(label(icons[index],17,purple))
                addView(label(page,10,if(selected) primaryText else secondaryText),LinearLayout.LayoutParams(-1,-2).apply { leftMargin=6 })
                setOnClickListener { currentPage=page; renderNavigation(); renderPage() }
            }
            navBar.addView(item,LinearLayout.LayoutParams(-1,(43*resources.displayMetrics.density).toInt()).apply { bottomMargin=4 })
        }
    }

    private fun loadThemePreference() {
        themeMode=getSharedPreferences("deckra_preferences",Context.MODE_PRIVATE).getString("theme_mode","system") ?: "system"
    }

    private fun applyTheme() {
        val night=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        isLightTheme=when(themeMode) { "light" -> true; "dark" -> false; else -> !night }
        if(isLightTheme) {
            bg=Color.rgb(244,245,250); surface=Color.WHITE; panel=Color.rgb(248,248,252); card=Color.WHITE
            stroke=Color.rgb(222,223,235); primaryText=Color.rgb(31,31,45); secondaryText=Color.rgb(105,107,128); muted=secondaryText
        } else {
            bg=Color.rgb(9,10,16); surface=Color.rgb(16,18,27); panel=Color.rgb(23,25,39); card=Color.rgb(18,20,29)
            stroke=Color.rgb(43,45,61); primaryText=Color.rgb(241,241,248); secondaryText=Color.rgb(157,160,184); muted=secondaryText
        }
        purple=Color.rgb(137,101,255)
        window.statusBarColor=bg; window.navigationBarColor=bg
        if(::root.isInitialized) {
            root.setBackgroundColor(bg)
            if(root.childCount>0) root.getChildAt(0).background=rounded(surface,9,stroke)
            if(root.childCount>2) {
                val body=root.getChildAt(2) as? LinearLayout
                if(body!=null && body.childCount>0) body.getChildAt(0).background=rounded(surface,9,stroke)
            }
        }
        if(::connectionPanel.isInitialized) connectionPanel.background=rounded(surface,9,stroke)
    }
    private inner class RotaryDialView(
        context:Context,
        initialValue:Int,
        private val onCommit:(Int)->Unit
    ):View(context) {
        private var value=initialValue.coerceIn(0,100)
        private val density=resources.displayMetrics.density
        private val track=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style=android.graphics.Paint.Style.STROKE
            strokeWidth=5.5f*density
            strokeCap=android.graphics.Paint.Cap.ROUND
            color=if(isLightTheme) Color.rgb(215,211,229) else Color.rgb(54,53,72)
        }
        private val active=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style=android.graphics.Paint.Style.STROKE
            strokeWidth=5.5f*density
            strokeCap=android.graphics.Paint.Cap.ROUND
            color=purple
        }
        private val pointer=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color=purple
            strokeWidth=3.5f*density
            strokeCap=android.graphics.Paint.Cap.ROUND
        }
        private val center=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color=if(isLightTheme) Color.rgb(235,232,244) else Color.rgb(35,34,48) }
        private val text=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color=primaryText
            textAlign=android.graphics.Paint.Align.CENTER
            textSize=15f*density
            typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)
        }
        private val bounds=android.graphics.RectF()

        override fun onDraw(canvas:android.graphics.Canvas) {
            super.onDraw(canvas)
            val cx=width/2f
            val cy=height/2f
            val radius=(minOf(width,height)*0.37f)
            bounds.set(cx-radius,cy-radius,cx+radius,cy+radius)
            canvas.drawArc(bounds,135f,270f,false,track)
            canvas.drawArc(bounds,135f,270f*value/100f,false,active)
            val angle=Math.toRadians((135.0+270.0*value/100.0))
            val px=cx+ kotlin.math.cos(angle).toFloat()*(radius-1f*density)
            val py=cy+ kotlin.math.sin(angle).toFloat()*(radius-1f*density)
            canvas.drawLine(cx,cy,px,py,pointer)
            canvas.drawCircle(cx,cy,5f*density,center)
            canvas.drawText("$value%",cx,cy+5f*density,text)
        }

        override fun onTouchEvent(event:android.view.MotionEvent):Boolean {
            when(event.action) {
                android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE -> {
                    val dx=event.x-width/2f
                    val dy=event.y-height/2f
                    var angle=Math.toDegrees(kotlin.math.atan2(dy.toDouble(),dx.toDouble()))
                    if(angle<135.0) angle+=360.0
                    value=(((angle-135.0)/270.0)*100.0).toInt().coerceIn(0,100)
                    invalidate()
                    return true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onCommit(value)
                    performClick()
                    return true
                }
                android.view.MotionEvent.ACTION_CANCEL -> return true
            }
            return true
        }

        override fun performClick():Boolean {
            super.performClick()
            return true
        }
    }

    private fun rounded(color:Int,radius:Int,stroke:Int=Color.TRANSPARENT)=GradientDrawable().apply {
        setColor(color); cornerRadius=radius*resources.displayMetrics.density
        if(stroke!=Color.TRANSPARENT) setStroke((1*resources.displayMetrics.density).toInt().coerceAtLeast(1),stroke)
    }
    private fun createCard(icon:String,title:String,density:Float):Pair<LinearLayout,LinearLayout> {
        val cardView=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=rounded(this@MainActivity.card,11,stroke) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding((10*density).toInt(),0,(8*density).toInt(),0) }
        header.addView(label(icon,20,Color.rgb(143,112,255)))
        header.addView(label(title,14,primaryText),LinearLayout.LayoutParams(0,-2,1f).apply { leftMargin=(8*density).toInt() })
        cardView.addView(header,LinearLayout.LayoutParams(-1,(43*density).toInt()))
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((10*density).toInt(),(7*density).toInt(),(10*density).toInt(),(9*density).toInt()) }
        cardView.addView(body)
        return Pair(cardView,body)
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
