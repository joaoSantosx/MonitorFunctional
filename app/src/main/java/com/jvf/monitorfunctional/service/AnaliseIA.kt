package com.jvf.monitorfunctional.service

import com.google.ai.client.generativeai.GenerativeModel
import android.util.Log
class AnaliseIA {

    private val apiKey = "chave"

    suspend fun verificarSeguranca(tituloVideo: String): ResultadoAnalise {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        // Prompt Engenheirado para resposta curta e direta
        val prompt = """
            Você é um filtro de segurança.
            Analise este texto extraído da tela do YouTube: "$tituloVideo"
            
            Passo 1: Se o texto parecer um botão (ex: "Pular", "Fechar"), tempo (ex: "12:00"), ou anúncio ("Sponsored", "Ad"), responda APENAS: IGNORAR.
            
            Passo 2: Se for um título de vídeo real, analise para uma criança de 10 anos.
            Responda EXATAMENTE neste JSON:
            {
              "seguro": "SIM" ou "NAO",
              "categoria": "...",
              "motivo": "..."
            }
        """.trimIndent()
        return try {
            val response = generativeModel.generateContent(prompt)
            val texto = response.text ?: ""

            // Aqui faríamos um parse JSON real, mas vamos simplificar pro teste:
            if (texto.uppercase().contains("NAO")) {
                ResultadoAnalise(false, texto)
            } else {
                ResultadoAnalise(true, texto)
            }
        } catch (e: Exception) {

            Log.e("JVF_IA", "Erro na IA: ${e.message}")
            ResultadoAnalise(true, "Erro na análise, assumindo seguro.")
        }
    }
}

data class ResultadoAnalise(
    val ehSeguro: Boolean,
    val detalhes: String
)