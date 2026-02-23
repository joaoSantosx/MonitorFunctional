package com.jvf.monitorfunctional.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.withContext
import android.graphics.Rect


class YouTubeMonitorService : AccessibilityService() {

    private val YOUTUBE_API_KEY = "chaveapi2"
    private val TAG = "JVF_Monitor"
    private var ultimoTituloCapturado: String? = null
    private val cacheVideosAnalisados = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "=== Monitor Iniciado: Detectando vídeo em reprodução ===")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val rootNode = rootInActiveWindow ?: return

        // 🔒 Garante que estamos no YouTube
        if (event.packageName?.toString()?.contains("youtube") != true) return

        // 🎬 Garante que estamos na tela de reprodução (ignora Home/Feed/Miniplayer)
        if (!estaNaTelaDeReproducao(rootNode)) return

        val titulo = capturarTituloAtual(rootNode) ?: return

        // Evita log duplicado
        if (titulo == ultimoTituloCapturado) return
        ultimoTituloCapturado = titulo

        Log.e(TAG, "🎬 Assistindo: $titulo")

        // 👉 INICIA A BUSCA SILENCIOSA NA API (Rodando fora da Thread Principal)
        CoroutineScope(Dispatchers.IO).launch {
            val comentariosConcatenados = analisarVideoSilenciosamente(titulo)

            processarTituloComIA(titulo, comentariosConcatenados)
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Serviço Interrompido")
    }

    // ==============================
    // 🤖 FLUXO 2: INTEGRAÇÃO IA + FIREBASE
    // ==============================

    private fun processarTituloComIA(titulo:String, comentarios: String){

        val agora = System.currentTimeMillis()
        val ultimaVez = cacheVideosAnalisados[titulo] ?: 0L
        if ((agora - ultimaVez) < (5 * 60 * 1000)){
            return
        }
        cacheVideosAnalisados[titulo] = agora

        Log.e(TAG, "✅ TÍTULO NOVO CAPTURADO: $titulo - ENVIANDO PARA IA...")

        CoroutineScope(Dispatchers.IO).launch{
            try{
                // 1. LÊ O CÓDIGO GERADO NESTE CELULAR
                val prefs = applicationContext.getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
                val codigoFilho = prefs.getString("codigo_filho", "SEM_CODIGO")

                // 2. PERGUNTA PARA A IA
                val analisador = AnaliseIA()
                val resultadoIA = analisador.verificarSeguranca(titulo, comentarios)
                Log.i(TAG, "🤖 IA Respondeu: Seguro=${resultadoIA.ehSeguro} | Motivo: ${resultadoIA.detalhes}")

                // 3. SALVA NO FIREBASE
                val bancoDeDados = Firebase.firestore

                val logVideo = hashMapOf(
                    "titulo" to titulo,
                    "data_hora" to Date(),
                    "timestamp" to agora,
                    "seguro" to resultadoIA.ehSeguro,
                    "motivo_ia" to resultadoIA.detalhes,
                    "dispositivo" to "Celular do Filho",
                    "codigo_pareamento" to codigoFilho // O Carimbo que liga ao App do Pai
                )

                bancoDeDados.collection("historico_parental")
                    .add(logVideo)
                    .addOnSuccessListener { doc ->
                        Log.d(TAG, "☁️ SUCESSO! Salvo na nuvem ID: ${doc.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Erro ao salvar na nuvem", e)
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro Crítico no Fluxo IA/Firebase: ${e.message}")
                // Remove do cache para tentar novamente se der erro de rede
                cacheVideosAnalisados.remove(titulo)
            }
        }
    }

    // ==============================
    // 🔍 INTEGRAÇÃO YOUTUBE DATA API
    // ==============================

    private suspend fun analisarVideoSilenciosamente(titulo: String) : String {
        return withContext(Dispatchers.IO) {
            var resultadoComentarios = ""
            try {
                Log.i(TAG, "🔍 Buscando ID na API para o título...")

                // 1. BUSCA O ID DO VÍDEO PELO TÍTULO
                val tituloFormatado = URLEncoder.encode(titulo, "UTF-8")
                val urlBusca = URL("https://www.googleapis.com/youtube/v3/search?part=id&q=$tituloFormatado&type=video&key=$YOUTUBE_API_KEY&maxResults=1")

                val conexaoBusca = urlBusca.openConnection() as HttpURLConnection
                conexaoBusca.requestMethod = "GET"

                if (conexaoBusca.responseCode == 200) {
                    val respostaBusca = conexaoBusca.inputStream.bufferedReader().readText()
                    val jsonBusca = JSONObject(respostaBusca)
                    val items = jsonBusca.getJSONArray("items")

                    if (items.length() > 0) {
                        val videoId = items.getJSONObject(0).getJSONObject("id").getString("videoId")
                        Log.i(TAG, "✅ Vídeo Encontrado na API! ID: $videoId")
                        resultadoComentarios = buscarComentarios(videoId)
                    } else {
                        Log.w(TAG, "❌ Nenhum vídeo correspondente encontrado na API.")
                    }
                } else {
                    Log.e(TAG, "Erro na API de Busca: Código ${conexaoBusca.responseCode}")
                }
                conexaoBusca.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Erro crítico ao consultar API do YouTube", e)
            }

            return@withContext resultadoComentarios
        }
    }

    private fun buscarComentarios(videoId: String) : String {
        val listaDeComentarios = mutableListOf<String>()
        try {
            val urlComentarios = URL("https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=$videoId&textFormat=plainText&key=$YOUTUBE_API_KEY&maxResults=5")
            val conexaoComentarios = urlComentarios.openConnection() as HttpURLConnection
            conexaoComentarios.requestMethod = "GET"

            when (conexaoComentarios.responseCode) {
                200 -> {
                    val respostaComentarios = conexaoComentarios.inputStream.bufferedReader().readText()
                    val jsonComentarios = JSONObject(respostaComentarios)
                    val items = jsonComentarios.getJSONArray("items")

                    if (items.length() == 0) {
                        Log.d(TAG, "💬 O vídeo não possui comentários.")
                        return ""
                    }

                    for (i in 0 until items.length()) {
                        val comentarioObjeto = items.getJSONObject(i)
                            .getJSONObject("snippet")
                            .getJSONObject("topLevelComment")
                            .getJSONObject("snippet")

                        val texto = comentarioObjeto.getString("textDisplay")
                        // Limpamos quebras de linha para ficar uma string mais "reta" para a IA
                        listaDeComentarios.add(texto.replace("\n", " "))
                        Log.d(TAG, "💬 Comentário: $texto")
                    }
                }
                403 -> {
                    Log.w(TAG, "⚠️ Comentários desativados para este vídeo (Possível conteúdo infantil/COPPA).")
                }
                else -> {
                    Log.e(TAG, "Erro na API de Comentários: Código ${conexaoComentarios.responseCode}")
                }
            }
            conexaoComentarios.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar comentários", e)
        }
        // Transforma a lista numa única String, separada por " | "
        // Exemplo: "Comentario 1 | Comentario 2 | Comentario 3"
        return listaDeComentarios.joinToString(separator = " | ")

    }

    // ==============================
    // 🔍 DETECÇÃO DE TELA DE VÍDEO
    // ==============================

    private fun estaNaTelaDeReproducao(root: AccessibilityNodeInfo): Boolean {
        // Baseado no seu log, a tela ativa de vídeo possui o "watch_panel" ou "watch_list".
        // Quando o usuário volta para a tela inicial, esses IDs desaparecem.
        val watchPanel = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_panel")
        val watchList = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_list")

        return watchPanel.isNotEmpty() || watchList.isNotEmpty()
    }

    // ==============================
    // 🎯 CAPTURA DO TÍTULO REAL
    // ==============================

    private fun capturarTituloAtual(root: AccessibilityNodeInfo): String? {
        val listaTitulos = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/title")

        for (node in listaTitulos) {
            val classe = node.className?.toString() ?: ""
            val texto = node.text?.toString()

            // O Log mostrou que o título principal é do tipo TextView e possui texto.
            // Os vídeos recomendados que ficam na mesma tela são do tipo View.
            if (classe == "android.widget.TextView" && !texto.isNullOrBlank()) {
                return texto.trim()
            }
        }

        return null
    }
}