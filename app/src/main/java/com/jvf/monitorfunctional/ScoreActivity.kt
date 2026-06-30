package com.jvf.monitorfunctional

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class ScoreActivity : AppCompatActivity() {
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
            ScoreScreen(
                codigoFilho = codigoMonitorado,
                onVoltar = { finish() }
            )
        }
    }
}

// ─── Estrutura de Dados para o Gráfico ───
data class CategoriaScore(
    val nome: String,
    val percentual: Int,
    val cor: Color
)

@Composable
fun ScoreScreen(codigoFilho: String, onVoltar: () -> Unit) {
    val db = Firebase.firestore
    var carregando by remember { mutableStateOf(true) }

    // Estados Matemáticos
    var scoreGeral by remember { mutableStateOf(0) }
    var totalVideos by remember { mutableStateOf(0) }
    var listaCategorias by remember { mutableStateOf<List<CategoriaScore>>(emptyList()) }

    // Busca e Cálculo
    LaunchedEffect(codigoFilho) {
        try {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            val inicioDaSemana = calendar.timeInMillis

            // 2. Buscar no Firebase filtrando por data
            val snapshot = db.collection("historico_parental")
                .whereEqualTo("codigo_pareamento", codigoFilho)
                .whereGreaterThanOrEqualTo("timestamp", inicioDaSemana)
                .get()
                .await()

            val documentos = snapshot.documents
            totalVideos = documentos.size

            if (totalVideos > 0) {
                // 3. Contar vídeos seguros
                val seguros = documentos.count { it.getBoolean("seguro") ?: true }
                scoreGeral = ((seguros.toFloat() / totalVideos) * 100).toInt()

                // 4. Agrupar por categoria e calcular os percentuais
                val mapAgrupado = documentos.groupBy { doc -> extrairCategoria(doc.getString("motivo_ia")) }

                val listaCalculada = mapAgrupado.map { (nomeCategoria, listaVideosDaCategoria) ->
                    val percentual = ((listaVideosDaCategoria.size.toFloat() / totalVideos) * 100).toInt()
                    val cor = definirCorDaCategoria(nomeCategoria)
                    CategoriaScore(nomeCategoria, percentual, cor)
                }

                // Ordena do maior percentual para o menor para o gráfico ficar bonito
                listaCategorias = listaCalculada.sortedByDescending { it.percentual }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            carregando = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        ScoreTopBar(onVoltar)

        if (carregando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFEA580C))
            }
        } else if (totalVideos == 0) {
            // Tela Vazia
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nenhum dado na última semana", color = Color.Gray, fontSize = 16.sp)
                    Text("O monitorado não assistiu vídeos ou o app esteve offline.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        } else {
            // Tela com os Gráficos
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                ScoreSemanalCard(scoreGeral, totalVideos, listaCategorias)

                Spacer(modifier = Modifier.height(8.dp))

                if (scoreGeral < 50) {
                    AlertaScoreBaixo()
                }
            }
        }
    }
}

@Composable
fun ScoreSemanalCard(scoreGeral: Int, totalVideos: Int, categorias: List<CategoriaScore>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Cabeçalho do Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFFEA580C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resumo da Semana", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                }
                Text("$totalVideos vídeos", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Geral Gigante
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$scoreGeral%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 46.sp,
                    color = if (scoreGeral >= 70) Color(0xFF4CAF50) else if (scoreGeral >= 40) Color(0xFFFF9800) else Color(0xFFF44336)
                )
                Text(
                    text = " Seguro",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            // Lista de Categorias
            Text("Distribuição de Conteúdo:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            categorias.forEach { categoria ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoria.nome,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )

                    LinearProgressIndicator(
                        progress = { categoria.percentual / 100f },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = categoria.cor,
                        trackColor = Color(0xFFF1F5F9)
                    )

                    Text(
                        text = "${categoria.percentual}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier
                            .width(48.dp)
                            .padding(start = 8.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun AlertaScoreBaixo() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), // Vermelho ultra claro
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Atenção ao Conteúdo", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 14.sp)
                Text("O score de segurança desta semana está abaixo da média recomendada. Considere rever as regras da Inteligência Artificial nas configurações.", color = Color(0xFF7F1D1D), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ScoreTopBar(onVoltar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFFC2410C), Color(0xFFEA580C)))) // Gradiente Laranja
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onVoltar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("RELATÓRIO", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.2.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Score da Semana", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ─── Lógica Visual: Cores por Categoria ───
fun definirCorDaCategoria(nome: String): Color {
    return when (nome.uppercase()) {
        "EDUCATIVO" -> Color(0xFF10B981) // Verde esmeralda
        "ENTRETENIMENTO" -> Color(0xFF3B82F6) // Azul
        "VIOLÊNCIA" -> Color(0xFFEF4444) // Vermelho
        "ADULTO" -> Color(0xFFDC2626) // Vermelho escuro
        "FILTRO PERSONALIZADO" -> Color(0xFFF97316) // Laranja
        else -> Color(0xFF94A3B8) // Cinza (Outros)
    }
}

fun extrairCategoria(motivo: String?): String {
    if (motivo == null) return "Outros"

    val motivoLower = motivo.lowercase()
    val categoriasPossiveis = listOf("Violência", "Adulto", "Educativo", "Entretenimento", "Filtro Personalizado")

    for (cat in categoriasPossiveis) {
        if (motivoLower.contains(cat.lowercase())) {
            return cat
        }
    }
    return "Outros"
}