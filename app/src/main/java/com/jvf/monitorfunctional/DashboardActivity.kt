package com.jvf.monitorfunctional

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.jvf.monitorfunctional.ui.LogVideoApp
import android.content.SharedPreferences
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import com.jvf.monitorfunctional.ui.DashboardScreen
class DashboardActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private val TAG = "JVF_Dashboard"

    private val listaDeVideosState = mutableStateListOf<LogVideoApp>()
    private var apelidoState by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)

        atualizarApelidoState()
        lerDadosDoFirebase()

        // Inicio do Compose
        setContent {
            MaterialTheme {
                DashboardScreen(
                    apelido = apelidoState,
                    listaVideos = listaDeVideosState,
                    onLimparClick = { confirmarLimpeza() },
                )
            }
        }
    }

    private fun atualizarApelidoState() {
        apelidoState = prefs.getString("apelido_monitorado", "") ?: ""
    }

    private fun confirmarLimpeza() {
        AlertDialog.Builder(this)
            .setTitle("Limpar visualização?")
            .setMessage("Isto vai ocultar o histórico atual do seu ecrã. Os dados continuarão salvos na nuvem para segurança.")
            .setPositiveButton("Limpar") { _, _ ->
                executarLimpeza()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarLimpeza() {
        val agora = System.currentTimeMillis()
        prefs.edit{
            putLong("ponto_de_corte", agora)
    }
    Toast.makeText(this, "Histórico limpo!", Toast.LENGTH_SHORT).show()
    lerDadosDoFirebase()
}

    private fun lerDadosDoFirebase() {
        val codigoMonitorado = prefs.getString("codigo_monitorado", "") ?: ""
        val pontoDeCorte = prefs.getLong("ponto_de_corte", 0L)

        if (codigoMonitorado.isEmpty()) {
            listaDeVideosState.clear()
            return
        }

        val db = Firebase.firestore
        db.collection("historico_parental")
            .whereEqualTo("codigo_pareamento", codigoMonitorado)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.w(TAG, "Erro de leitura no Firebase", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val listaTemporaria = mutableListOf<LogVideoApp>()

                    for (doc in snapshots) {
                        try {
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            if (timestamp > pontoDeCorte) {
                                val titulo = doc.getString("titulo") ?: "Sem título"
                                val seguro = doc.getBoolean("seguro") ?: true
                                val motivo = doc.getString("motivo_ia") ?: "Sem análise"

                                listaTemporaria.add(LogVideoApp(titulo, seguro, motivo, timestamp))
                            }
                        } catch (erro: Exception) {
                            Log.e(TAG, "Erro ao converter item do Firebase", erro)
                        }
                    }
                    listaDeVideosState.clear()
                    listaDeVideosState.addAll(listaTemporaria)
                }
            }
    }
}