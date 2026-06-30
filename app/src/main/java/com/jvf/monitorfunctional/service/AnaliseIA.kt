package com.jvf.monitorfunctional.service


import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
class AnaliseIA {
    private val functions: FirebaseFunctions = Firebase.functions

    suspend fun verificarSeguranca(
        tituloVideo: String,
        videoId: String,
        nivelRigidez: String,
        palavrasMonitoradas: List<String> = emptyList(),
        palavrasPermitidas: List<String> = emptyList()
    ): ResultadoAnalise {
        val dados = hashMapOf(
            "tituloVideo" to tituloVideo,
            "videoId" to videoId,
            "nivelRigidez" to nivelRigidez,
            "palavrasMonitoradas" to palavrasMonitoradas,
            "palavrasPermitidas" to palavrasPermitidas
        )

        return try {
            val result = functions
                .getHttpsCallable("analisarVideoGemini")
                .call(dados)
                .await()

            val respostaMap = result.data as? Map<String, Any>
            val ehSeguro = respostaMap?.get("ehSeguro") as? Boolean ?: true
            val detalhes = respostaMap?.get("detalhes") as? String ?: "Análise indisponível."

            ResultadoAnalise(ehSeguro, detalhes)
        } catch (e: Exception) {
            Log.e("App_IA", "Erro ao conectar com o servidor seguro: ${e.message}", e)
            ResultadoAnalise(true, "Erro de conexão com o servidor.")
        }
    }
}
data class ResultadoAnalise(
    val ehSeguro: Boolean,
    val detalhes: String
)
