package com.jvf.monitorfunctional.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*



@Composable
fun DashboardScreen(
    apelido: String,
    listaVideos: List<LogVideoApp>,
    onLimparClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)) // Fundo cinza bem moderno
    ) {
        TopAppBarModerno(apelido,onLimparClick)
        TermometroCard(listaVideos)

        // Lista de Vídeos
        if (listaVideos.isEmpty()) {
            EstadoVazio()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listaVideos) { video ->
                    VideoLogCard(video)
                }
            }
        }
    }
}

@Composable
fun TermometroCard(listaVideos: List<LogVideoApp>) {
    val total = listaVideos.size
    val seguros = listaVideos.count { it.seguro }
    val isVazio = total == 0

    val porcentagem = if (!isVazio) (seguros.toFloat() / total.toFloat()) else 0f

    //50% laranja, 80% ou mais Verde
    val corAlvo = when {
        isVazio -> Color.LightGray
        porcentagem >= 0.70f -> Color(0xFF4CAF50)
        porcentagem >= 0.50f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    //Animação da cor
    val corBarraAnimada by animateColorAsState(targetValue = corAlvo, label = "animacaoCor")
    // Se estiver vazio enche a barra de cinza se não, usa a porcentagem real
    val progressoAnimado by animateFloatAsState(targetValue = if (isVazio) 1f else porcentagem, label = "progresso")

    val textoStatus = if (isVazio) {
        "Nenhum vídeo recente analisado"
    } else {
        "${(porcentagem * 100).toInt()}% do conteúdo está adequado"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Nível de Segurança", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Barra de progresso com a cor animada
            LinearProgressIndicator(
                progress = { progressoAnimado },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
                color = corBarraAnimada, // <--- Usando a cor que muda suavemente
                trackColor = Color(0xFFEEEEEE)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = textoStatus,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isVazio) Color.Gray else Color.DarkGray
            )
        }
    }
}

@Composable
fun VideoLogCard(video: LogVideoApp) {
    val corFundo = if (video.seguro) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val corIcone = if (video.seguro) Color(0xFF4CAF50) else Color(0xFFF44336)
    val icone = if (video.seguro) Icons.Default.CheckCircle else Icons.Default.Warning
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { expandido = !expandido }
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            //Icone circular
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(corFundo),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = corIcone)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
            Column(modifier = Modifier.weight(1f).animateContentSize()) {
                Text(
                    text = video.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = if (expandido) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
                 Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.motivo,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = if (expandido) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                val formatador = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                Text(
                    text = formatador.format(Date(video.timestamp)),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EstadoVazio() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nenhum dado coletado do dispositivo",
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarModerno(apelido: String,onLimparClick: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("Histórico", fontWeight = FontWeight.Bold)
                Text(
                    if (apelido.isEmpty()) "Nenhum dispositivo" else "Monitorizando: $apelido",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1565C0),
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        actions = {
            Button(onClick = onLimparClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                Text("Limpar")
            }
        }
    )
}
