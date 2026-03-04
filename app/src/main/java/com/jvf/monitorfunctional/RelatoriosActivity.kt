package com.jvf.monitorfunctional

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.util.Calendar
import com.jvf.monitorfunctional.components.AcessoPorHora
import com.jvf.monitorfunctional.components.GraficoUsoHorario
import androidx.compose.material.icons.automirrored.filled.ArrowBack

class RelatoriosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        val codigoMonitorado = prefs.getString("codigo_monitorado", "") ?: ""

        if (codigoMonitorado.isEmpty()) {
            Toast.makeText(this, "Nenhum dispositivo vinculado para gerar relatórios.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            RelatoriosScreen(
                codigoFilho = codigoMonitorado,
                onVoltar = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(codigoFilho: String, onVoltar: () -> Unit) {
    val db = Firebase.firestore

    // O estado que guarda a lista pronta para o nosso gráfico
    var listaAcessos by remember { mutableStateOf(List(24) { AcessoPorHora(it, 0) }) }
    var carregando by remember { mutableStateOf(true) }

    // Busca os dados no Firebase ao abrir a tela
    LaunchedEffect(codigoFilho) {
        val vinteQuatroHorasAtras = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        db.collection("historico_parental")
            .whereEqualTo("codigo_pareamento", codigoFilho)
            .whereGreaterThan("timestamp", vinteQuatroHorasAtras)
            .get()
            .addOnSuccessListener { result ->
                val mapaHoras = IntArray(24)

                for (doc in result) {
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    if (timestamp > 0L) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = timestamp
                        val hora = cal.get(Calendar.HOUR_OF_DAY)
                        mapaHoras[hora]++
                    }
                }

                val novaLista = mutableListOf<AcessoPorHora>()
                mapaHoras.forEachIndexed { index, qtd -> novaLista.add(AcessoPorHora(index, qtd)) }

                listaAcessos = novaLista
                carregando = false
            }
            .addOnFailureListener { carregando = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatórios de Uso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Horários de Pico", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            Text(
                "Descubra as faixas de horário em que o aplicativo do YouTube é mais acessado.",
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            if (carregando) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                GraficoUsoHorario(dados = listaAcessos)
            }
        }
    }
}