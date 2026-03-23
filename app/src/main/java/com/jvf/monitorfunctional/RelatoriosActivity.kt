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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import java.util.*

data class AcessoPorHora(val hora: Int, val quantidade: Int)
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

private object RelColors {
    val azulEscuro     = Color(0xFF0D47A1)
    val azulPrimario   = Color(0xFF1565C0)
    val azulMedio      = Color(0xFF3B82F6)
    val azulClaro      = Color(0xFFBFDBFE)
    val azulMuitoClaro = Color(0xFFE0EFFF)
    val cinzaFundo     = Color(0xFFF0F4F8)
    val bordaCard      = Color(0xFFE2E8F0)
    val textoPrimario  = Color(0xFF1E293B)
    val textoMuted     = Color(0xFF94A3B8)
    val cinzaClaro     = Color(0xFFF1F5F9)
    val branco         = Color.White

    val gradienteTopBar = Brush.linearGradient(
        listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
    )
    val gradienteBarra = Brush.verticalGradient(
        listOf(Color(0xFF3B82F6), Color(0xFF1565C0))
    )
}


@Composable
fun RelatoriosScreen(codigoFilho: String, onVoltar: () -> Unit) {
    val db = Firebase.firestore

    var listaAcessos by remember { mutableStateOf(List(24) { AcessoPorHora(it, 0) }) }
    var carregando by remember { mutableStateOf(true) }

    LaunchedEffect(codigoFilho) {
        val vinteQuatroHorasAtras = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)

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
                        mapaHoras[cal.get(Calendar.HOUR_OF_DAY)]++
                    }
                }
                listaAcessos = mapaHoras.mapIndexed { i, qtd -> AcessoPorHora(i, qtd) }
                carregando = false
            }
            .addOnFailureListener { carregando = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RelColors.cinzaFundo)
            .verticalScroll(rememberScrollState())
    ) {
        RelatoriosTopBar(
            listaAcessos = listaAcessos,
            carregando = carregando,
            onVoltar = onVoltar
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (carregando) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RelColors.azulPrimario)
            }
        } else {
            RelSectionTitle("Distribuição por hora")
            GraficoBarras(dados = listaAcessos)

            Spacer(modifier = Modifier.height(4.dp))

            RelSectionTitle("Top horários de pico")
            RankingHorarios(dados = listaAcessos)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RelatoriosTopBar(
    listaAcessos: List<AcessoPorHora>,
    carregando: Boolean,
    onVoltar: () -> Unit
) {
    //calculo status
    val totalVideos = listaAcessos.sumOf { it.quantidade }
    val horaPico = listaAcessos.maxByOrNull { it.quantidade }
        ?.takeIf { it.quantidade > 0 }
        ?.let { "%02dh".format(it.hora) } ?: "--"
    val horasAtivas = listaAcessos.count { it.quantidade > 0 }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RelColors.gradienteTopBar)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .offset(x = 280.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset(x = 290.dp, y = 10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

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
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .clickable { onVoltar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "PREMIUM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gráfico de Horários",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Últimas 24 horas",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPilulaTopBar(
                    valor = if (carregando) "..." else "$totalVideos",
                    label = "Vídeos hoje",
                    modifier = Modifier.weight(1f)
                )
                StatPilulaTopBar(
                    valor = if (carregando) "..." else horaPico,
                    label = "Pico de uso",
                    modifier = Modifier.weight(1f)
                )
                StatPilulaTopBar(
                    valor = if (carregando) "..." else "$horasAtivas",
                    label = "Horas ativas",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatPilulaTopBar(valor: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = valor,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            lineHeight = 22.sp
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

//Título
@Composable
private fun RelSectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = RelColors.textoMuted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

//Gráfico
@Composable
fun GraficoBarras(dados: List<AcessoPorHora>) {
    val maximo = dados.maxOfOrNull { it.quantidade }?.takeIf { it > 0 } ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RelColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RelColors.bordaCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Acessos nas últimas 24h",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RelColors.textoPrimario
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dados.forEach { acesso ->
                    val proporcao = acesso.quantidade.toFloat() / maximo.toFloat()

                    val alturaAnimada by animateFloatAsState(
                        targetValue = proporcao,
                        animationSpec = tween(durationMillis = 600, delayMillis = acesso.hora * 12),
                        label = "barra_${acesso.hora}"
                    )

                    val corBarra = when {
                        proporcao >= 0.70f -> RelColors.gradienteBarra
                        proporcao >= 0.30f -> Brush.verticalGradient(
                            listOf(RelColors.azulClaro, RelColors.azulClaro)
                        )
                        else -> Brush.verticalGradient(
                            listOf(RelColors.azulMuitoClaro, RelColors.azulMuitoClaro)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(alturaAnimada.coerceAtLeast(if (acesso.quantidade > 0) 0.03f else 0.02f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(corBarra)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("00h", "06h", "12h", "18h", "23h").forEach { label ->
                    Text(text = label, fontSize = 9.sp, color = RelColors.textoMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legenda
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendaItem(cor = RelColors.azulPrimario, texto = "Pico")
                LegendaItem(cor = RelColors.azulClaro, texto = "Normal")
                LegendaItem(cor = RelColors.azulMuitoClaro, texto = "Baixo")
            }
        }
    }
}

@Composable
private fun LegendaItem(cor: Color, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cor)
        )
        Text(text = texto, fontSize = 10.sp, color = RelColors.textoMuted)
    }
}

//Ranking
@Composable
fun RankingHorarios(dados: List<AcessoPorHora>) {
    // Pega os top 4 horários com mais acessos
    val top4 = dados
        .filter { it.quantidade > 0 }
        .sortedByDescending { it.quantidade }
        .take(4)

    val maximo = top4.firstOrNull()?.quantidade?.takeIf { it > 0 } ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RelColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RelColors.bordaCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (top4.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum acesso registrado nas últimas 24h",
                        fontSize = 12.sp,
                        color = RelColors.textoMuted
                    )
                }
            } else {
                top4.forEachIndexed { index, acesso ->
                    RankingRow(
                        posicao = index + 1,
                        hora = acesso.hora,
                        quantidade = acesso.quantidade,
                        maximo = maximo,
                        isUltimo = index == top4.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingRow(
    posicao: Int,
    hora: Int,
    quantidade: Int,
    maximo: Int,
    isUltimo: Boolean
) {
    val proporcaoAnimada by animateFloatAsState(
        targetValue = quantidade.toFloat() / maximo.toFloat(),
        animationSpec = tween(700, delayMillis = posicao * 80),
        label = "ranking_$posicao"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Número de posição
            Text(
                text = "$posicao",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = RelColors.textoMuted,
                modifier = Modifier.width(16.dp)
            )

            // Faixa de hora
            Text(
                text = "%02dh – %02dh".format(hora, (hora + 1) % 24),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RelColors.textoPrimario,
                modifier = Modifier.width(72.dp)
            )

            // Barra de progresso
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(RelColors.cinzaClaro)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(proporcaoAnimada)
                        .clip(RoundedCornerShape(99.dp))
                        .background(RelColors.gradienteBarra)
                )
            }

            // Contagem
            Text(
                text = "$quantidade",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RelColors.azulPrimario,
                modifier = Modifier.width(28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }

        // Divisória
        if (!isUltimo) {
            HorizontalDivider(
                color = RelColors.bordaCard,
                thickness = 0.5.dp
            )
        }
    }
}