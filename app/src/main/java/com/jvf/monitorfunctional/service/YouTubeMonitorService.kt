package com.jvf.monitorfunctional.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class YouTubeMonitorService : AccessibilityService() {

    private val TAG = "JVF_Service"

    // Variáveis de Controle
    private var candidatoTitulo: String = ""
    private var jobEstabilizacao: Job? = null

    // Cache para evitar gastar API com o mesmo vídeo (Anti-Spam de 5 minutos)
    private val cacheVideosAnalisados = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        Log.i(TAG, "=== SERVIÇO INICIADO: MONITORAMENTO ATIVO (MODO NUVEM) ===")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Filtra eventos para economizar processamento
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            val rootNode = rootInActiveWindow ?: return

            // Segurança: Só processa se for o app do YouTube
            if (rootNode.packageName?.toString()?.contains("youtube") == true) {
                // Inicia a busca. O retorno 'Boolean' serve para parar assim que achar o primeiro.
                buscarTituloNaForcaBruta(rootNode)
            }
        }
    }

    /**
     * Varre a árvore de visualização recursivamente.
     * Retorna TRUE se encontrou um título válido, para interromper a busca nos irmãos (vídeos recomendados).
     */
    private fun buscarTituloNaForcaBruta(node: AccessibilityNodeInfo): Boolean {
        // 1. Tenta analisar o nó atual
        if (node.childCount == 0) {
            val achou = analisarNo(node)
            if (achou) return true // <--- SUCESSO: Para a varredura imediatamente
        }

        // 2. Se não achou, mergulha nos filhos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val encontrouNoFilho = buscarTituloNaForcaBruta(child)
                child.recycle() // Importante: Libera memória do nó

                if (encontrouNoFilho) return true // <--- FILHO ACHOU: Para a varredura
            }
        }
        return false
    }

    /**
     * Verifica se um nó específico contém um título válido de vídeo.
     */
    private fun analisarNo(node: AccessibilityNodeInfo): Boolean {
        val texto = node.text?.toString()
        val descricao = node.contentDescription?.toString()
        val candidato = texto ?: descricao

        if (!candidato.isNullOrEmpty()) {

            // FILTRO 1: Ignora elementos de cabeçalho (topo da tela)
            if (node.viewIdResourceName?.contains("header") == true) return false

            // FILTRO 2: Ignora textos muito curtos (botões, horas)
            if (candidato.length < 15) return false

            // FILTRO 3: Filtro de Ruído (Palavras proibidas, anúncios, etc)
            if (ehRuido(candidato)) return false

            // SE PASSOU: Inicia o processo de estabilização (Debounce)
            iniciarEstabilizacaoTitulo(candidato)
            return true // Avisa que encontrou
        }
        return false
    }

    private fun ehRuido(texto: String): Boolean {
        val textoLower = texto.lowercase()

        // 1. LISTA NEGRA AMPLIADA (Baseada nos seus logs)
        val termosProibidos = listOf(
            // Comandos de Player
            "rewind", "fast forward", "play", "pause", "stop", "next", "previous",
            "minimizar", "expandir", "full screen", "tela cheia",
            "mute", "unmute", "volume", "cast", "cc", "legendas",
            "settings", "configurações", "share", "compartilhar",
            "save", "salvar", "download", "clip", "thanks", "valeu",
            "dislike", "gostei", "não gostei", "inscrever-se", "subscribe",

            // Palavras de Sistema/Status
            "view", "visualizaç", "subscriber", "inscrito", "comment", "comentário",
            "notification", "notificação", "new content", "novo conteúdo",
            "autoplay", "reprodução automática", "chat", "top chat",
            "sponsored", "patrocinado", "ad ·", "anúncio",

            // Termos de Tempo (O grande vilão dos seus logs)
            "seconds of", "segundos de", "minutes of", "minutos de",
            "hours of", "horas de", "remaining", "restante",
            "ago", "atrás" // Ex: "2 days ago"
        )

// Se tiver qualquer um desses termos, LIXO.
        if (termosProibidos.any { textoLower.contains(it) }) return true

        // 2. REGEX DE TEMPO AVANÇADO
        // Pega: "3 hours, 1 second", "10:00", "1:05:20"
        // Se o texto tiver muito número e palavras de tempo, é duração.
        if (textoLower.matches(Regex(".*\\d+.*(hour|hora|minute|minuto|second|segundo|sec|min).*"))) return true
        if (textoLower.matches(Regex("^\\d+:\\d+$"))) return true
        if (textoLower.matches(Regex("^\\d+:\\d+:\\d+$"))) return true

        // Pega padrão "1 of 2" (Anúncios)
        if (textoLower.matches(Regex(".*\\d+ (of|de) \\d+.*"))) return true

        // 3. FILTRO DE NOME DE CANAL (@)
        if (textoLower.startsWith("@")) return true

        // 4. FILTRO DE TAMANHO (Ajuste Fino)
        // "Counter-Strike 2" tem 16 letras. Títulos reais geralmente são frases.
        // Vamos subir a régua para 20 caracteres.
        // Isso mata tags de jogos e botões descritivos curtos, mas mantém títulos reais.
        if (texto.length < 20) return true

        // 5. FILTRO DE DESCRIÇÃO GIGANTE
        // Se for maior que 150 caracteres, provavelmente é a descrição do vídeo e não o título.
        if (texto.length > 150) return true

        return false
    }

    /**
     * Sistema de "Debounce": Espera o título ficar na tela por 2.5s antes de processar.
     * Isso evita capturar títulos de anúncios que passam rápido ou durante a rolagem.
     */
    private fun iniciarEstabilizacaoTitulo(tituloNovo: String) {
        // Se o título for idêntico ao que já estamos esperando, não reinicia o timer
        if (tituloNovo == candidatoTitulo) return

        candidatoTitulo = tituloNovo

        // Cancela qualquer contagem anterior, pois o texto mudou
        jobEstabilizacao?.cancel()

        // Inicia novo timer
        jobEstabilizacao = CoroutineScope(Dispatchers.Main).launch {
            delay(2500) // Espera 2.5 segundos

            // Se chegou aqui, o texto estabilizou. Processa!
            processarTituloReal(candidatoTitulo)
        }
    }

    private fun processarTituloReal(titulo: String) {
        val agora = System.currentTimeMillis()

        // 1. Verifica no Cache se já analisamos esse vídeo recentemente
        val ultimaVez = cacheVideosAnalisados[titulo] ?: 0L
        if ((agora - ultimaVez) < (5 * 60 * 1000)) {
            // Log.v(TAG, "Ignorado pelo Cache: $titulo")
            return
        }

        // 2. Atualiza o cache e prepara o envio
        cacheVideosAnalisados[titulo] = agora
        Log.e(TAG, "✅ TÍTULO NOVO CAPTURADO: $titulo - ENVIANDO PARA IA...")

        // 3. Processamento em Background (IA + Firebase)
        /*CoroutineScope(Dispatchers.IO).launch {
            try {
                // Passo A: Instancia a IA
                val analisador = AnaliseIA() // Certifique-se que a classe AnaliseIA existe

                // Passo B: Pergunta para a IA
                val resultadoIA = analisador.verificarSeguranca(titulo)
                Log.i(TAG, "🤖 IA Respondeu: Seguro=${resultadoIA.ehSeguro} | Motivo: ${resultadoIA.detalhes}")

                // Passo C: Salva no Firebase
                val bancoDeDados = Firebase.firestore

                val logVideo = hashMapOf(
                    "titulo" to titulo,
                    "data_hora" to Date(),
                    "timestamp" to agora,
                    "seguro" to resultadoIA.ehSeguro,
                    "motivo_ia" to resultadoIA.detalhes,
                    "dispositivo" to "Celular do Filho"
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
        }*/
    }

    override fun onInterrupt() {
        Log.w(TAG, "Serviço de Acessibilidade Interrompido!")
    }
}