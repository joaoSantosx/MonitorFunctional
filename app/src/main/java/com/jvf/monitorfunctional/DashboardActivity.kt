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
import android.content.SharedPreferences
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.content.Context


class DashboardActivity : AppCompatActivity() {

    private lateinit var adapter: LogAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var btnVincular: ImageView
    private lateinit var btnLimpar: ImageView
    private lateinit var prefs: SharedPreferences
    private val TAG = "JVF_Dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Inicializa o SharedPreferences
        val prefs = getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)

         recyclerView = findViewById<RecyclerView>(R.id.recyclerLogs)
         txtEmpty = findViewById<TextView>(R.id.txtEmptyState)
         btnLimpar = findViewById<ImageView>(R.id.btnLimpar)
         btnVincular = findViewById<ImageView>(R.id.btnVincular) // <--- O ID CORRETO AQUI

        // Configuração da Lista
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Certifique-se de que a sua variável 'adapter' está declarada no topo da classe
        adapter = LogAdapter(emptyList())
        recyclerView.adapter = adapter

        // 2. CONFIGURANDO OS CLIQUES
        btnVincular.setOnClickListener {
            solicitarCodigoDoFilho()
        }

        btnLimpar.setOnClickListener {
            confirmarLimpeza()
        }

        // 3. INICIANDO A BUSCA
        lerDadosDoFirebase()
    }

    private fun solicitarCodigoDoFilho() {
        val input = android.widget.EditText(this)
        input.hint = "Ex: A7F9B2"
        input.gravity = android.view.Gravity.CENTER

        AlertDialog.Builder(this)
            .setTitle("Vincular Dispositivo")
            .setMessage("Digite o código de 6 dígitos gerado no celular da criança:")
            .setView(input)
            .setPositiveButton("Vincular") { _, _ ->
                val codigoDigitado = input.text.toString().trim().uppercase()

                if (codigoDigitado.isNotEmpty()) {
                    // Salva o código que o pai quer monitorar
                    val prefs = getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("codigo_monitorado", codigoDigitado).apply()

                    Toast.makeText(this, "Vinculado! Carregando dados...", Toast.LENGTH_SHORT).show()
                    lerDadosDoFirebase() // Recarrega a lista
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun confirmarLimpeza() {
        AlertDialog.Builder(this)
            .setTitle("Limpar visualização?")
            .setMessage("Isso vai ocultar o histórico atual da sua tela. Os dados continuarão salvos na nuvem para segurança.")
            .setPositiveButton("Limpar") { _, _ ->
                executarLimpeza()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // <--- NOVA FUNÇÃO: Salva o momento atual como "Ponto de Corte"
    private fun executarLimpeza() {
        val agora = System.currentTimeMillis()

        // Salva na memória: "Só mostre coisas depois de AGORA"
        prefs.edit().putLong("ponto_de_corte", agora).apply()

        Toast.makeText(this, "Histórico limpo!", Toast.LENGTH_SHORT).show()

        // Recarrega a lista (o filtro vai acontecer dentro do lerDadosDoFirebase)
        lerDadosDoFirebase()
    }
    private fun lerDadosDoFirebase() {
        val prefs = getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
        val codigoMonitorado = prefs.getString("codigo_monitorado", "") ?: ""
        val pontoDeCorte = prefs.getLong("ponto_de_corte", 0L)

        // 🔒 NOVA VERIFICAÇÃO: O Pai já vinculou um filho?
        if (codigoMonitorado.isEmpty()) {
            txtEmpty.text = "Nenhum dependente vinculado.\nClique no botão '+' acima para vincular."
            txtEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return // Interrompe a função aqui, não faz a busca no Firebase!
        }

        // Se chegou aqui, o pai tem um código vinculado. Vamos buscar!
        val db = Firebase.firestore

        db.collection("historico_parental")
            .whereEqualTo("codigo_pareamento", codigoMonitorado) // Traz só os vídeos DESSA criança
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limita aos últimos 50 para economia
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

                            // FILTRO DO "LIMPAR HISTÓRICO": Só mostra vídeos após o ponto de corte
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

                    // Atualiza a tela (Mostra a lista ou mostra mensagem de vazio)
                    if (listaTemporaria.isEmpty()) {
                        txtEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        txtEmpty.text = "Histórico limpo ou sem novos vídeos."
                    } else {
                        txtEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.atualizarLista(listaTemporaria)
                    }
                }
            }
    }
}