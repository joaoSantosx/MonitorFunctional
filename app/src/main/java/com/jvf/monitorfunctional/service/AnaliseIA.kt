package com.jvf.monitorfunctional.service


import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.GenerativeModel
import android.util.Log
import org.json.JSONObject
import org.json.JSONException
class AnaliseIA {

    private val apiKey = "chaveapi"

    suspend fun verificarSeguranca(tituloVideo: String, comentarios: String): ResultadoAnalise {

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
        )

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            safetySettings = safetySettings
        )


        val prompt = """
            Você é um assistente de controle parental que analisará conteúdos assistidos por crianças no app do Youtube.
            Analise o vídeo com seguinte título: "$tituloVideo"
            
            Abaixo estão alguns comentários feitos por usuários neste vídeo (use-os como contexto para entender o tom e o assunto real do vídeo, caso o título seja enganoso):
            [$comentarios]
            
            Responda APENAS um JSON cru (sem markdown, sem ```json):
            {
              "seguro": "SIM" ou "NAO",
              "categoria": "Violência, Adulto, Educativo, Entretenimento ou Outros",
              "motivo": "Explicação curta e direta para os pais."
            }
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            val textoResposta  = response.text ?: ""

            if (textoResposta.isNullOrEmpty()) {
                Log.w("JVF_IA", "⚠️ A IA retornou vazio para: $tituloVideo")
                // Se veio vazio, assumimos que é seguro para não travar o app, mas avisamos
                return ResultadoAnalise(true, "Análise automática indisponível (Erro na IA).")
            }

            val jsonLimpo = textoResposta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            try {
                // Tenta ler como JSON
                val json = JSONObject(jsonLimpo)

                val seguroTexto = json.optString("seguro", "SIM")
                val categoria = json.optString("categoria", "Geral")
                val motivo = json.optString("motivo", "")

                val ehSeguro = seguroTexto.uppercase().contains("SIM")

                // Formata bonito
                val mensagemFinal = "Categoria: $categoria. $motivo"

                ResultadoAnalise(ehSeguro, mensagemFinal)

            } catch (e: Exception) {
                // 4. PLANO B: Se não for JSON, usa o texto que veio
                Log.w("JVF_IA", "A IA não mandou JSON válido, usando texto bruto.")
                val ehSeguroManual = !textoResposta.uppercase().contains("NAO")
                ResultadoAnalise(ehSeguroManual, "Info: $textoResposta")
            }

        } catch (e: Exception) {
            Log.e("JVF_IA", "Erro de conexão/API: ${e.message}")
            ResultadoAnalise(true, "Erro de conexão.")
        }
    }
}

data class ResultadoAnalise(
    val ehSeguro: Boolean,
    val detalhes: String
)