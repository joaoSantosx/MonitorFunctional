package com.jvf.monitorfunctional.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.jvf.monitorfunctional.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
@SuppressLint("AccessibilityPolicy")
class YouTubeMonitorService : AccessibilityService() {

    private val youtubeApiKey = BuildConfig.youtube_api_key
    private val TAG = "Aware_Kids"
    private var ultimoTituloCapturado: String? = null

    private var ultimaPesquisaCapturada: String? = null

    private val cacheVideosAnalisados = mutableMapOf<String, Long>()
    private val cachePesquisas = mutableMapOf<String, Long>()

    private var isScreenOn = true
    private var screenReceiver: android.content.BroadcastReceiver? = null


    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "=== Monitor Iniciado: Detectando vídeo em reprodução ===")

        val filter = android.content.IntentFilter().apply() {
            addAction(android.content.Intent.ACTION_SCREEN_ON)
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
        }

        screenReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(
                context: android.content.Context?,
                intent: android.content.Intent?
            ) {
                when (intent?.action) {
                    android.content.Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        Log.d(TAG, "Tela apagada: Monitoramento pausado para poupar bateria")
                        ultimoTituloCapturado = null
                        ultimaPesquisaCapturada = null
                    }
                    android.content.Intent.ACTION_SCREEN_ON ->{
                        isScreenOn = true
                        Log.d(TAG, "Tela Ligada: Reativando monitor")
                }
            }
        }
    }

    registerReceiver(screenReceiver, filter)
}
    override fun onDestroy(){
        super.onDestroy()
        try {
            if(screenReceiver != null){
                unregisterReceiver(screenReceiver)
                Log.i(TAG, "=== Monitor Encerrado: Receptor de bateria removido com sucesso ===")
            }
        } catch (e: Exception){
            Log.e(TAG, "Erro ao remover receptor de tela:", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isScreenOn) return
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val rootNode = rootInActiveWindow ?: return


        //Defesa contra crianças desativando o acessibility service
        val pacoteAtual = event.packageName?.toString() ?: ""
        if (pacoteAtual.contains("settings") || pacoteAtual.contains("config") || pacoteAtual.contains(
                "accessibility"
            )
        ) {

            Log.e("Aware Kids_DEFESA", "!O usuário abriu as configurações! Pacote: $pacoteAtual")

            val prefs = applicationContext.getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
            val protecaoAtiva = prefs.getBoolean("protecao_ativa", true)

            Log.e("Aware Kids_DEFESA", "A proteção está ativa no banco local? $protecaoAtiva")

            if (protecaoAtiva) {
                val estaNaTelaDeDesativar = procurarTextoNaTela(rootNode, "Monitor Aware Kids")

                Log.e(
                    "Aware Kids_DEFESA",
                    "O nome do serviço apareceu na tela? $estaNaTelaDeDesativar"
                )

                if (estaNaTelaDeDesativar) {
                    Log.e("Aware Kids_DEFESA", " ENCERRANDO tela de configurações!")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    android.widget.Toast.makeText(
                        applicationContext,
                        "Área restrita aos responsáveis.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }
        }
        // Garante que está no YouTube
        if (event.packageName?.toString()?.contains("youtube") != true) return


        if (estaNaTelaDeResultados(rootNode)) {
            val textoPesquisado = capturarPesquisaConfirmada(rootNode)

            if (textoPesquisado != null && textoPesquisado != ultimaPesquisaCapturada) {
                ultimaPesquisaCapturada = textoPesquisado

                val agora = System.currentTimeMillis()
                val tempoUltimaPesquisa = cachePesquisas[textoPesquisado] ?: 0L
                val mSPassados = (agora - tempoUltimaPesquisa)

                if (mSPassados < 300000) {
                    Log.d(TAG, "Pesquisa ignorada (já registrada recentemente): $textoPesquisado)")
                } else {
                    ultimaPesquisaCapturada = textoPesquisado
                    cachePesquisas[textoPesquisado] = agora
                    Log.w(TAG, "Pesquisa do usuário localizada: $textoPesquisado")
                    salvarPesquisaNoFirebase(textoPesquisado)

                    if (cachePesquisas.size > 100) {
                        cachePesquisas.clear()
                        Log.i(TAG, "Limpeza de cache de pesquisas realizada com sucesso")
                    }
                }
            }
        }
        //Garante que o usuário está na tela de reprodução
        if (!estaNaTelaDeReproducao(rootNode)) return

        val titulo = capturarTituloAtual(rootNode) ?: return

        // Evita log duplicado
        if (titulo == ultimoTituloCapturado) return
        ultimoTituloCapturado = titulo

        Log.e(TAG, "Assistindo: $titulo")

        val tempoInicioLatencia = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            val comentariosConcatenados = buscarIDVideoSilenciosamente(titulo)
            processarTituloComIA(titulo, comentariosConcatenados, tempoInicioLatencia)

        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Serviço Interrompido")
    }

    // INTEGRAÇÃO IA E FIREBASE
    private fun processarTituloComIA(titulo: String, videoId: String, inicioLatencia: Long) {

        val agora = System.currentTimeMillis()
        val ultimaVez = cacheVideosAnalisados[titulo] ?: 0L
        if ((agora - ultimaVez) < (5 * 60 * 1000)) {
            return
        }
        cacheVideosAnalisados[titulo] = agora

        if (cacheVideosAnalisados.size > 100) {
            cacheVideosAnalisados.clear()
            Log.i(TAG, "Limpeza de cache de videos realizada (Prevenção de Memória)")
        }

        Log.e(TAG, "Novo título capturado: $titulo - Enviando para análise...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = applicationContext.getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
                val codigoFilho = prefs.getString("codigo_filho", "SEM_CODIGO") ?: "SEM_CODIGO"
                val bancoDeDados = Firebase.firestore

                var nivelRigidezAtual = "ALTA" // Nível padrão
                var palavrasMonitoradas = listOf<String>()
                var palavrasPermitidas = listOf<String>()
                var planoAtual = "FREE"
                try {
                    if (codigoFilho != "SEM_CODIGO") {
                        val docRegra =
                            bancoDeDados.collection("regras_parentais").document(codigoFilho).get()
                                .await()
                        if (docRegra.exists()) {
                            planoAtual = docRegra.getString("plano") ?: "FREE"
                            nivelRigidezAtual = docRegra.getString("nivel") ?: "ALTA"

                            val palavrasSalvas = docRegra.get("palavras_monitoradas") as? List<*>
                            if (palavrasSalvas != null) {
                                palavrasMonitoradas = palavrasSalvas.map { it.toString() }
                            }

                            val palavrasPermitidasSalvas =
                                docRegra.get("palavras_permitidas") as? List<*>
                            if (palavrasPermitidasSalvas != null) {
                                palavrasPermitidas = palavrasPermitidasSalvas.map { it.toString() }
                            }

                            if (planoAtual != "PREMIUM"){
                                val formatoData = java.text.SimpleDateFormat("yyy-MM-dd", java.util.Locale.getDefault())
                                val hojeStr = formatoData.format(java.util.Date())

                                val dataUltimo = docRegra.getString("data_ultimo_video") ?: ""
                                var contagemHoje = docRegra.getLong("contagem_videos_hoje") ?: 0L

                              //mudou de dia zera o contador
                              if (dataUltimo != hojeStr){
                                  contagemHoje = 0
                              }

                                if (contagemHoje >= 40) {
                                    Log.w(TAG, "Limite diário Free atingido! Abortando análise.")

                                    val ultimoAviso = prefs.getString("data_aviso_limite", "")
                                    if (ultimoAviso != hojeStr) {
                                        val logLimite = hashMapOf(
                                            "titulo" to "Limite diário de proteção atingido!",
                                            "data_hora" to Date(),
                                            "timestamp" to System.currentTimeMillis(),
                                            "seguro" to false,
                                            "motivo_ia" to "O plano FREE permite analisar apenas 40 vídeos por dia e o limite foi excedido. O monitoramento está pausado, faça o upgrade para o plano Premium para ter proteção 24 horas.",
                                            "dispositivo" to "Sistema Aware Kids",
                                            "codigo_pareamento" to codigoFilho,
                                            "nivel_rigidez_usado" to "BLOQUEIO_FREE"
                                        )
                                        bancoDeDados.collection("historico_parentais").add(logLimite)
                                        prefs.edit().putString("data_aviso_limite", hojeStr).apply()
                                    }
                                    return@launch
                                }
                            }
                        }
                    }


                } catch (e: Exception) {
                    Log.w(TAG, "⚠ Erro ao buscar regra, usando padrões.", e)
                }

                val analisador = AnaliseIA()
                val resultadoIA = analisador.verificarSeguranca(
                    titulo,
                    videoId,
                    nivelRigidezAtual,
                    palavrasMonitoradas,
                    palavrasPermitidas
                )
                Log.i(
                    TAG,
                    "IA: Seguro=${resultadoIA.ehSeguro} | Nível: $nivelRigidezAtual | Motivo: ${resultadoIA.detalhes}"
                )

                val tempoFimLatencia = System.currentTimeMillis()
                val tempoTotalLatencia = tempoFimLatencia - inicioLatencia
                val logVideo = hashMapOf(
                    "titulo" to titulo,
                    "data_hora" to Date(),
                    "timestamp" to agora,
                    "seguro" to resultadoIA.ehSeguro,
                    "motivo_ia" to resultadoIA.detalhes,
                    "dispositivo" to "Celular do Filho",
                    "codigo_pareamento" to codigoFilho,
                    "nivel_rigidez_usado" to nivelRigidezAtual,
                    "tempo-total-latencia" to tempoTotalLatencia

                )

                bancoDeDados.collection("historico_parental")
                    .add(logVideo)
                    .addOnSuccessListener { doc ->
                        Log.d(TAG, "Sucesso! Salvo na nuvem ID: ${doc.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao salvar na nuvem!", e)
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Erro crítico no fluxo IA/Firebase: ${e.message}")
                cacheVideosAnalisados.remove(titulo)
            }
        }
    }


    //Busca apenas o ID para delegar a carga de rede ao Node.js
    private suspend fun buscarIDVideoSilenciosamente(titulo: String): String {
        return withContext(Dispatchers.IO) {
            var videoIdEncontrado = ""
            try {
                Log.i(TAG, "Buscando id na API para o título...")

                val tituloFormatado = URLEncoder.encode(titulo, "UTF-8")
                val urlBusca =
                    URL("https://www.googleapis.com/youtube/v3/search?part=id&q=$tituloFormatado&type=video&key=$youtubeApiKey&maxResults=1")

                val conexaoBusca = urlBusca.openConnection() as HttpURLConnection
                conexaoBusca.requestMethod = "GET"

                if (conexaoBusca.responseCode == 200) {
                    val respostaBusca = conexaoBusca.inputStream.bufferedReader().readText()
                    val jsonBusca = JSONObject(respostaBusca)
                    val items = jsonBusca.getJSONArray("items")

                    if (items.length() > 0) {
                        videoIdEncontrado =
                            items.getJSONObject(0).getJSONObject("id").getString("videoId")
                        Log.i(
                            TAG,
                            " Vídeo encontrado na API, id delegando comentários ao servidor: $videoIdEncontrado"
                        )
                    } else {
                        Log.w(TAG, " Nenhum vídeo correspondente encontrado na API.")
                    }
                } else {
                    Log.e(TAG, "Erro na API de busca: Código ${conexaoBusca.responseCode}")
                }
                conexaoBusca.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Erro crítico ao consultar API do YouTube", e)
            }

            return@withContext videoIdEncontrado
        }
    }

    //Detecção de tela de vídeo
    private fun estaNaTelaDeReproducao(root: AccessibilityNodeInfo): Boolean {
        val watchPanel =
            root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_panel")
        val watchList =
            root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_list")
        return watchPanel.isNotEmpty() || watchList.isNotEmpty()
    }

    //Captura do título
    private fun capturarTituloAtual(root: AccessibilityNodeInfo): String? {
        val listaTitulos =
            root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/title")
        for (node in listaTitulos) {
            val classe = node.className?.toString() ?: ""
            val texto = node.text?.toString()
            if (classe == "android.widget.TextView" && !texto.isNullOrBlank()) {
                return texto.trim()
            }
        }
        return null
    }

    // Defesa para varrer texto
    private fun procurarTextoNaTela(node: AccessibilityNodeInfo, textoAlvo: String): Boolean {
        val textoDoNo = node.text?.toString() ?: ""
        val descricaoDoNo = node.contentDescription?.toString() ?: ""

        if (textoDoNo.contains(textoAlvo, ignoreCase = true) || descricaoDoNo.contains(
                textoAlvo,
                ignoreCase = true
            )
        ) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val encontrou = procurarTextoNaTela(child, textoAlvo)
                if (encontrou) return true
            }
        }
        return false
    }

    private fun estaNaTelaDeResultados(root: AccessibilityNodeInfo): Boolean {
        val searchQuery =
            root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/search_query")
        return searchQuery.isNotEmpty()
    }

    private fun capturarPesquisaConfirmada(root: AccessibilityNodeInfo): String? {
        val listaPesquisa =
            root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/search_query")

        for (node in listaPesquisa) {
            val texto = node.text?.toString()
            if (!texto.isNullOrBlank()) {
                return texto.trim()
            }
        }
        return null
    }

    private fun salvarPesquisaNoFirebase(termoPesquisado: String) {
        val prefs = applicationContext.getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        val codigoFilho = prefs.getString("codigo_filho", "SEM_CODIGO") ?: "SEM_CODIGO"

        if (codigoFilho == "SEM_CODIGO") return

        val bancoDeDados = Firebase.firestore
        val logPesquisa = hashMapOf(
            "termo" to termoPesquisado,
            "data_hora" to java.util.Date(),
            "timestamp" to System.currentTimeMillis(),
            "codigo_pareamento" to codigoFilho
        )
        bancoDeDados.collection("historico_pesquisas")
            .add(logPesquisa)
            .addOnSuccessListener {
                Log.d(TAG, "Pesquisa salva na nuvem com sucesso: $termoPesquisado")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar pesquisa na nuvem", e)
            }
    }
}