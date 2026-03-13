package com.jvf.monitorfunctional

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.jvf.monitorfunctional.model.HistoricoPesquisa

class PesquisasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        val codigoMonitorado = prefs.getString("codigo_monitorado", "") ?: ""

        if (codigoMonitorado.isEmpty()) {
            Toast.makeText(this, "Nenhum dispositivo vinculado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                PesquisasScreen(
                    codigoFilho = codigoMonitorado,
                    onVoltar = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesquisasScreen(codigoFilho: String, onVoltar: () -> Unit) {
    var listaPesquisas by remember { mutableStateOf(listOf<HistoricoPesquisa>()) }
    var carregando by remember { mutableStateOf(true) }
    val db = Firebase.firestore

    // Escuta o Firebase
    LaunchedEffect(codigoFilho) {
        val listener = db.collection("historico_pesquisas")
            .whereEqualTo("codigo_pareamento", codigoFilho)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Traz as últimas 100 pesquisas
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    carregando = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val pesquisas = snapshot.documents.mapNotNull { doc ->
                        val termo = doc.getString("termo") ?: return@mapNotNull null
                        val data = doc.getDate("data_hora") ?: Date()
                        HistoricoPesquisa(termo, data)
                    }
                    listaPesquisas = pesquisas
                    carregando = false
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatório de Pesquisas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF673AB7),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Intenções de Busca",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Monitore o que está sendo digitado na barra de pesquisa do YouTube.",
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (carregando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF673AB7))
                }
            } else if (listaPesquisas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma pesquisa registrada ainda.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(listaPesquisas) { pesquisa ->
                        CardPesquisaItem(pesquisa)
                    }
                }
            }
        }
    }
}

@Composable
fun CardPesquisaItem(pesquisa: HistoricoPesquisa) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEDE7F6), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF673AB7),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pesquisa.termo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val formatoHora = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                Text(
                    text = formatoHora.format(pesquisa.dataHora),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
