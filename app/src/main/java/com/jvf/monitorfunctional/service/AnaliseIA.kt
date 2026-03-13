package com.jvf.monitorfunctional.service


import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.GenerativeModel
import android.util.Log
import com.jvf.monitorfunctional.BuildConfig
import org.json.JSONObject
class AnaliseIA {
    private val apiKey = BuildConfig.gemini_api_key

        suspend fun verificarSeguranca(
            tituloVideo: String,
            comentarios: String,
            nivelRigidez: String,
            palavrasMonitoradas: List<String> = emptyList()
        ): ResultadoAnalise {
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

            val regrasParentais = when (nivelRigidez.uppercase()) {
                "BAIXA" -> "Desconsidere jogos violentos fictícios, animes e humor adolescente. Alerte apenas pornografia, crimes reais, uso de drogas e extrema violência."
                "MEDIA" -> "Desconsidere jogos infantis, Minecraft e humor leve. Alerte violência explícita, palavrões pesados e temas adultos."
                "ALTA" -> "Seja extremamente rigoroso. O conteúdo deve ser estritamente infantil (livre para todas as idades). Alerte qualquer palavrão, duplo sentido, armas, jogos violentos ou terror."
                else -> "Seja rigoroso. O conteúdo deve ser adequado para crianças."
            }

            val regraPersonalizada = if (palavrasMonitoradas.isNotEmpty()) {
                """
            ATENÇÃO - REGRA ESTRITA DA FAMÍLIA:
            O responsável quer ser alertado sobre qualquer conteúdo que aborde ou tenha relação com os seguintes temas:
            [ ${palavrasMonitoradas.joinToString(", ")} ]
            Se o vídeo abordar qualquer um desses temas, mesmo que de forma educativa, inofensiva ou positiva, você deve obrigatoriamente:            
            1 - Classificar como "seguro": "NÃO"
            2 - Classificar a "categoria" como: "Filtro Personalizado"
            3 - No campo "motivo", escreva explicitamente: "Alerta de monitoramento personalizado."
            """.trimIndent()
            } else {
                "" // Se não tiver palavra, não vai ter nenhum acrescimo no prompt
            }

            val prompt = """
            Você é um assistente de controle parental que analisará conteúdos assistidos por crianças no app do Youtube.
            Nível de rigidez atual do responsável: "$nivelRigidez"
            Regras de filtragem: $regrasParentais
            $regraPersonalizada
            
            Contexto do vídeo:
            Analise o vídeo com seguinte título: "$tituloVideo"
            Abaixo estão alguns comentários feitos por usuários neste vídeo (use-os como contexto para entender o tom e o assunto real do vídeo, caso o título seja enganoso):
            [$comentarios]
            
            Responda APENAS um JSON cru (sem markdown, sem ```json):
            {
              "seguro": "SIM" ou "NAO",
              "categoria": "Violência, Adulto, Educativo, Entretenimento, Filtro Personalizado ou Outros",
              "motivo": "Explicação curta e direta para os pais."
            }
        """.trimIndent()

            return try {
                val response = generativeModel.generateContent(prompt)
                val textoResposta = response.text ?: ""

                if (textoResposta.isEmpty()) {
                    Log.w("App_IA", "⚠ A IA retornou vazio para: $tituloVideo")
                    // Se veio vazio, a IA avisa que é seguro para não travar o app
                    return ResultadoAnalise(true, "Análise automática indisponível (Erro na IA).")
                }

                val jsonLimpo = textoResposta
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                try {
                    // Tenta ler JSON
                    val json = JSONObject(jsonLimpo)
                    val seguroTexto = json.optString("seguro", "SIM")
                    val categoria = json.optString("categoria", "Geral")
                    val motivo = json.optString("motivo", "")
                    val ehSeguro = seguroTexto.uppercase().contains("SIM")
                    val mensagemFinal = "Categoria: $categoria. $motivo"
                    ResultadoAnalise(ehSeguro, mensagemFinal)

                } catch (e: Exception) {
                    //Se não for JSON usa o texto que veio
                    Log.w(
                        "App_IA",
                        "A IA não retornou JSON válido. Motivo: ${e.message}. usando texto bruto."
                    )
                    val ehSeguroManual = !textoResposta.uppercase().contains("NAO")
                    ResultadoAnalise(ehSeguroManual, "Info: $textoResposta")
                }

            } catch (e: Exception) {
                Log.e("App_IA", "Erro de conexão/API: ${e.message}", e)
                ResultadoAnalise(true, "Erro de conexão.")
            }
        }
    }
data class ResultadoAnalise(
    val ehSeguro: Boolean,
    val detalhes: String
)