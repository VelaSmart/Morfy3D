package com.velasmart.deckramobile

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.util.Base64
import android.view.DragEvent
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sin

/**
 * DECKRA Mobile: um clone do DECKRA APP do PC no celular. As mesmas abas (Início, Aplicativos,
 * Microfone, Equalizador, Configurações), os mesmos canais DECKRA 1 a 4 e o mesmo visual. Conversa
 * pelo Wi-Fi com o DECKRA APP (Configurações › DECKRA Mobile), que manda o estado de tudo.
 */
class MainActivity : Activity() {
    // ---------- conexão ----------
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var socket: WebSocket? = null
    private var host = ""
    private var paired = false
    private var deckra: JSONObject? = null           // estado completo do DECKRA APP (protocolo 3)
    private var antigo = false                       // conectado a um PC sem o protocolo 3
    private var statusText = "Não conectado"
    private var statusColor = 0
    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            if (paired) { send("get_deckra", silencioso = true); handler.postDelayed(this, 450) }
        }
    }
    private val icones = mutableMapOf<String, Bitmap?>()
    private val prefs get() = getSharedPreferences("deckra_preferences", Context.MODE_PRIVATE)
    /** Identificador deste celular (aleatório, criado uma vez) e a chave que o PC deu ao acertar o PIN. */
    private val deviceId: String by lazy {
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device_id", it).apply() }
    }
    private var chavePc = ""
    private var tentando = false                     // conexão em andamento
    private var automatico = false                   // esta conexão é a volta automática (com a chave)
    private var jaProcurou = false                   // a volta automática já procurou o PC pela rede
    private var estiloVolume = "circulo"             // aba Aplicativos: "circulo" ou "deslizante"

    // ---------- tela ----------
    private var page = "inicio"
    private var themeMode = "system"
    private var light = false
    private lateinit var root: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var statusDot: View
    private lateinit var statusLabel: TextView
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private var hostInput: EditText? = null
    private var pinInput: EditText? = null
    private var updateButton: TextView? = null
    private var awaitingInstallPermission = false
    private var assinatura = ""                       // muda quando a estrutura da tela precisa ser refeita
    private val atualizadores = mutableListOf<(JSONObject) -> Unit>()

    // ---------- cores (as mesmas do DECKRA APP) ----------
    private var cFundo = 0; private var cLateral = 0; private var cPainel = 0; private var cBorda = 0
    private var cTexto = 0; private var cSuave = 0; private var cFraco = 0; private var cSelecao = 0
    private var cDisco = 0; private var cTrilho = 0; private var cBotaoTopo = 0; private var cBotaoBase = 0
    private var cBotaoBorda = 0; private var cMarca = 0; private var cCampo = 0; private var cDiv = 0
    private val cRoxo = Color.rgb(140, 107, 255)
    private val cRoxoClaro = Color.rgb(165, 140, 255)
    private val cAzul = Color.rgb(61, 123, 255)
    private val cRoxoArco = Color.rgb(140, 82, 255)
    private val cOk = Color.rgb(92, 201, 138)
    private val cMudo = Color.rgb(255, 107, 129)
    private val cAlerta = Color.rgb(224, 168, 74)

    private val d get() = resources.displayMetrics.density
    private val retrato get() = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    private val inter: Typeface? by lazy { try { resources.getFont(R.font.inter) } catch (_: Exception) { null } }
    private fun fonte(forte: Boolean): Typeface {
        val base = inter ?: return if (forte) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT
        return if (Build.VERSION.SDK_INT >= 28) Typeface.create(base, if (forte) 600 else 400, false)
        else if (forte) Typeface.create(base, Typeface.BOLD) else base
    }
    private fun dp(v: Float) = (v * d).toInt()
    private fun dp(v: Int) = (v * d).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("deckra_preferences", Context.MODE_PRIVATE)
        themeMode = prefs.getString("theme_mode", "system") ?: "system"
        host = prefs.getString("last_host", "") ?: ""
        chavePc = prefs.getString("pc_token", "") ?: ""
        estiloVolume = prefs.getString("estilo_volume", "circulo") ?: "circulo"
        applyTheme()
        if (!retrato) page = "deckra"
        buildShell()
        renderPage()
        handler.postDelayed({ checkForUpdates(manual = false) }, 2500)
        voltarSozinho(400)
    }

    // =====================================================================
    //  Tema
    // =====================================================================
    private fun applyTheme() {
        val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        light = when (themeMode) { "light" -> true; "dark" -> false; else -> !night }
        if (light) {
            cFundo = Color.rgb(238, 239, 249); cLateral = Color.rgb(232, 233, 246); cPainel = Color.rgb(246, 246, 255)
            cBorda = Color.rgb(214, 216, 236); cTexto = Color.rgb(28, 29, 44); cSuave = Color.rgb(96, 98, 122)
            cFraco = Color.rgb(140, 142, 166); cSelecao = Color.rgb(221, 212, 255); cDisco = Color.rgb(232, 233, 246)
            cTrilho = Color.rgb(214, 216, 236); cBotaoTopo = Color.rgb(250, 250, 255); cBotaoBase = Color.rgb(226, 227, 242)
            cBotaoBorda = Color.rgb(190, 192, 214); cMarca = Color.rgb(36, 37, 56); cCampo = Color.rgb(240, 240, 252)
            cFundo = Color.rgb(244, 243, 251); cLateral = cFundo; cDiv = Color.rgb(217, 216, 234)
        } else {
            cFundo = Color.rgb(16, 14, 29); cLateral = Color.rgb(19, 17, 35); cPainel = Color.rgb(22, 23, 37); cDiv = Color.rgb(30, 31, 51)
            cBorda = Color.rgb(38, 40, 62); cTexto = Color.rgb(236, 236, 244); cSuave = Color.rgb(139, 141, 166)
            cFraco = Color.rgb(92, 94, 120); cSelecao = Color.rgb(40, 32, 84); cDisco = Color.rgb(19, 20, 33)
            cTrilho = Color.rgb(38, 40, 64); cBotaoTopo = Color.rgb(44, 45, 66); cBotaoBase = Color.rgb(21, 22, 35)
            cBotaoBorda = Color.rgb(62, 64, 92); cMarca = Color.rgb(235, 236, 248); cCampo = Color.rgb(27, 28, 44)
        }
        statusColor = cAlerta
        window.statusBarColor = cFundo
        window.navigationBarColor = cFundo
    }

    // =====================================================================
    //  Estrutura: menu lateral + página (igual ao DECKRA APP)
    // =====================================================================
    private var divLateral: View? = null

    private fun buildShell() {
        val vertical = retrato
        root = LinearLayout(this).apply { orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL; setBackgroundColor(cFundo) }
        statusDot = View(this).apply { background = oval(statusColor) }
        statusLabel = text(statusText, 11f, cTexto).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER; isVerticalScrollBarEnabled = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content, FrameLayout.LayoutParams(-1, -2))
        if (vertical) {
            // celular em pé: marca + status em cima, abas embaixo (como a prévia)
            val topo = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(10), dp(18), dp(10))
            }
            topo.addView(ImageView(this).apply { setImageResource(R.drawable.deckra_logo); scaleType = ImageView.ScaleType.FIT_CENTER },
                LinearLayout.LayoutParams(dp(22), dp(22)))
            topo.addView(text("D E C K R A", 12f, cTexto, bold = true).apply { letterSpacing = 0.12f },
                LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = rounded(cPainel, 40, cBorda); setPadding(dp(10), dp(5), dp(12), dp(5))
            }
            chip.addView(statusDot, LinearLayout.LayoutParams(dp(7), dp(7)))
            chip.addView(statusLabel, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })
            topo.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
            root.addView(topo, LinearLayout.LayoutParams(-1, -2))
            root.addView(View(this).apply { setBackgroundColor(cDiv) }, LinearLayout.LayoutParams(-1, dp(1)))
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            root.addView(View(this).apply { setBackgroundColor(cDiv) }, LinearLayout.LayoutParams(-1, dp(1)))
            nav = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; setBackgroundColor(cLateral); setPadding(dp(6), dp(6), dp(6), dp(8))
            }
            root.addView(nav, LinearLayout.LayoutParams(-1, -2))
            sidebar = LinearLayout(this); divLateral = null
        } else {
            sidebar = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setBackgroundColor(cLateral); setPadding(dp(12), dp(14), dp(12), dp(12))
            }
            val marca = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            marca.addView(ImageView(this).apply {
                setImageResource(R.drawable.deckra_logo); scaleType = ImageView.ScaleType.FIT_CENTER; adjustViewBounds = true
            }, LinearLayout.LayoutParams(dp(26), dp(26)))
            marca.addView(text("D E C K R A", 13f, cTexto, bold = true).apply { letterSpacing = 0.12f },
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(10) })
            sidebar.addView(marca, LinearLayout.LayoutParams(-1, dp(36)))
            sidebar.addView(View(this).apply { setBackgroundColor(cDiv) },
                LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(10); bottomMargin = dp(10) })
            nav = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            sidebar.addView(nav, LinearLayout.LayoutParams(-1, 0, 1f))
            sidebar.addView(View(this).apply { setBackgroundColor(cDiv) },
                LinearLayout.LayoutParams(-1, dp(1)).apply { bottomMargin = dp(8) })
            val linhaStatus = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            linhaStatus.addView(statusDot, LinearLayout.LayoutParams(dp(8), dp(8)))
            linhaStatus.addView(statusLabel, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            sidebar.addView(linhaStatus)
            sidebar.addView(text("v${versionName()} beta · Morfy3D", 10f, cFraco), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
            root.addView(sidebar, LinearLayout.LayoutParams(dp(178), -1))
            divLateral = View(this).apply { setBackgroundColor(cDiv) }
            root.addView(divLateral, LinearLayout.LayoutParams(dp(1), -1))
            root.addView(scroll, LinearLayout.LayoutParams(0, -1, 1f))
        }
        window.statusBarColor = if (vertical) cFundo else cFundo
        window.navigationBarColor = if (vertical) cLateral else cFundo
        setContentView(root)
        renderNav()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // deitou o celular: vira o Modo DECKRA; ficou em pé: volta para as abas
        if (retrato && page == "deckra") page = "inicio"
        else if (!retrato) page = "deckra"
        applyTheme(); buildShell(); renderPage()
    }

    private val pages get() = if (retrato) listOf("inicio" to ("⌂" to "Início"), "apps" to ("☰" to "Apps"),
        "mic" to ("◉" to "Mic"), "eq" to ("♫" to "EQ"), "ajustes" to ("⚙" to "Ajustes"))
    else listOf("deckra" to ("▦" to "Modo DECKRA"), "inicio" to ("⌂" to "Início"), "apps" to ("☰" to "Aplicativos"),
        "mic" to ("◉" to "Microfone"), "eq" to ("♫" to "Equalizador"), "ajustes" to ("⚙" to "Configurações"))

    private fun irPara(chave: String) { if (page != chave) { page = chave; renderNav(); renderPage() } }

    private fun renderNav() {
        nav.removeAllViews()
        for ((chave, par) in pages) {
            val sel = chave == page
            if (retrato) {
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(5))
                    background = if (sel) rounded(cSelecao, 14, cRoxo) else null
                    isClickable = true; setOnClickListener { irPara(chave) }
                }
                item.addView(text(par.first, 17f, if (sel) cRoxoClaro else cFraco).apply { gravity = Gravity.CENTER })
                item.addView(text(par.second, 10f, if (sel) cTexto else cFraco, bold = true).apply { gravity = Gravity.CENTER })
                nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
                continue
            }
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(8), 0)
                background = if (sel) rounded(cSelecao, 10, cRoxo) else null
                isClickable = true
                setOnClickListener { irPara(chave) }
            }
            item.addView(text(par.first, 16f, if (sel) cRoxoClaro else cSuave).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(dp(22), -2))
            item.addView(text(par.second, 13f, if (sel) cTexto else cSuave, bold = true), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(10) })
            nav.addView(item, LinearLayout.LayoutParams(-1, dp(40)).apply { bottomMargin = dp(5) })
        }
    }

    private fun setStatus(texto: String, cor: Int) {
        statusText = texto; statusColor = cor
        statusLabel.text = texto
        statusDot.background = oval(cor)
    }

    // =====================================================================
    //  Páginas: cada uma monta a estrutura e registra "atualizadores" que pintam os valores
    // =====================================================================
    private fun renderPage() {
        content.removeAllViews()
        atualizadores.clear()
        hostInput = null; pinInput = null; updateButton = null
        assinatura = estrutura(deckra)
        scroll.scrollTo(0, 0)
        val modo = page == "deckra"
        if (!retrato) { sidebar.visibility = if (modo) View.GONE else View.VISIBLE; divLateral?.visibility = sidebar.visibility }
        if (modo) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (modo) content.setPadding(dp(12), dp(10), dp(14), dp(10))
        else if (retrato) content.setPadding(dp(16), dp(14), dp(16), dp(18))
        else content.setPadding(dp(22), dp(16), dp(22), dp(18))
        when (page) {
            "deckra" -> renderDeckra()
            "inicio" -> renderHome()
            "apps" -> renderApps()
            "mic" -> renderMic()
            "eq" -> renderEq()
            else -> renderSettings()
        }
        deckra?.let { s -> atualizadores.forEach { it(s) } }
        refreshPolling()
    }

    /** O que muda a estrutura da tela (quais canais e aplicativos existem). */
    private fun estrutura(s: JSONObject?): String {
        val canais = s?.optJSONArray("canais") ?: return "-"
        return (0 until canais.length()).joinToString(",") { canais.optJSONObject(it)?.optString("alvo") ?: "" }
    }

    private fun aoReceberEstado(s: JSONObject) {
        deckra = s
        if (estrutura(s) != assinatura) renderPage() else atualizadores.forEach { it(s) }
        val controle = s.optString("controle").takeIf { it.isNotBlank() && it != "null" }
        setStatus("PC conectado", cOk)
        statusLabel.text = if (controle != null) "PC · $controle" else "PC conectado"
    }

    private fun header(titulo: String, subtitulo: String) {
        content.addView(text(titulo, 24f, cTexto, bold = true))
        content.addView(text(subtitulo, 12f, cSuave), LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(12) })
    }

    /** Sem conexão (ou PC antigo): mostra como conectar e para por aí. */
    private fun precisaConexao(): Boolean {
        avisoAtualizacao()?.let { content.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }) }
        if (!paired) { content.addView(connectCard(compacto = true)); return true }
        if (antigo) {
            val c = card("↻", "Atualize o DECKRA APP do PC")
            c.second.addView(text("Este celular é um clone do DECKRA APP e precisa da versão 1.12.7 ou mais nova no PC. " +
                "Abra o DECKRA no PC › Configurações › Atualizações.", 12f, cSuave))
            content.addView(c.first); return true
        }
        if (deckra == null) { content.addView(text("Carregando…", 13f, cSuave)); return true }
        return false
    }

    private fun canais(): List<JSONObject> {
        val arr = deckra?.optJSONArray("canais") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    // ---------- Início ----------
    private fun renderHome() {
        header("Início", "Tudo do seu DECKRA em um só lugar.")
        if (precisaConexao()) return
        // Canais (os 4 do DECKRA)
        val c = card("🔊", "Canais", link = "Escolher aplicativos  ›") { page = "apps"; renderNav(); renderPage() }
        val linha = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (canal in canais().take(4)) {
            val i = canal.optInt("indice")
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            val dial = DialView(this).apply { onChange = { v -> send("canal_volume", JSONObject().put("indice", i).put("valor", v / 100.0), true) } }
            val tamDisco = if (retrato) 70 else 96
            col.addView(dial, LinearLayout.LayoutParams(dp(tamDisco), dp(tamDisco)))
            val icone = ImageView(this)
            col.addView(icone, LinearLayout.LayoutParams(dp(22), dp(22)).apply { topMargin = dp(6) })
            val nome = text("", 12f, cTexto, bold = true).apply { gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
            col.addView(nome, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
            val mudo = PillButton(this) { send("canal_mudo", JSONObject().put("indice", i)) }
            col.addView(mudo, LinearLayout.LayoutParams(dp(tamDisco), dp(30)).apply { topMargin = dp(6) })
            if (retrato) mudo.textSize = 10f
            linha.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
            atualizadores += { s -> canalDe(s, i)?.let { k ->
                if (!dial.dragging) dial.setValue(k.optValor(), k.optBoolean("ativo") && !k.optBoolean("mudo"))
                nome.text = k.optString("nome"); mudo.setMuted(k.optBoolean("mudo")); pintarIcone(icone, k.optString("alvo"))
            } }
        }
        c.second.addView(linha)
        content.addView(c.first, LinearLayout.LayoutParams(-1, -2))

        val baixo = LinearLayout(this).apply { orientation = if (retrato) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
        fun lp(peso: Float) = if (retrato) LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            else LinearLayout.LayoutParams(0, -1, peso).apply { rightMargin = dp(10) }
        // Microfone
        val mic = card("◉", "Microfone")
        val micNome = text("", 10f, cSuave).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        mic.second.addView(micNome)
        val micDial = DialView(this).apply { onChange = { v -> send("mic_volume", JSONObject().put("valor", v / 100.0), true) } }
        mic.second.addView(micDial, LinearLayout.LayoutParams(dp(96), dp(96)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(4) })
        val micMudo = PillButton(this) { send("mic_mudo") }
        mic.second.addView(micMudo, LinearLayout.LayoutParams(dp(100), dp(30)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(6) })
        atualizadores += { s -> s.optJSONObject("mic")?.let { m ->
            micNome.text = m.optString("nome").ifBlank { "Nenhum microfone" }
            if (!micDial.dragging) micDial.setValue(m.optValor(), m.optBoolean("existe") && !m.optBoolean("mudo"))
            micMudo.setMuted(m.optBoolean("mudo"))
        } }
        baixo.addView(mic.first, lp(1f))
        // Mídia: o que está tocando no PC agora
        val md = card("♫", "Mídia")
        val mdApp = text("", 11f, cFraco).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        md.third.addView(mdApp)
        val mdInfo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val mdIcone = ImageView(this)
        mdInfo.addView(mdIcone, LinearLayout.LayoutParams(dp(32), dp(32)))
        val mdTextos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val mdTitulo = text("", 14f, cTexto, bold = true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        val mdArtista = text("", 11f, cSuave).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        mdTextos.addView(mdTitulo); mdTextos.addView(mdArtista)
        mdInfo.addView(mdTextos, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })
        md.second.addView(mdInfo)
        val mdVazio = text("Nada tocando agora", 13f, cSuave).apply { gravity = Gravity.CENTER }
        md.second.addView(mdVazio, LinearLayout.LayoutParams(-1, dp(60)))
        val mdT0 = text("0:00", 10f, cSuave); val mdT1 = text("", 10f, cSuave)
        val prog = ProgressoView(this).apply {
            aoArrastar = { seg -> mdT0.text = tempo(seg) }
            aoSoltar = { seg -> send("midia_buscar", JSONObject().put("segundos", seg)) }
        }
        md.second.addView(prog, LinearLayout.LayoutParams(-1, dp(26)).apply { topMargin = dp(8) })
        val mdTempos = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        mdTempos.addView(mdT0, LinearLayout.LayoutParams(0, -2, 1f)); mdTempos.addView(mdT1)
        md.second.addView(mdTempos)
        val mdBotoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val mdTocar = botaoMidia("▶", true) { send("midia_comando", JSONObject().put("acao", "tocar")) }
        mdBotoes.addView(botaoMidia("⏮", false) { send("midia_comando", JSONObject().put("acao", "anterior")) }, LinearLayout.LayoutParams(dp(38), dp(38)))
        mdBotoes.addView(mdTocar, LinearLayout.LayoutParams(dp(46), dp(46)).apply { leftMargin = dp(12); rightMargin = dp(12) })
        mdBotoes.addView(botaoMidia("⏭", false) { send("midia_comando", JSONObject().put("acao", "proxima")) }, LinearLayout.LayoutParams(dp(38), dp(38)))
        md.second.addView(mdBotoes, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        atualizadores += { s ->
            val suporta = temRecurso(s, "midia")
            val m = if (suporta) s.optJSONObject("midia") else null
            val tem = m != null
            for (v in listOf(mdInfo, prog, mdTempos, mdBotoes)) v.visibility = if (tem) View.VISIBLE else View.GONE
            mdVazio.visibility = if (tem) View.GONE else View.VISIBLE
            mdVazio.text = if (suporta) "Nada tocando agora" else "Atualize o DECKRA APP do PC (1.13.0 ou mais nova) para ver a mídia aqui."
            mdApp.text = m?.optString("app") ?: ""
            if (m != null) {
                mdTitulo.text = m.optString("titulo").ifBlank { "Sem título" }
                mdArtista.text = m.optString("artista")
                pintarIcone(mdIcone, m.optString("alvo"))
                val dur = m.optDouble("dur", 0.0); val pos = m.optDouble("pos", 0.0)
                prog.definir(pos, dur, m.optBoolean("pode_buscar"))
                if (!prog.arrastando) mdT0.text = tempo(pos)
                mdT1.text = if (dur > 0) tempo(dur) else ""
                prog.visibility = if (dur > 0) View.VISIBLE else View.INVISIBLE
                mdTocar.text = if (m.optBoolean("tocando")) "⏸" else "▶"
            }
        }
        if (retrato) { baixo.removeView(mic.first); baixo.addView(md.first, lp(1f)); baixo.addView(mic.first, lp(1f)) }
        else baixo.addView(md.first, lp(1f))
        // Dispositivos
        val disp = card("⚙", "Dispositivos")
        val ponto = View(this); val ctrl = text("", 12f, cTexto); val saida = text("", 12f, cTexto); val entrada = text("", 12f, cTexto)
        disp.second.addView(text("Controle", 10f, cFraco))
        disp.second.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(ponto, LinearLayout.LayoutParams(dp(8), dp(8))); addView(ctrl, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        })
        for ((rot, v) in listOf("Saída de som" to saida, "Microfone" to entrada)) {
            disp.second.addView(text(rot, 10f, cFraco), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
            disp.second.addView(v.apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
        }
        disp.second.addView(link("Configurações  ›") { page = "ajustes"; renderNav(); renderPage() })
        atualizadores += { s ->
            val nome = s.optString("controle").takeIf { it.isNotBlank() && it != "null" }
            ponto.background = oval(if (nome != null) cOk else cAlerta)
            ctrl.text = if (nome != null) "$nome conectado" else "Procurando o controle…"
            saida.text = s.optString("saida").ifBlank { "—" }; entrada.text = s.optString("entrada").ifBlank { "—" }
        }
        baixo.addView(disp.first, if (retrato) LinearLayout.LayoutParams(-1, -2) else LinearLayout.LayoutParams(0, -1, 1.05f))
        content.addView(baixo, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }

    // ---------- Modo DECKRA (celular deitado): vira uma mesa de som igual ao controle ----------
    private fun renderDeckra() {
        val tudo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        // lateral: marca, conexão e a mídia
        val lado = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(10), 0) }
        lado.addView(text("D E C K R A", 13f, cTexto, bold = true).apply { letterSpacing = 0.12f })
        lado.addView(text("MODO DECKRA", 10f, cRoxoClaro, bold = true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
        val chip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(cPainel, 40, cBorda); setPadding(dp(9), dp(4), dp(10), dp(4))
        }
        val chipPonto = View(this).apply { background = oval(statusColor) }
        val chipTxt = text(statusLabel.text.toString(), 10f, cTexto).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        chip.addView(chipPonto, LinearLayout.LayoutParams(dp(7), dp(7)))
        chip.addView(chipTxt, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })
        lado.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10) })
        val sair = pill("Sair do modo", false) { irPara("inicio") }.apply { textSize = 11f }
        val baixoLado = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tudo.addView(lado, LinearLayout.LayoutParams(dp(170), -1))
        tudo.addView(View(this).apply { setBackgroundColor(cDiv) }, LinearLayout.LayoutParams(dp(1), -1).apply { rightMargin = dp(12) })
        val palco = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tudo.addView(palco, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(tudo, LinearLayout.LayoutParams(-1, -2))
        atualizadores += { _ -> chipPonto.background = oval(statusColor); chipTxt.text = statusLabel.text }

        // topo do palco: estilo do volume + sair
        val topo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        for ((chave, rot) in listOf("circulo" to "Potenciômetro", "deslizante" to "Deslizante")) {
            topo.addView(pill(rot, estiloVolume == chave) {
                if (estiloVolume != chave) { estiloVolume = chave; prefs.edit().putString("estilo_volume", chave).apply(); renderPage() }
            }.apply { textSize = 11f }, LinearLayout.LayoutParams(dp(112), dp(30)).apply { rightMargin = dp(6) })
        }
        topo.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        topo.addView(sair, LinearLayout.LayoutParams(dp(110), dp(30)))
        palco.addView(topo, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        if (!paired || antigo || deckra == null) {
            content.removeView(tudo); topo.removeView(sair)
            val antes = content.childCount
            precisaConexao()
            if (content.childCount == antes) content.addView(text("Carregando…", 13f, cSuave))
            content.addView(sair, LinearLayout.LayoutParams(dp(140), dp(34)).apply { topMargin = dp(10) })
            return
        }

        // mídia na lateral
        val md = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(cPainel, 14, cBorda); setPadding(dp(10), dp(10), dp(10), dp(8)) }
        val mdApp = text("", 10f, cFraco).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        val mdTitulo = text("", 13f, cTexto, bold = true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        val mdArtista = text("", 10f, cSuave).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        md.addView(mdApp); md.addView(mdTitulo); md.addView(mdArtista)
        val prog = ProgressoView(this).apply { aoSoltar = { seg -> send("midia_buscar", JSONObject().put("segundos", seg)) } }
        md.addView(prog, LinearLayout.LayoutParams(-1, dp(22)).apply { topMargin = dp(4) })
        val mdBotoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val mdTocar = botaoMidia("▶", true) { send("midia_comando", JSONObject().put("acao", "tocar")) }
        mdBotoes.addView(botaoMidia("⏮", false) { send("midia_comando", JSONObject().put("acao", "anterior")) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        mdBotoes.addView(mdTocar, LinearLayout.LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(8); rightMargin = dp(8) })
        mdBotoes.addView(botaoMidia("⏭", false) { send("midia_comando", JSONObject().put("acao", "proxima")) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        md.addView(mdBotoes, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
        baixoLado.addView(md, LinearLayout.LayoutParams(-1, -2))
        lado.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        lado.addView(baixoLado, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        atualizadores += { s ->
            val m = if (temRecurso(s, "midia")) s.optJSONObject("midia") else null
            md.visibility = if (m != null) View.VISIBLE else View.GONE
            if (m != null) {
                mdApp.text = m.optString("app"); mdTitulo.text = m.optString("titulo").ifBlank { "Sem título" }; mdArtista.text = m.optString("artista")
                val dur = m.optDouble("dur", 0.0)
                prog.definir(m.optDouble("pos", 0.0), dur, m.optBoolean("pode_buscar"))
                prog.visibility = if (dur > 0) View.VISIBLE else View.INVISIBLE
                mdTocar.text = if (m.optBoolean("tocando")) "⏸" else "▶"
            }
        }

        // os canais lado a lado
        val lista = canais().take(8)
        val n = lista.size.coerceAtLeast(1)
        val largura = resources.displayMetrics.widthPixels / d
        val alturaTela = resources.displayMetrics.heightPixels / d
        val col = ((largura - 170 - 1 - 12 - 26 - n * 6) / n).coerceIn(56f, 150f)
        val deslizante = estiloVolume == "deslizante"
        val disco = (col - 10).coerceAtMost(alturaTela - 200).coerceIn(48f, 120f).toInt()
        val alturaFader = (alturaTela - 210).coerceIn(90f, 220f).toInt()
        val linha = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (canal in lista) {
            val i = canal.optInt("indice")
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                background = rounded(cPainel, 14, cBorda); setPadding(dp(4), dp(8), dp(4), dp(8))
            }
            val icone = ImageView(this).apply { isClickable = true; setOnClickListener { escolherAplicativo(i) } }
            c.addView(icone, LinearLayout.LayoutParams(dp(24), dp(24)))
            val nome = text("", 10f, cTexto, bold = true).apply { gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
            c.addView(nome, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
            val envio: (Int) -> Unit = { x -> send("canal_volume", JSONObject().put("indice", i).put("valor", x / 100.0), true) }
            val dial = DialView(this).apply { onChange = envio }
            val fader = FaderView(this).apply { onChange = envio }
            if (deslizante) c.addView(fader, LinearLayout.LayoutParams(dp(44), dp(alturaFader)).apply { topMargin = dp(6) })
            else c.addView(dial, LinearLayout.LayoutParams(dp(disco), dp(disco)).apply { topMargin = dp(8); bottomMargin = dp(4) })
            val pct = text("", 12f, cTexto, bold = true).apply { gravity = Gravity.CENTER }
            c.addView(pct, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
            val vu = VuView(this)
            c.addView(vu, LinearLayout.LayoutParams((col * 0.75f * d).toInt(), dp(5)).apply { topMargin = dp(4) })
            val mudo = PillButton(this) { send("canal_mudo", JSONObject().put("indice", i)) }.apply { textSize = 10f }
            c.addView(mudo, LinearLayout.LayoutParams(-1, dp(28)).apply { topMargin = dp(6) })
            linha.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
            atualizadores += { s -> canalDe(s, i)?.let { k ->
                val ativo = k.optBoolean("ativo") && !k.optBoolean("mudo")
                if (!dial.dragging) dial.setValue(k.optValor(), ativo)
                if (!fader.dragging) fader.setValue(k.optValor(), ativo, k.optBoolean("mudo"))
                val valor = if (fader.dragging) fader.value else k.optValor()
                pct.text = if (valor == null) "—" else "$valor%"
                pct.setTextColor(if (ativo) cTexto else cSuave)
                nome.text = k.optString("nome"); mudo.setMuted(k.optBoolean("mudo"))
                vu.setLevel(k.optDouble("nivel", 0.0).toFloat()); pintarIcone(icone, k.optString("alvo"))
            } }
        }
        palco.addView(linha, LinearLayout.LayoutParams(-1, -2))
        palco.addView(text("Toque no ícone de um canal para trocar o aplicativo. A tela fica ligada enquanto o Modo DECKRA estiver aberto.", 10f, cFraco),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    // ---------- Aplicativos ----------
    private fun renderApps() {
        val deslizante = estiloVolume == "deslizante"
        header("Aplicativos", "Arraste o ${if (deslizante) "deslizante" else "círculo"} para ajustar o volume e escolha o aplicativo na caixa. " +
            "Para trocar canais de lugar, segure o cartão e arraste.")
        if (precisaConexao()) return
        val linha = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val rolagem = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        for (canal in canais()) {
            val i = canal.optInt("indice")
            val v = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                background = rounded(cPainel, 14, cBorda); setPadding(dp(10), dp(12), dp(10), dp(12))
            }
            ligarArrasto(v, i, rolagem)
            v.addView(text(canal.optString("titulo"), 13f, cRoxoClaro, bold = true))
            val envio: (Int) -> Unit = { x -> send("canal_volume", JSONObject().put("indice", i).put("valor", x / 100.0), true) }
            val vu = VuView(this).apply { vertical = deslizante }
            val dial = DialView(this).apply { marcador = true; onChange = envio }
            val fader = FaderView(this).apply { onChange = envio }
            val pct = text("", 15f, cTexto, bold = true).apply { gravity = Gravity.CENTER }
            if (deslizante) {
                // potenciômetro deslizante com o medidor em pé ao lado e o volume embaixo
                val par = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
                par.addView(fader, LinearLayout.LayoutParams(dp(56), dp(136)))
                par.addView(vu, LinearLayout.LayoutParams(dp(6), dp(118)).apply { leftMargin = dp(14) })
                v.addView(par, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
                v.addView(pct, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
            } else {
                v.addView(vu, LinearLayout.LayoutParams(dp(120), dp(6)).apply { topMargin = dp(8) })
                v.addView(dial, LinearLayout.LayoutParams(dp(128), dp(128)).apply { topMargin = dp(6) })
            }
            v.addView(text("Aplicativo", 10f, cFraco), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            val seletor = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = rounded(cCampo, 9, cBorda); setPadding(dp(8), 0, dp(8), 0); isClickable = true
                setOnClickListener { escolherAplicativo(i) }
            }
            val icone = ImageView(this)
            seletor.addView(icone, LinearLayout.LayoutParams(dp(20), dp(20)))
            val nome = text("", 12f, cTexto).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
            seletor.addView(nome, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
            seletor.addView(text("⌄", 13f, cSuave))
            v.addView(seletor, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(3) })
            val estado = text("", 11f, cSuave).apply { gravity = Gravity.CENTER; maxLines = 2 }
            v.addView(estado, LinearLayout.LayoutParams(-1, dp(32)).apply { topMargin = dp(6) })
            val mudo = PillButton(this) { send("canal_mudo", JSONObject().put("indice", i)) }
            v.addView(mudo, LinearLayout.LayoutParams(dp(112), dp(34)).apply { topMargin = dp(4) })
            linha.addView(v, LinearLayout.LayoutParams(dp(170), -2).apply { rightMargin = dp(12) })
            atualizadores += { s -> canalDe(s, i)?.let { k ->
                val ativo = k.optBoolean("ativo") && !k.optBoolean("mudo")
                if (!dial.dragging) dial.setValue(k.optValor(), ativo)
                if (!fader.dragging) fader.setValue(k.optValor(), ativo, k.optBoolean("mudo"))
                val valor = if (fader.dragging) fader.value else k.optValor()
                pct.text = if (valor == null) "—" else "$valor%"
                pct.setTextColor(if (ativo) cTexto else cSuave)
                nome.text = k.optString("nome"); estado.text = k.optString("estado"); mudo.setMuted(k.optBoolean("mudo"))
                vu.setLevel(k.optDouble("nivel", 0.0).toFloat()); pintarIcone(icone, k.optString("alvo"))
            } }
        }
        rolagem.addView(linha)
        content.addView(rolagem, LinearLayout.LayoutParams(-1, -2))
    }

    /** Segurar o cartão e arrastar até outro: os dois canais trocam de lugar (igual ao DECKRA APP). */
    private fun ligarArrasto(cartao: View, indice: Int, rolagem: HorizontalScrollView) {
        cartao.setOnLongClickListener {
            if (!temRecurso(deckra, "trocar")) { toast("Atualize o DECKRA APP do PC (1.13.0 ou mais nova) para trocar canais de lugar."); return@setOnLongClickListener true }
            cartao.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            cartao.startDragAndDrop(ClipData.newPlainText("canal", indice.toString()), View.DragShadowBuilder(cartao), indice, 0)
            cartao.alpha = 0.3f
            true
        }
        cartao.setOnDragListener { alvo, ev ->
            val de = ev.localState as? Int
            when (ev.action) {
                DragEvent.ACTION_DRAG_ENTERED -> if (de != indice) alvo.background = rounded(cSelecao, 14, cRoxo)
                DragEvent.ACTION_DRAG_EXITED -> alvo.background = rounded(cPainel, 14, cBorda)
                DragEvent.ACTION_DRAG_LOCATION -> {
                    // perto da borda: a fileira rola sozinha
                    val x = alvo.left + ev.x - rolagem.scrollX
                    if (x < dp(56)) rolagem.scrollBy(-dp(14), 0) else if (x > rolagem.width - dp(56)) rolagem.scrollBy(dp(14), 0)
                }
                DragEvent.ACTION_DROP -> {
                    alvo.background = rounded(cPainel, 14, cBorda)
                    if (de != null && de != indice) send("canal_trocar", JSONObject().put("de", de).put("para", indice))
                }
                DragEvent.ACTION_DRAG_ENDED -> { alvo.alpha = 1f; alvo.background = rounded(cPainel, 14, cBorda) }
            }
            true
        }
    }

    private fun temRecurso(s: JSONObject?, nome: String): Boolean {
        val r = s?.optJSONArray("recursos") ?: return false
        for (k in 0 until r.length()) if (r.optString(k) == nome) return true
        return false
    }

    private fun tempo(seg: Double): String {
        val t = seg.toLong().coerceAtLeast(0)
        val h = t / 3600; val m = (t % 3600) / 60; val s_ = t % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s_) else "%d:%02d".format(m, s_)
    }

    private fun botaoMidia(simbolo: String, principal: Boolean, acao: () -> Unit) = TextView(this).apply {
        text = simbolo; gravity = Gravity.CENTER; textSize = if (principal) 17f else 14f; isClickable = true
        setTextColor(if (principal) Color.WHITE else cTexto)
        background = if (principal) GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cAzul, cRoxoArco)).apply { shape = GradientDrawable.OVAL }
            else GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cCampo); setStroke(dp(1).coerceAtLeast(1), cBorda) }
        setOnClickListener { acao() }
    }

    private var escolhendo = -1
    private fun escolherAplicativo(indice: Int) { escolhendo = indice; send("get_opcoes") }

    private fun mostrarOpcoes(opcoes: JSONArray) {
        val itens = mutableListOf<Pair<String?, String>>()
        for (k in 0 until opcoes.length()) {
            val o = opcoes.optJSONObject(k) ?: continue
            itens += Pair(if (o.isNull("alvo")) null else o.optString("alvo"), o.optString("nome"))
        }
        val escolhiveis = itens.filter { it.first != null }
        if (escolhendo < 0 || escolhiveis.isEmpty()) return
        val indice = escolhendo
        android.app.AlertDialog.Builder(this)
            .setTitle("Aplicativo do ${canalDe(deckra, indice)?.optString("titulo") ?: "canal"}")
            .setItems(escolhiveis.map { it.second }.toTypedArray()) { _, k ->
                send("canal_alvo", JSONObject().put("indice", indice).put("alvo", escolhiveis[k].first))
            }
            .setNegativeButton("Cancelar", null)
            .show()
        escolhendo = -1
    }

    // ---------- Microfone ----------
    private fun renderMic() {
        header("Microfone", "Regule o volume de captação e silencie o microfone.")
        if (precisaConexao()) return
        val painel = LinearLayout(this).apply {
            orientation = if (retrato) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(cPainel, 14, cBorda); setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        val esq = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        esq.addView(text("◉  Microfone em uso", 15f, cTexto, bold = true))
        val nome = text("", 12f, cSuave).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
        esq.addView(nome, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        val linhaEstado = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val ponto = View(this); val estado = text("", 12f, cSuave)
        linhaEstado.addView(ponto, LinearLayout.LayoutParams(dp(8), dp(8)))
        linhaEstado.addView(estado, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        esq.addView(linhaEstado, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        esq.addView(text("O ajuste vale para o microfone padrão do Windows, em qualquer programa (Discord, Teams, jogos, gravação…). " +
            "Para controlar pelo potenciômetro, escolha \"Microfone\" em um dos canais.", 11f, cFraco),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        painel.addView(esq, if (retrato) LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
            else LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(16) })
        val dir = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        val vu = VuView(this)
        dir.addView(vu, LinearLayout.LayoutParams(dp(150), dp(6)))
        val dial = DialView(this).apply { big = true; marcador = true; onChange = { v -> send("mic_volume", JSONObject().put("valor", v / 100.0), true) } }
        dir.addView(dial, LinearLayout.LayoutParams(dp(170), dp(170)).apply { topMargin = dp(8) })
        val mudo = PillButton(this) { send("mic_mudo") }
        dir.addView(mudo, LinearLayout.LayoutParams(dp(130), dp(38)).apply { topMargin = dp(10) })
        painel.addView(dir, LinearLayout.LayoutParams(-2, -2))
        content.addView(painel, LinearLayout.LayoutParams(-1, -2))
        atualizadores += { s -> s.optJSONObject("mic")?.let { m ->
            val existe = m.optBoolean("existe"); val mud = m.optBoolean("mudo")
            nome.text = m.optString("nome").ifBlank { "Nenhum microfone encontrado" }
            ponto.background = oval(if (!existe) cAlerta else if (mud) cMudo else cOk)
            estado.text = if (!existe) "Nenhum microfone encontrado" else if (mud) "Microfone mudo" else "Microfone ativo"
            if (!dial.dragging) dial.setValue(m.optValor(), existe && !mud)
            mudo.setMuted(mud); vu.setLevel(m.optDouble("nivel", 0.0).toFloat())
        } }
    }

    // ---------- Equalizador ----------
    private fun renderEq() {
        val cab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titulos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titulos.addView(text("Equalizador", 24f, cTexto, bold = true))
        titulos.addView(text("Ajuste graves, médios e agudos do som de todo o Windows.", 12f, cSuave))
        cab.addView(titulos, LinearLayout.LayoutParams(0, -2, 1f))
        val rotulo = text("", 13f, cSuave, bold = true)
        val chave = Chave(this) { send("eq_ligar") }
        if (paired && !antigo && deckra != null) {
            cab.addView(rotulo); cab.addView(chave, LinearLayout.LayoutParams(dp(40), dp(22)).apply { leftMargin = dp(10) })
        }
        content.addView(cab, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        if (precisaConexao()) return
        val painel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = rounded(cPainel, 14, cBorda); setPadding(dp(18), dp(14), dp(18), dp(16))
        }
        val linhaStatus = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val ponto = View(this); val status = text("", 12f, cSuave)
        linhaStatus.addView(ponto, LinearLayout.LayoutParams(dp(8), dp(8)))
        linhaStatus.addView(status, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        painel.addView(linhaStatus)
        val perfis = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val pillsPerfil = mutableListOf<Pair<String, TextView>>()
        for (k in 0 until (deckra?.optJSONObject("eq")?.optJSONArray("perfis")?.length() ?: 0)) {
            val nome = deckra!!.optJSONObject("eq")!!.optJSONArray("perfis")!!.optString(k)
            val p = pill(nome, false) { send("eq_perfil", JSONObject().put("nome", nome)) }
            pillsPerfil += Pair(nome, p)
            perfis.addView(p, LinearLayout.LayoutParams(dp(92), dp(30)).apply { rightMargin = dp(8) })
        }
        painel.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(perfis) },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val curva = CurvaView(this, mini = false)
        painel.addView(curva, LinearLayout.LayoutParams(-1, dp(110)).apply { topMargin = dp(10) })
        val bandas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bArr = deckra?.optJSONObject("eq")?.optJSONArray("bandas") ?: JSONArray()
        val sliders = mutableListOf<Pair<String, BandaView>>()
        for (k in 0 until bArr.length()) {
            val b = bArr.optJSONObject(k) ?: continue
            val chaveB = b.optString("chave")
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            val valor = text("", 13f, cTexto, bold = true)
            col.addView(valor)
            val bv = BandaView(this, valor) { g -> send("eq_ganho", JSONObject().put("banda", chaveB).put("valor", g), true) }
            col.addView(bv, LinearLayout.LayoutParams(dp(if (retrato) 40 else 56), dp(130)).apply { topMargin = dp(4) })
            col.addView(text(b.optString("nome"), 12f, cTexto, bold = true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
            col.addView(text(b.optString("freq"), 10f, cSuave))
            bandas.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
            sliders += Pair(chaveB, bv)
        }
        painel.addView(bandas, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        content.addView(painel, LinearLayout.LayoutParams(-1, -2))
        atualizadores += { s -> s.optJSONObject("eq")?.let { e ->
            val ligado = e.optBoolean("ligado"); val pronto = e.optString("estado") == "ok"
            chave.ligado = ligado; rotulo.text = if (ligado) "Ligado" else "Desligado"; rotulo.setTextColor(if (ligado) cTexto else cSuave)
            val aviso = when { e.optString("estado") == "ausente" -> "Equalizer APO não encontrado no PC. Ele é gratuito; instale pelo DECKRA no PC."
                e.optString("aviso").isNotBlank() -> e.optString("aviso") + " Abra o Device Selector no PC e marque essa saída."
                ligado -> "Equalizador ativo no Windows"; else -> "Equalizador desligado" }
            status.text = aviso
            ponto.background = oval(if (e.optString("aviso").isNotBlank() || e.optString("estado") == "ausente") cAlerta else if (ligado) cOk else cFraco)
            for ((nome, p) in pillsPerfil) {
                val sel = nome == e.optString("perfil")
                p.background = rounded(if (sel) cSelecao else cCampo, 40, if (sel) cRoxo else cBorda); p.setTextColor(if (sel) cTexto else cSuave)
            }
            curva.definir(e.optJSONArray("curva"), ligado && pronto)
            val bs = e.optJSONArray("bandas") ?: JSONArray()
            for (k in 0 until bs.length()) {
                val b = bs.optJSONObject(k) ?: continue
                sliders.firstOrNull { it.first == b.optString("chave") }?.second?.definir(b.optDouble("ganho", 0.0).toFloat(),
                    e.optDouble("min", -12.0).toFloat(), e.optDouble("max", 12.0).toFloat(), ligado && pronto)
            }
        } }
    }

    // ---------- Configurações ----------
    private fun renderSettings() {
        header("Configurações", "Conexão com o PC, tema e atualizações.")
        avisoAtualizacao()?.let { content.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }) }
        content.addView(connectCard(compacto = false))
        val aparencia = card("◐", "Tema")
        aparencia.second.addView(text("Deixa o DECKRA Mobile escuro, claro ou igual ao tema do celular.", 12f, cSuave))
        val temas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((modo, nome) in listOf("system" to "Automático", "dark" to "Modo escuro", "light" to "Modo claro")) {
            temas.addView(pill(nome, modo == themeMode) {
                themeMode = modo
                getSharedPreferences("deckra_preferences", Context.MODE_PRIVATE).edit().putString("theme_mode", modo).apply()
                applyTheme(); buildShell(); renderPage()
            }, LinearLayout.LayoutParams(dp(118), dp(34)).apply { rightMargin = dp(8); topMargin = dp(8) })
        }
        aparencia.second.addView(temas)
        content.addView(aparencia.first, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val estilo = card("◎", "Controle de volume")
        estilo.second.addView(text("Como o volume aparece na aba Aplicativos: um círculo para girar ou um potenciômetro " +
            "deslizante, que sobe e desce.", 12f, cSuave))
        val estilos = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((modo, nome) in listOf("circulo" to "Círculo", "deslizante" to "Deslizante")) {
            estilos.addView(pill(nome, modo == estiloVolume) {
                estiloVolume = modo
                prefs.edit().putString("estilo_volume", modo).apply()
                renderPage()
            }, LinearLayout.LayoutParams(dp(118), dp(34)).apply { rightMargin = dp(8); topMargin = dp(8) })
        }
        estilo.second.addView(estilos)
        content.addView(estilo.first, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val att = card("↻", "Atualizações")
        att.second.addView(text("Versão instalada: ${versionName()} beta", 12f, cSuave))
        updateButton = pill(estadoAtualizacao.takeIf { novaVersao != null || it != "Ainda não verificado" } ?: "Verificar agora",
            novaVersao != null) { if (novaVersao != null) confirmarAtualizacao() else checkForUpdates(manual = true) }
        att.second.addView(updateButton, LinearLayout.LayoutParams(dp(240), dp(36)).apply { topMargin = dp(8) })
        content.addView(att.first, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val sobre = card("ⓘ", "Sobre")
        sobre.second.addView(text("DECKRA Mobile · acesso antecipado. O mesmo DECKRA APP do PC, no celular, pela rede Wi-Fi. " +
            "Morfy3D · morfy3d.com.br/deckra", 12f, cSuave))
        content.addView(sobre.first, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }

    /** Cartão de conexão: procurar o PC, IP, PIN e conectar. */
    private fun connectCard(compacto: Boolean): View {
        val c = card("📶", if (paired) "Conectado ao PC" else "Conectar ao PC")
        if (paired) {
            c.second.addView(text("Conectado a ${deckra?.optString("pc")?.ifBlank { null } ?: host} ($host). Da próxima vez ele " +
                "conecta sozinho, sem PIN. Desconectar faz o PC esquecer este celular (aí precisa do PIN de novo).", 12f, cSuave))
            c.second.addView(pill("Desconectar", false) { disconnect() }, LinearLayout.LayoutParams(dp(160), dp(36)).apply { topMargin = dp(10) })
            return c.first
        }
        c.second.addView(text(if (chavePc.isNotBlank())
            "Este celular já foi pareado: toque em Procurar ou Conectar, sem PIN. O DECKRA Mobile precisa estar ligado nas Configurações do DECKRA no PC."
        else "No PC, abra o DECKRA APP › Configurações › DECKRA Mobile e toque em \"Procurar celular\". " +
            "Depois toque em Procurar aqui e digite o PIN que aparece no PC. O PIN só é pedido na primeira vez.", 12f, cSuave))
        val linha = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        hostInput = field("IP do PC", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI).apply { setText(host) }
        pinInput = field(if (chavePc.isNotBlank()) "PIN (não precisa)" else "PIN de 6 dígitos", InputType.TYPE_CLASS_NUMBER)
        linha.addView(hostInput, LinearLayout.LayoutParams(0, dp(40), 1f))
        linha.addView(pinInput, LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(8) })
        val linha2 = if (retrato) LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL } else linha
        val larg = if (retrato) LinearLayout.LayoutParams(0, dp(42), 1f) else LinearLayout.LayoutParams(dp(104), dp(40))
        linha2.addView(pill("Procurar", false) { discoverPc() }, LinearLayout.LayoutParams(larg).apply { leftMargin = if (retrato) 0 else dp(8) })
        linha2.addView(pill("Conectar", true) { connectPc() }, LinearLayout.LayoutParams(larg).apply { leftMargin = dp(8) })
        c.second.addView(linha, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        if (retrato) c.second.addView(linha2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        if (!compacto) c.second.addView(text("Celular e PC precisam estar na mesma rede Wi-Fi. Na primeira vez, o Windows pergunta " +
            "se o DECKRA pode usar a rede: permita para redes privadas.", 11f, cFraco),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        return c.first
    }

    // =====================================================================
    //  Ajudantes dos dados
    // =====================================================================
    private fun canalDe(s: JSONObject?, indice: Int): JSONObject? {
        val arr = s?.optJSONArray("canais") ?: return null
        for (k in 0 until arr.length()) arr.optJSONObject(k)?.let { if (it.optInt("indice") == indice) return it }
        return null
    }

    private fun JSONObject.optValor(): Int? = if (isNull("valor")) null else optInt("valor").coerceIn(0, 100)

    private fun pintarIcone(v: ImageView, alvo: String) {
        if (alvo.isBlank() || alvo == "nenhum") { v.setImageDrawable(null); return }
        if (icones.containsKey(alvo)) { v.setImageBitmap(icones[alvo]); return }
        icones[alvo] = null
        send("get_icone", JSONObject().put("alvo", alvo), silencioso = true)
    }

    // =====================================================================
    //  Peças visuais
    // =====================================================================
    private fun text(valor: String, tam: Float, cor: Int, bold: Boolean = false) = TextView(this).apply {
        text = valor; textSize = tam; setTextColor(cor)
        typeface = fonte(bold)
    }

    private fun rounded(cor: Int, raio: Int, borda: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        setColor(cor); cornerRadius = raio * d
        if (borda != Color.TRANSPARENT) setStroke(dp(1).coerceAtLeast(1), borda)
    }

    /** Pílula em degradê azul -> roxo (botão escolhido / principal, como no DECKRA 2.0). */
    private fun degrade(raio: Int) = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cAzul, cRoxoArco)).apply {
        cornerRadius = raio * d
    }

    private fun oval(cor: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cor) }

    private fun link(rotulo: String, acao: () -> Unit) = text(rotulo, 11f, cSuave).apply {
        isClickable = true; setPadding(0, dp(8), 0, 0); setOnClickListener { acao() }
    }

    /** Cartão: (cartão, corpo, canto direito do cabeçalho). */
    private fun card(icone: String, titulo: String, link: String? = null, acaoLink: (() -> Unit)? = null): Triple<LinearLayout, LinearLayout, LinearLayout> {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = rounded(cPainel, 14, cBorda); setPadding(dp(16), dp(14), dp(16), dp(16))
        }
        val cab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        cab.addView(text(icone, 15f, cRoxoClaro))
        cab.addView(text(titulo, 15f, cTexto, bold = true), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })
        val canto = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        if (link != null && acaoLink != null) canto.addView(link(link, acaoLink).apply { setPadding(0, 0, 0, 0) })
        cab.addView(canto)
        c.addView(cab, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        val corpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(corpo)
        return Triple(c, corpo, canto)
    }

    private fun pill(rotulo: String, destaque: Boolean, acao: () -> Unit) = TextView(this).apply {
        text = rotulo; gravity = Gravity.CENTER; textSize = 13f; isClickable = true
        typeface = fonte(true)
        setTextColor(if (destaque) Color.WHITE else cTexto)
        background = if (destaque) degrade(40) else rounded(cCampo, 40, cBorda)
        setOnClickListener { acao() }
    }

    private fun field(dica: String, tipo: Int) = EditText(this).apply {
        hint = dica; inputType = tipo; setSingleLine(true); textSize = 13f
        setTextColor(cTexto); setHintTextColor(cFraco)
        background = rounded(cCampo, 10, cBorda); setPadding(dp(12), 0, dp(12), 0)
    }

    /** Interruptor liga/desliga igual ao do DECKRA APP. */
    private inner class Chave(ctx: Context, acao: () -> Unit) : View(ctx) {
        var ligado = false
            set(v) { if (field != v) { field = v; invalidate() } }
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        init { isClickable = true; setOnClickListener { acao() } }
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val r = h / 2
            if (ligado) p.shader = LinearGradient(0f, 0f, w, 0f, cAzul, cRoxoArco, Shader.TileMode.CLAMP)
            else { p.shader = null; p.color = if (light) Color.rgb(215, 217, 239) else Color.rgb(42, 44, 66) }
            c.drawRoundRect(0f, 0f, w, h, r, r, p)
            p.shader = null; p.color = if (ligado || light) Color.WHITE else Color.rgb(160, 162, 186)
            c.drawCircle(if (ligado) w - r else r, r, r * 0.7f, p)
        }
    }

    /** Curva de resposta do equalizador (os pontos vêm do PC). */
    private inner class CurvaView(ctx: Context, val mini: Boolean) : View(ctx) {
        private var pontos: JSONArray? = null
        private var ativo = false
        private val pLinha = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val pGrade = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f }
        private val caminho = Path()
        fun definir(p: JSONArray?, ativo: Boolean) { pontos = p; this.ativo = ativo; invalidate() }
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val meio = h / 2
            pGrade.color = cTrilho
            if (!mini) for (k in listOf(0.1f, 0.3f, 0.7f, 0.9f)) c.drawLine(0f, h * k, w, h * k, pGrade)
            c.drawLine(0f, meio, w, meio, pGrade)
            val p = pontos ?: return
            caminho.reset()
            val faixa = if (mini) 12f else 15f
            for (k in 0 until p.length()) {
                val ponto = p.optJSONArray(k) ?: continue
                val f = ponto.optDouble(0, 20.0); val db = ponto.optDouble(1, 0.0).toFloat().coerceIn(-faixa, faixa)
                val x = (w * log10(f / 20.0) / 3.0).toFloat(); val y = meio - db / faixa * (h / 2 - 3 * d)
                if (k == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
            }
            pLinha.strokeWidth = if (mini) 2.5f * d else 3f * d
            pLinha.color = if (ativo) misturar(cAzul, cRoxoArco, 0.6f) else misturar(cTrilho, cSuave, 0.5f)
            c.drawPath(caminho, pLinha)
        }
    }

    /** Controle deslizante vertical de uma banda do equalizador. */
    private inner class BandaView(ctx: Context, val rotulo: TextView, val aoMudar: (Double) -> Unit) : View(ctx) {
        private var ganho = 0f; private var gMin = -12f; private var gMax = 12f; private var ativo = false
        var arrastando = false
        private var ultimo = 0L
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun definir(g: Float, mn: Float, mx: Float, at: Boolean) {
            gMin = mn; gMax = mx; ativo = at
            if (!arrastando) ganho = g
            rotulo.text = "%s%.1f dB".format(if (ganho > 0) "+" else "", ganho).replace(".", ",")
            rotulo.setTextColor(if (ativo) cTexto else cSuave)
            invalidate()
        }
        private fun y(g: Float): Float { val t = (g - gMin) / (gMax - gMin); return height - 12 * d - t * (height - 24 * d) }
        override fun onDraw(c: Canvas) {
            val cx = width / 2f; val meio = y(0f); val yv = y(ganho)
            p.color = cTrilho; c.drawRoundRect(cx - 3 * d, 12 * d, cx + 3 * d, height - 12 * d, 3 * d, 3 * d, p)
            p.color = if (ativo) (if (ganho >= 0) cRoxoArco else cAzul) else cFraco
            c.drawRect(cx - 3 * d, min(yv, meio), cx + 3 * d, maxOf(yv, meio), p)
            p.color = if (ativo) cRoxo else Color.rgb(90, 92, 120); c.drawCircle(cx, yv, 11 * d, p)
            p.color = if (ativo) Color.rgb(245, 245, 252) else Color.rgb(150, 152, 172); c.drawCircle(cx, yv, 6 * d, p)
        }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    arrastando = true; parent?.requestDisallowInterceptTouchEvent(true)
                    val t = ((height - 12 * d - e.y) / (height - 24 * d)).coerceIn(0f, 1f)
                    ganho = Math.round((gMin + t * (gMax - gMin)) * 2) / 2f
                    rotulo.text = "%s%.1f dB".format(if (ganho > 0) "+" else "", ganho).replace(".", ",")
                    invalidate()
                    val agora = System.currentTimeMillis()
                    if (agora - ultimo > 90) { ultimo = agora; aoMudar(ganho.toDouble()) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    arrastando = false; parent?.requestDisallowInterceptTouchEvent(false); aoMudar(ganho.toDouble())
                }
            }
            return true
        }
    }

    /** Botão "Mutar / Mudo" igual ao do DECKRA APP. */
    private inner class PillButton(ctx: Context, acao: () -> Unit) : TextView(ctx) {
        private var mudo = false
        init {
            gravity = Gravity.CENTER; textSize = 13f; isClickable = true
            typeface = fonte(true)
            setOnClickListener { acao() }
            pintar()
        }
        fun setMuted(m: Boolean) { if (m != mudo || text.isNullOrEmpty()) { mudo = m; pintar() } }
        private fun pintar() {
            text = if (textSize < 11.5f * d) (if (mudo) "🔇 Mudo" else "🔊 Mutar") else if (mudo) "🔇  Mudo" else "🔊  Mutar"
            setTextColor(if (mudo) Color.rgb(255, 214, 220) else cTexto)
            background = if (mudo) rounded(Color.rgb(64, 24, 40), 40, cMudo) else rounded(cCampo, 40, cBorda)
        }
    }

    /** Barrinha de nível (verde, amarelo, vermelho) igual à dos cartões do DECKRA APP. */
    private inner class VuView(ctx: Context) : View(ctx) {
        var vertical = false                          // em pé ao lado do deslizante (o segmento 0 é o de baixo)
        private var nivel = 0f
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun setLevel(v: Float) {
            val alvo = if (v <= 0.016f) 0f else (1f + 20f * kotlin.math.log10(v) / 36f).coerceIn(0f, 1f)
            nivel = if (alvo >= nivel) alvo else maxOf(alvo, nivel - 0.18f)
            invalidate()
        }
        override fun onDraw(c: Canvas) {
            val n = 20
            val passo = (if (vertical) height else width).toFloat() / n
            val acesos = (nivel * n).toInt()
            for (k in 0 until n) {
                val f = (k + 1f) / n
                p.color = if (k < acesos) (if (f > 0.88f) Color.rgb(255, 90, 110) else if (f > 0.7f) Color.rgb(232, 197, 71) else Color.rgb(79, 209, 139)) else cTrilho
                if (vertical) {
                    val y1 = height - k * passo
                    c.drawRoundRect(0f, y1 - passo + 2 * d, width.toFloat(), y1, d, d, p)
                } else c.drawRoundRect(k * passo, 0f, k * passo + passo - 2 * d, height.toFloat(), d, d, p)
            }
        }
    }

    /** Círculo de volume do DECKRA APP: disco, trilho, arco em gradiente com brilho, botão e marcador. */
    private inner class DialView(ctx: Context) : View(ctx) {
        var value = 0; private set
        private var vazio = false
        private var ativo = true
        var big = false
        var marcador = false
        var dragging = false; private set
        var onChange: ((Int) -> Unit)? = null
        private var ultimoEnvio = 0L
        private val pDisco = Paint(Paint.ANTI_ALIAS_FLAG)
        private val pTrilho = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val pArco = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val pBrilho = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val pBotao = Paint(Paint.ANTI_ALIAS_FLAG)
        private val pContorno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val pMarca = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        private val pNum = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = fonte(true) }
        private val pPct = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
        private val caixa = RectF()
        init { setLayerType(LAYER_TYPE_SOFTWARE, null) }        // o brilho (blur) precisa de software

        fun setValue(v: Int?, ativo: Boolean) {
            val novoVazio = v == null
            val novo = (v ?: 0).coerceIn(0, 100)
            if (novo != value || ativo != this.ativo || novoVazio != vazio) { value = novo; vazio = novoVazio; this.ativo = ativo; invalidate() }
        }

        override fun onDraw(c: Canvas) {
            val w = min(width, height).toFloat()
            val cx = width / 2f; val cy = height / 2f
            val r0 = w * 0.485f
            pDisco.color = cDisco
            c.drawCircle(cx, cy, r0, pDisco)
            val lw = w * 0.058f
            val raio = w * 0.405f
            caixa.set(cx - raio, cy - raio, cx + raio, cy + raio)
            pTrilho.strokeWidth = lw; pTrilho.color = cTrilho
            c.drawArc(caixa, 135f, 270f, false, pTrilho)
            if (value > 0 && !vazio) {
                val a0 = if (ativo) cAzul else misturar(cTrilho, cFraco, 0.55f)
                val a1 = if (ativo) cRoxoArco else misturar(cTrilho, cSuave, 0.55f)
                val grad = SweepGradient(cx, cy, intArrayOf(a0, a1, a1, a0), floatArrayOf(0f, 0.75f, 0.76f, 1f))
                val m = android.graphics.Matrix(); m.setRotate(135f, cx, cy); grad.setLocalMatrix(m)
                pArco.shader = grad; pArco.strokeWidth = lw
                if (ativo) {
                    pBrilho.shader = grad; pBrilho.strokeWidth = lw * 1.6f; pBrilho.alpha = if (light) 110 else 170
                    pBrilho.maskFilter = BlurMaskFilter(w * 0.045f, BlurMaskFilter.Blur.NORMAL)
                    c.drawArc(caixa, 135f, 270f * value / 100f, false, pBrilho)
                }
                c.drawArc(caixa, 135f, 270f * value / 100f, false, pArco)
            }
            val rk = w * 0.30f
            pBotao.shader = LinearGradient(0f, cy - rk, 0f, cy + rk, cBotaoTopo, cBotaoBase, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, rk, pBotao)
            pContorno.color = cBotaoBorda; pContorno.strokeWidth = maxOf(1f, d)
            c.drawCircle(cx, cy, rk, pContorno)
            // número
            pNum.color = if (ativo) cTexto else cSuave
            pNum.textSize = w * (if (big) 0.17f else 0.18f)
            pPct.color = pNum.color; pPct.textSize = pNum.textSize * 0.45f
            val num = if (vazio) "—" else value.toString()
            val wn = pNum.measureText(num); val wp = pPct.measureText("%")
            val x0 = cx - (wn + wp) / 2f
            pNum.textAlign = Paint.Align.LEFT
            c.drawText(num, x0, cy + pNum.textSize * 0.35f, pNum)
            if (!vazio) c.drawText("%", x0 + wn + d, cy + pNum.textSize * 0.35f, pPct)
            if (vazio || !marcador) return
            // marcador
            val ang = Math.toRadians(135.0 + 270.0 * value / 100.0)
            pMarca.color = if (ativo) cMarca else cSuave; pMarca.strokeWidth = w * 0.025f
            c.drawLine(cx + (rk * 0.72f * cos(ang)).toFloat(), cy + (rk * 0.72f * sin(ang)).toFloat(),
                cx + (rk * 0.9f * cos(ang)).toFloat(), cy + (rk * 0.9f * sin(ang)).toFloat(), pMarca)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    var ang = Math.toDegrees(atan2((e.y - height / 2f).toDouble(), (e.x - width / 2f).toDouble()))
                    if (ang < 135.0) ang += 360.0
                    if (ang > 405.0) ang = if (ang > 427.5) 135.0 else 405.0     // vão de baixo: encosta na ponta
                    val novo = (((ang - 135.0) / 270.0) * 100.0).toInt().coerceIn(0, 100)
                    if (kotlin.math.abs(novo - value) > 60 && e.actionMasked == MotionEvent.ACTION_MOVE) return true   // não pula 0↔100
                    value = novo; vazio = false; invalidate()
                    val agora = System.currentTimeMillis()
                    if (agora - ultimoEnvio > 70) { ultimoEnvio = agora; onChange?.invoke(value) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onChange?.invoke(value)
                    performClick()
                }
            }
            return true
        }
        override fun performClick(): Boolean { super.performClick(); return true }
    }

    /** Potenciômetro deslizante (vertical), igual ao do DECKRA APP: escala, trilho, preenchimento e puxador. */
    private inner class FaderView(ctx: Context) : View(ctx) {
        var value: Int? = null; private set
        private var ativo = true
        private var mudo = false
        var dragging = false; private set
        var onChange: ((Int) -> Unit)? = null
        private var ultimoEnvio = 0L
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun yTopo() = height * 0.065f + d
        private fun yBase() = height - height * 0.065f - d

        fun setValue(v: Int?, ativo: Boolean, mudo: Boolean) {
            val novo = v?.coerceIn(0, 100)
            if (novo != value || ativo != this.ativo || mudo != this.mudo) { value = novo; this.ativo = ativo; this.mudo = mudo; invalidate() }
        }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val cx = w / 2f
            val y0 = yTopo(); val y1 = yBase()
            val esp = w * 0.13f
            p.shader = null; p.color = cTrilho; p.strokeWidth = maxOf(1f, d)
            for (k in 0..10) {                                         // escala
                val y = y1 - (y1 - y0) * k / 10f
                val comp = w * (if (k % 5 == 0) 0.13f else 0.08f)
                for (lado in listOf(-1f, 1f)) {
                    val x = cx + lado * (esp / 2 + w * 0.07f)
                    c.drawLine(x, y, x + lado * comp, y, p)
                }
            }
            p.color = cDisco
            c.drawRoundRect(cx - esp / 2, y0 - esp / 2, cx + esp / 2, y1 + esp / 2, esp / 2, esp / 2, p)
            val frac = (value ?: 0) / 100f
            val yv = y1 - (y1 - y0) * frac
            if (frac > 0f) {
                val a0 = when { mudo -> Color.rgb(110, 60, 78); ativo -> cAzul; else -> misturar(cTrilho, cFraco, 0.55f) }
                val a1 = when { mudo -> Color.rgb(110, 60, 78); ativo -> cRoxoArco; else -> misturar(cTrilho, cSuave, 0.55f) }
                p.shader = LinearGradient(0f, y1, 0f, y0, a0, a1, Shader.TileMode.CLAMP)
                val m = esp * 0.32f
                c.drawRoundRect(cx - m, yv, cx + m, y1 + m, m, m, p)
                p.shader = null
            }
            val lc = w * 0.66f; val ac = height * 0.13f
            val caixa = RectF(cx - lc / 2, yv - ac / 2, cx + lc / 2, yv + ac / 2)
            p.shader = LinearGradient(0f, caixa.top, 0f, caixa.bottom, cBotaoTopo, cBotaoBase, Shader.TileMode.CLAMP)
            c.drawRoundRect(caixa, 3 * d, 3 * d, p)
            p.shader = null; p.style = Paint.Style.STROKE; p.color = cBotaoBorda
            c.drawRoundRect(caixa, 3 * d, 3 * d, p)
            p.style = Paint.Style.FILL; p.strokeWidth = 1.6f * d
            p.color = if (ativo && !mudo) misturar(cAzul, cRoxoArco, frac) else cSuave
            c.drawLine(cx - lc * 0.32f, yv, cx + lc * 0.32f, yv, p)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val frac = (yBase() - e.y) / (yBase() - yTopo())
                    value = (frac * 100).toInt().coerceIn(0, 100); invalidate()
                    val agora = System.currentTimeMillis()
                    if (agora - ultimoEnvio > 70) { ultimoEnvio = agora; onChange?.invoke(value ?: 0) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onChange?.invoke(value ?: 0)
                    performClick()
                }
            }
            return true
        }
        override fun performClick(): Boolean { super.performClick(); return true }
    }

    /** Barrinha de tempo do cartão Mídia: arrastar ou tocar leva o vídeo/música para aquele ponto. */
    private inner class ProgressoView(ctx: Context) : View(ctx) {
        private var pos = 0.0; private var dur = 0.0; private var pode = false
        var arrastando = false; private set
        var aoArrastar: ((Double) -> Unit)? = null
        var aoSoltar: ((Double) -> Unit)? = null
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        private fun margem() = 7 * d

        fun definir(pos: Double, dur: Double, pode: Boolean) {
            this.dur = dur; this.pode = pode
            if (!arrastando) this.pos = pos
            invalidate()
        }

        override fun onDraw(c: Canvas) {
            val y = height / 2f; val x0 = margem(); val x1 = width - margem()
            val frac = if (dur > 0) (pos / dur).toFloat().coerceIn(0f, 1f) else 0f
            val xv = x0 + (x1 - x0) * frac
            p.strokeWidth = 4 * d; p.shader = null; p.color = cTrilho
            c.drawLine(x0, y, x1, y, p)
            p.shader = LinearGradient(x0, 0f, x1, 0f, cAzul, cRoxoArco, Shader.TileMode.CLAMP)
            if (frac > 0f) c.drawLine(x0, y, xv, y, p)
            p.shader = null; p.color = if (pode) cRoxoArco else cSuave
            c.drawCircle(xv, y, (if (arrastando) 8 else 6) * d, p)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (!pode || dur <= 0) return false
            val frac = ((e.x - margem()) / (width - 2 * margem())).coerceIn(0f, 1f)
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    arrastando = true; parent?.requestDisallowInterceptTouchEvent(true)
                    pos = dur * frac; aoArrastar?.invoke(pos); invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    arrastando = false; parent?.requestDisallowInterceptTouchEvent(false)
                    pos = dur * frac; aoSoltar?.invoke(pos); invalidate(); performClick()
                }
                MotionEvent.ACTION_CANCEL -> { arrastando = false; invalidate() }
            }
            return true
        }
        override fun performClick(): Boolean { super.performClick(); return true }
    }

    private fun misturar(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt())

    private fun versionName(): String = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (_: Exception) { "" }

    // =====================================================================
    //  Conexão com o PC (mesmo protocolo da DECKRA Bridge)
    // =====================================================================
    private fun discoverPc(silencioso: Boolean = false) {
        setStatus("Procurando o PC…", cAlerta)
        thread {
            var found: InetAddress? = null
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("deckra-discovery").apply { setReferenceCounted(false); acquire() }
            try {
                DatagramSocket(null).use { ds ->
                    ds.reuseAddress = true; ds.broadcast = true; ds.soTimeout = 650
                    val wifiAddress = NetworkInterface.getNetworkInterfaces().toList()
                        .filter { it.isUp && !it.isLoopback && (it.name.startsWith("wlan", true) || it.name.startsWith("wifi", true)) }
                        .flatMap { it.interfaceAddresses }
                        .firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
                        ?: throw IllegalStateException("Wi-Fi IPv4 não disponível.")
                    ds.bind(InetSocketAddress(wifiAddress.address, 0))
                    val targets = linkedSetOf<InetAddress>()
                    wifiAddress.broadcast?.let { targets.add(it) }
                    targets.add(InetAddress.getByName("255.255.255.255"))
                    val data = "DECKRA_DISCOVER_V2".toByteArray()
                    for (attempt in 1..5) {
                        if (found != null) break
                        for (t in targets) try { ds.send(DatagramPacket(data, data.size, t, 37891)) } catch (_: Exception) {}
                        val limite = System.currentTimeMillis() + 900
                        while (System.currentTimeMillis() < limite && found == null) {
                            try {
                                val buf = ByteArray(1024); val pk = DatagramPacket(buf, buf.size); ds.receive(pk)
                                val payload = runCatching { JSONObject(String(pk.data, 0, pk.length, Charsets.UTF_8)) }.getOrNull()
                                if (payload?.optString("type") == "deckra_bridge" && payload.optInt("port", 0) == 37892) found = pk.address
                            } catch (_: SocketTimeoutException) { break }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally { try { lock.release() } catch (_: Exception) {} }
            runOnUiThread {
                val achado = found
                if (achado != null) {
                    host = achado.hostAddress ?: ""
                    hostInput?.setText(host)
                    setStatus("PC encontrado", cOk)
                    if (chavePc.isNotBlank()) connectPc(automatico = true)          // já pareado: sem PIN
                    else if (pinInput?.text?.toString()?.trim()?.matches(Regex("\\d{6}")) == true) connectPc()
                    else { pinInput?.requestFocus(); toast("PC encontrado. Digite o PIN que aparece no DECKRA.") }
                } else {
                    setStatus("PC não encontrado", cAlerta)
                    if (!silencioso) toast("No PC, toque em \"Procurar celular\" no DECKRA (Configurações) e tente de novo.")
                }
            }
        }
    }

    /** Celular já pareado: conecta sozinho ao abrir (sem PIN). */
    private fun voltarSozinho(atraso: Long) {
        if (chavePc.isBlank()) return
        handler.postDelayed({
            if (paired || tentando) return@postDelayed
            jaProcurou = false
            if (host.isNotBlank()) connectPc(automatico = true) else { jaProcurou = true; discoverPc(silencioso = true) }
        }, atraso)
    }

    private fun connectPc(automatico: Boolean = false) {
        if (!automatico) hostInput?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { host = it }
        if (host.isBlank() || runCatching { InetAddress.getByName(host) }.isFailure) { if (!automatico) toast("Informe o IP do PC ou toque em Procurar."); return }
        val code = pinInput?.text?.toString()?.trim().orEmpty()
        val usarChave = chavePc.isNotBlank() && (automatico || code.isBlank())
        if (!usarChave && !code.matches(Regex("\\d{6}"))) { toast("O PIN tem 6 dígitos."); return }
        this.automatico = automatico; tentando = true
        setStatus(if (usarChave) "Reconectando…" else "Conectando…", cAlerta)
        socket?.cancel()
        val pedido = JSONObject().put("type", "pair").put("device", deviceId).put("nome", Build.MODEL ?: "Celular")
        if (usarChave) pedido.put("token", chavePc) else pedido.put("pin", code)
        socket = client.newWebSocket(Request.Builder().url("ws://$host:37892/").build(), object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) { ws.send(pedido.toString()) }
            override fun onMessage(ws: WebSocket, text: String) {
                val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (msg.optString("type")) {
                    "pair_result" -> runOnUiThread {
                        paired = msg.optBoolean("paired"); tentando = false
                        if (paired) {
                            prefs.edit().putString("last_host", host).apply()
                            msg.optString("token").takeIf { it.isNotBlank() }?.let {       // PIN certo: o PC deu a chave
                                chavePc = it; prefs.edit().putString("pc_token", it).apply()
                            }
                            setStatus("PC conectado", cOk)
                            antigo = false; deckra = null; icones.clear()
                            if (page == "ajustes") page = "inicio"
                            renderNav(); renderPage()
                            send("get_deckra", silencioso = true)
                        } else if (msg.optBoolean("esquecido")) {
                            // o PC não conhece mais este celular: volta a pedir o PIN
                            chavePc = ""; prefs.edit().remove("pc_token").apply()
                            setStatus("Digite o PIN", cAlerta); renderPage()
                            toast("O PC não reconheceu este celular. No DECKRA do PC, toque em \"Procurar celular\" e digite o PIN.")
                        } else { setStatus("PIN incorreto", cMudo); toast(msg.optString("message").ifBlank { "PIN incorreto." } + " Confira o PIN no DECKRA do PC.") }
                    }
                    "state" -> {}
                    "result" -> {
                        val data = msg.optJSONObject("data")
                        val comando = msg.optString("command")
                        runOnUiThread {
                            if (!msg.optBoolean("ok", true)) {
                                if (comando == "get_deckra") { if (!antigo) { antigo = true; renderPage() } }
                                else if (comando != "get_icone") toast(msg.optString("message", "O PC recusou o comando."))
                                return@runOnUiThread
                            }
                            when {
                                data == null -> {}
                                comando == "get_opcoes" -> mostrarOpcoes(data.optJSONArray("opcoes") ?: JSONArray())
                                comando == "get_icone" -> {
                                    val bytes = runCatching { Base64.decode(data.optString("png"), Base64.DEFAULT) }.getOrNull()
                                    icones[data.optString("alvo")] = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                                    deckra?.let { st -> atualizadores.forEach { it(st) } }
                                }
                                data.optBoolean("deckra") -> { val era = deckra == null; aoReceberEstado(data); if (era) renderPage() }
                            }
                        }
                    }
                    "error" -> runOnUiThread { toast(msg.optString("message")) }
                }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    if (socket !== ws) return@runOnUiThread
                    val estava = paired
                    paired = false; tentando = false; refreshPolling()
                    setStatus(if (estava) "PC desconectado" else "Falha na conexão", cAlerta)
                    when {
                        estava -> voltarSozinho(5000)                          // caiu: tenta voltar uma vez
                        automatico && !jaProcurou -> { jaProcurou = true; discoverPc(silencioso = true) }   // o IP pode ter mudado
                        automatico -> setStatus("PC não encontrado", cAlerta)
                        else -> toast("Não conectou. No PC, abra a procura do DECKRA Mobile e tente de novo.")
                    }
                    renderPage()
                }
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    if (socket !== ws) return@runOnUiThread
                    val estava = paired
                    paired = false; tentando = false; refreshPolling(); setStatus("PC desconectado", cAlerta); renderPage()
                    if (estava) voltarSozinho(5000)
                }
            }
        })
    }

    /** Desconectar: o PC esquece este celular, e da próxima vez precisa do PIN. */
    private fun disconnect() {
        val ws = socket; socket = null
        if (paired) ws?.send(JSONObject().put("type", "forget").toString())
        ws?.close(1000, "desconectado pelo usuário")
        chavePc = ""; prefs.edit().remove("pc_token").apply()
        paired = false; tentando = false; refreshPolling(); setStatus("Não conectado", cAlerta); renderPage()
    }

    private fun refreshPolling() {
        handler.removeCallbacks(poll)
        if (paired) handler.postDelayed(poll, 450)
    }

    private fun send(command: String, extra: JSONObject = JSONObject(), silencioso: Boolean = false) {
        if (!paired) { if (!silencioso) toast("Conecte ao PC primeiro."); return }
        extra.put("type", "command").put("command", command)
        if (socket?.send(extra.toString()) != true && !silencioso) toast("Falha ao enviar o comando.")
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    // =====================================================================
    //  Atualização do próprio aplicativo: mesmo esquema do DECKRA APP do PC. Um versao.json no
    //  GitHub diz a versão nova, o link do APK, o sha256, o tamanho e as notas; o app confere ao
    //  abrir, avisa, baixa, confere o arquivo e abre o instalador do Android.
    // =====================================================================
    private val urlManifesto = "https://raw.githubusercontent.com/VelaSmart/Morfy3D/Deckra/mobile/versao.json"
    private var novaVersao: JSONObject? = null           // manifesto de uma versão mais nova, se houver
    private var estadoAtualizacao = "Ainda não verificado"
    private var ultimaVerificacao = 0L

    private fun installedVersionCode(): Long =
        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))

    private fun setUpdate(texto: String, acao: (() -> Unit)? = null) {
        estadoAtualizacao = texto
        updateButton?.let { b ->
            b.text = texto
            b.setOnClickListener { (acao ?: { checkForUpdates(manual = true) })() }
        }
    }

    /** Confere o versao.json. manual = false: só avisa se houver versão nova (como ao abrir o app). */
    private fun checkForUpdates(manual: Boolean) {
        ultimaVerificacao = System.currentTimeMillis()
        if (manual) setUpdate("Verificando…")
        val pedido = Request.Builder().url("$urlManifesto?t=${System.currentTimeMillis()}")
            .header("Cache-Control", "no-cache").header("User-Agent", "DECKRA-Mobile/${versionName()}").build()
        client.newCall(pedido).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread { if (manual) setUpdate("Sem internet · tentar de novo") }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val texto = it.body?.string().orEmpty()
                    val json = if (it.isSuccessful) runCatching { JSONObject(texto) }.getOrNull() else null
                    runOnUiThread {
                        if (json == null) {
                            if (manual) setUpdate(if (it.code == 404) "Nenhuma versão publicada ainda" else "Não foi possível verificar · tentar de novo")
                            return@runOnUiThread
                        }
                        val codigo = json.optLong("versionCode", 0L)
                        val url = json.optString("url")
                        val valido = url.startsWith("https://raw.githubusercontent.com/VelaSmart/Morfy3D/") &&
                            json.optString("sha256").matches(Regex("[0-9a-fA-F]{64}")) && json.optLong("tamanho", 0L) > 0
                        if (codigo > installedVersionCode() && valido) {
                            val primeiraVez = novaVersao == null
                            novaVersao = json
                            setUpdate("Baixar e instalar a versão ${json.optString("versao")}") { confirmarAtualizacao() }
                            if (primeiraVez && page == "inicio") renderPage()        // mostra o aviso no Início
                        } else {
                            novaVersao = null
                            setUpdate("Você está com a versão mais recente")
                        }
                    }
                }
            }
        })
    }

    private fun confirmarAtualizacao() {
        val json = novaVersao ?: return
        val notas = json.optString("notas").ifBlank { "Melhorias e correções." }
        android.app.AlertDialog.Builder(this)
            .setTitle("DECKRA Mobile ${json.optString("versao")}")
            .setMessage("$notas\n\nO Android vai abrir o instalador. Seus dados são mantidos.")
            .setNegativeButton("Agora não", null)
            .setPositiveButton("Baixar e instalar") { _, _ -> downloadAndInstall(json) }
            .show()
    }

    /** Aviso de versão nova no Início, igual ao "Nova versão disponível" do DECKRA APP. */
    private fun avisoAtualizacao(): View? {
        val json = novaVersao ?: return null
        val c = card("↻", "Nova versão ${json.optString("versao")} disponível")
        c.second.addView(text(json.optString("notas").ifBlank { "Melhorias e correções." }, 12f, cSuave).apply { maxLines = 4; ellipsize = TextUtils.TruncateAt.END })
        c.second.addView(pill("Baixar e instalar", true) { confirmarAtualizacao() },
            LinearLayout.LayoutParams(dp(180), dp(36)).apply { topMargin = dp(10) })
        return c.first
    }

    private fun downloadAndInstall(json: JSONObject) {
        val url = json.optString("url")
        val esperadoSha = json.optString("sha256").lowercase()
        val esperadoTam = json.optLong("tamanho", 0L)
        setUpdate("Baixando… 0%")
        client.newCall(Request.Builder().url("$url?t=${System.currentTimeMillis()}").header("Cache-Control", "no-cache").build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    runOnUiThread { setUpdate("Falha no download · tentar de novo") { downloadAndInstall(json) } }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body
                        if (!it.isSuccessful || body == null) {
                            runOnUiThread { setUpdate("Download falhou · tentar de novo") { downloadAndInstall(json) } }; return
                        }
                        val apk = File(cacheDir, "deckra-mobile-update.apk")
                        apk.delete()
                        val resumo = java.security.MessageDigest.getInstance("SHA-256")
                        var lido = 0L; var ultimo = -1
                        body.byteStream().use { input ->
                            apk.outputStream().use { output ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    val n = input.read(buf); if (n < 0) break
                                    output.write(buf, 0, n); resumo.update(buf, 0, n); lido += n
                                    val pct = (lido * 100 / maxOf(1L, esperadoTam)).toInt().coerceAtMost(100)
                                    if (pct != ultimo) { ultimo = pct; runOnUiThread { setUpdate("Baixando… $pct%") } }
                                }
                            }
                        }
                        val sha = resumo.digest().joinToString("") { b -> "%02x".format(b) }
                        if (lido != esperadoTam || sha != esperadoSha) {
                            apk.delete()
                            runOnUiThread { setUpdate("Arquivo não confere · tentar de novo") { downloadAndInstall(json) } }
                            return
                        }
                        runOnUiThread {
                            if (!packageManager.canRequestPackageInstalls()) {
                                awaitingInstallPermission = true
                                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                setUpdate("Autorize a instalação e volte ao app") { downloadAndInstall(json) }
                                return@runOnUiThread
                            }
                            installDownloadedUpdate(apk)
                        }
                    }
                }
            })
    }

    private fun installDownloadedUpdate(apk: File) {
        try {
            if (!apk.exists() || apk.length() == 0L) throw IllegalStateException("O arquivo baixado não está disponível.")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apk)
            startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).apply { data = uri; flags = Intent.FLAG_GRANT_READ_URI_PERMISSION })
            setUpdate("Instalador do Android aberto")
        } catch (e: Exception) {
            setUpdate("Não foi possível abrir o instalador")
            toast(e.message ?: "Falha ao iniciar a atualização.")
        }
    }

    override fun onResume() {
        super.onResume()
        if (ultimaVerificacao > 0 && System.currentTimeMillis() - ultimaVerificacao > 30 * 60 * 1000L) checkForUpdates(manual = false)
        if (!paired && !tentando) voltarSozinho(300)
        if (awaitingInstallPermission && packageManager.canRequestPackageInstalls()) {
            awaitingInstallPermission = false
            installDownloadedUpdate(File(cacheDir, "deckra-mobile-update.apk"))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        socket?.close(1000, "app fechado")
        client.dispatcher.executorService.shutdown()
        super.onDestroy()
    }
}
