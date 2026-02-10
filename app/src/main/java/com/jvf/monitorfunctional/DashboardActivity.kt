package com.jvf.monitorfunctional

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.jvf.monitorfunctional.ui.LogAdapter
import com.jvf.monitorfunctional.ui.LogVideoApp

class DashboardActivity : AppCompatActivity() {

    private lateinit var adapter: LogAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtEmpty: TextView
    private val TAG = "JVF_Dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Configurações iniciais da tela
        recyclerView = findViewById(R.id.recyclerLogs)
        txtEmpty = findViewById(R.id.txtEmptyState)

        // Define que a lista será vertical
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicia o adaptador com lista vazia por enquanto
        adapter = LogAdapter(emptyList())
        recyclerView.adapter = adapter

        // Começa a escutar o Firebase
        lerDadosDoFirebase()
    }

    private fun lerDadosDoFirebase() {
        val db = Firebase.firestore

        // Conecta na coleção "historico_parental"
        // Ordena por "timestamp" (do mais novo para o mais antigo)
        db.collection("historico_parental")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limita aos últimos 50 para não travar
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.w(TAG, "Erro de leitura", e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    // Esconde o texto "Aguardando..."
                    txtEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    val listaTemporaria = mutableListOf<LogVideoApp>()

                    for (doc in snapshots) {
                        try {
                            // Pega os dados com segurança (se faltar campo, usa padrão)
                            val titulo = doc.getString("titulo") ?: "Sem título"
                            val seguro = doc.getBoolean("seguro") ?: true
                            val motivo = doc.getString("motivo_ia") ?: "Sem análise"
                            val timestamp = doc.getLong("timestamp") ?: 0L

                            listaTemporaria.add(LogVideoApp(titulo, seguro, motivo, timestamp))
                        } catch (erro: Exception) {
                            Log.e(TAG, "Erro ao converter item: ${doc.id}", erro)
                        }
                    }

                    // Atualiza a lista na tela
                    adapter.atualizarLista(listaTemporaria)
                } else {
                    // Se não tiver nada, mostra o texto de vazio
                    txtEmpty.visibility = View.VISIBLE
                    txtEmpty.text = "Nenhum histórico encontrado."
                }
            }
    }
}