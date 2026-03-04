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


       //Defesa contra crianças desativando o acessibility service
        val pacoteAtual = event.packageName?.toString() ?: ""
        if (pacoteAtual.contains("settings") || pacoteAtual.contains("config") || pacoteAtual.contains("accessibility")) {

            Log.e("JVF_DEFESA", "!O usuário abriu as configurações! Pacote: $pacoteAtual")

            val prefs = applicationContext.getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
            val protecaoAtiva = prefs.getBoolean("protecao_ativa", true)

            Log.e("JVF_DEFESA", "A proteção está ativa no banco local? $protecaoAtiva")

            if (protecaoAtiva) {
                val estaNaTelaDeDesativar = procurarTextoNaTela(rootNode, "Monitor Parental")

                Log.e("JVF_DEFESA", "O nome do serviço apareceu na tela? $estaNaTelaDeDesativar")

                if (estaNaTelaDeDesativar) {
                    Log.e("JVF_DEFESA", " ENCERRANDO tela de configurações!")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    android.widget.Toast.makeText(applicationContext, "Área restrita aos responsáveis.", android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        // Garante que está no YouTube
        if (event.packageName?.toString()?.contains("youtube") != true) return

        //Garante que o usuário está na tela de reprodução
        if (!estaNaTelaDeReproducao(rootNode)) return

        val titulo = capturarTituloAtual(rootNode) ?: return

        // Evita log duplicado
        if (titulo == ultimoTituloCapturado) return
        ultimoTituloCapturado = titulo

        Log.e(TAG, "Assistindo: $titulo")

        //busca na api do (fora da thread principal)
        CoroutineScope(Dispatchers.IO).launch {
            val comentariosConcatenados = analisarVideoSilenciosamente(titulo)
            processarTituloComIA(titulo, comentariosConcatenados)
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Serviço Interrompido")
    }

    // INTEGRAÇÃO IA E FIREBASE
        private fun processarTituloComIA(titulo:String, comentarios: String){

        val agora = System.currentTimeMillis()
        val ultimaVez = cacheVideosAnalisados[titulo] ?: 0L
        if ((agora - ultimaVez) < (5 * 60 * 1000)){
            return
        }
        cacheVideosAnalisados[titulo] = agora

        Log.e(TAG, "Novo título capturado: $titulo - Enviando para anaálise...")

        CoroutineScope(Dispatchers.IO).launch{
            try{
                val prefs = applicationContext.getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
                val codigoFilho = prefs.getString("codigo_filho", "SEM_CODIGO") ?: "SEM_CODIGO"
                val bancoDeDados = Firebase.firestore

                var nivelRigidezAtual = "ALTA" // Nível padrão
                try {
                    if (codigoFilho != "SEM_CODIGO") {
                        val docRegra = bancoDeDados.collection("regras_parentais").document(codigoFilho).get().await()
                        if (docRegra.exists()) {
                            nivelRigidezAtual = docRegra.getString("nivel") ?: "ALTA"
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠ Erro ao buscar regra, usando ALTA por padrão.", e)
                }

                val analisador = AnaliseIA()
                val resultadoIA = analisador.verificarSeguranca(titulo, comentarios, nivelRigidezAtual)
                Log.i(TAG, "IA: Seguro=${resultadoIA.ehSeguro} | Nível: $nivelRigidezAtual | Motivo: ${resultadoIA.detalhes}")

                val logVideo = hashMapOf(
                    "titulo" to titulo,
                    "data_hora" to Date(),
                    "timestamp" to agora,
                    "seguro" to resultadoIA.ehSeguro,
                    "motivo_ia" to resultadoIA.detalhes,
                    "dispositivo" to "Celular do Filho",
                    "codigo_pareamento" to codigoFilho,
                    "nivel_rigidez_usado" to nivelRigidezAtual

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

    //INTEGRAÇÃO YOUTUBE DATA API
    private suspend fun analisarVideoSilenciosamente(titulo: String) : String {
        return withContext(Dispatchers.IO) {
            var resultadoComentarios = ""
            try {
                Log.i(TAG, "Buscando id na API para o título...")

                //Pegar ID pelo título
                val tituloFormatado = URLEncoder.encode(titulo, "UTF-8")
                val urlBusca = URL("https://www.googleapis.com/youtube/v3/search?part=id&q=$tituloFormatado&type=video&key=$youtubeApiKey&maxResults=1")

                val conexaoBusca = urlBusca.openConnection() as HttpURLConnection
                conexaoBusca.requestMethod = "GET"

                if (conexaoBusca.responseCode == 200) {
                    val respostaBusca = conexaoBusca.inputStream.bufferedReader().readText()
                    val jsonBusca = JSONObject(respostaBusca)
                    val items = jsonBusca.getJSONArray("items")

                    if (items.length() > 0) {
                        val videoId = items.getJSONObject(0).getJSONObject("id").getString("videoId")
                        Log.i(TAG, " Vídeo encontrado na API, id: $videoId")
                        resultadoComentarios = buscarComentarios(videoId)
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

            return@withContext resultadoComentarios
        }
    }

    private fun buscarComentarios(videoId: String) : String {
        val listaDeComentarios = mutableListOf<String>()
        try {
            val urlComentarios = URL("https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=$videoId&textFormat=plainText&key=$youtubeApiKey&maxResults=5")
            val conexaoComentarios = urlComentarios.openConnection() as HttpURLConnection
            conexaoComentarios.requestMethod = "GET"

            when (conexaoComentarios.responseCode) {
                200 -> {
                    val respostaComentarios = conexaoComentarios.inputStream.bufferedReader().readText()
                    val jsonComentarios = JSONObject(respostaComentarios)
                    val items = jsonComentarios.getJSONArray("items")

                    if (items.length() == 0) {
                        Log.d(TAG, "Vídeo não possui comentários.")
                        return ""
                    }

                    for (i in 0 until items.length()) {
                        val comentarioObjeto = items.getJSONObject(i)
                            .getJSONObject("snippet")
                            .getJSONObject("topLevelComment")
                            .getJSONObject("snippet")

                        val texto = comentarioObjeto.getString("textDisplay")
                        listaDeComentarios.add(texto.replace("\n", " "))
                        Log.d(TAG, " Comentário: $texto")
                    }
                }
                403 -> {
                    Log.w(TAG, "⚠ Comentários desativados para este vídeo (possível conteúdo infantil).")
                }
                else -> {
                    Log.e(TAG, "Erro na API de comentários: Código ${conexaoComentarios.responseCode}")
                }
            }
            conexaoComentarios.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar comentários", e)
        }
        return listaDeComentarios.joinToString(separator = " | ")

    }

    //DETECÇÃO DE TELA DE VÍDEO
    private fun estaNaTelaDeReproducao(root: AccessibilityNodeInfo): Boolean {
        val watchPanel = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_panel")
        val watchList = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_list")
        return watchPanel.isNotEmpty() || watchList.isNotEmpty()
    }

    // CAPTURA DO TÍTULO REAL

    private fun capturarTituloAtual(root: AccessibilityNodeInfo): String? {
        val listaTitulos = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/title")

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

        // Verifica se o nó atual contém o nome do app
        if (textoDoNo.contains(textoAlvo, ignoreCase = true) || descricaoDoNo.contains(textoAlvo, ignoreCase = true)) {
            return true
        }

        // Se não achou procura nos outros elementos dentro da tela
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val encontrou = procurarTextoNaTela(child, textoAlvo)
                if (encontrou) return true
            }
        }
        return false
    }
}