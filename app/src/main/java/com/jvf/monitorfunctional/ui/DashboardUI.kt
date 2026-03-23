package com.jvf.monitorfunctional.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
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
import android.widget.Toast
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.animateColorAsState


private object DashColors {
    val azulEscuro     = Color(0xFF0D47A1)
    val azulPrimario   = Color(0xFF1565C0)
    val verdeSafe      = Color(0xFF16A34A)
    val verdeFundo     = Color(0xFFF0FDF4)
    val vermelhoPerigo = Color(0xFFDC2626)
    val vermelhoFundo  = Color(0xFFFFF1F2)
    val cinzaFundo     = Color(0xFFF0F4F8)
    val cinzaClaro     = Color(0xFFCBD5E1)
    val cinzaMedio     = Color(0xFF94A3B8)
    val cinzaEscuro    = Color(0xFF64748B)
    val textoPrimario  = Color(0xFF1E293B)
    val branco         = Color.White

    val gradienteTopBar = Brush.linearGradient(
        colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
    )
    val gradienteVerde = Brush.horizontalGradient(
        colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A))
    )
    val gradienteAmarelo = Brush.horizontalGradient(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    )
    val gradienteVermelho = Brush.horizontalGradient(
        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    apelido: String,
    planoAtual: String,
    listaVideos: List<LogVideoApp>,
    onLimparClick: () -> Unit,
    onFiltrarDatas: (Long?, Long?) -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val isPremium = planoAtual == "PREMIUM"
    var mostrarCalendario by remember { mutableStateOf(false) }
    var isFiltroAtivo by remember { mutableStateOf(false) }
    var textoPeriodo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DashColors.cinzaFundo)
    ) {
        Box {
            TopBarModerna(
                apelido = apelido,
                isFiltroAtivo = isFiltroAtivo,
                onLimparClick = onLimparClick,
                onCalendarioClick = {
                    if (isPremium) {
                        mostrarCalendario = true
                    } else {
                        contexto.startActivity(
                            android.content.Intent(contexto, PremiumActivity::class.java)
                        )
                    }
                },
                onLimparFiltro = {
                    isFiltroAtivo = false
                    textoPeriodo = ""
                    onFiltrarDatas(null, null)
                }
            )
            Box(modifier = Modifier.padding(top = 88.dp)) {
                ScoreCard(listaVideos = listaVideos)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        //Tag de período ativo
        if (isFiltroAtivo && textoPeriodo.isNotEmpty()) {
            PeriodoTag(
                texto = textoPeriodo,
                onLimpar = {
                    isFiltroAtivo = false
                    textoPeriodo = ""
                    onFiltrarDatas(null, null)
                }
            )
        }

        //Lista de Vídeos
        SectionTitle(titulo = "Recentes")

        if (listaVideos.isEmpty()) {
            EstadoVazio()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listaVideos) { video ->
                    VideoLogCardModerno(video)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    if (mostrarCalendario) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    val inicioMillis = dateRangePickerState.selectedStartDateMillis
                    val fimMillis = dateRangePickerState.selectedEndDateMillis
                    if (inicioMillis != null && fimMillis != null) {
                        val fimDoDia = fimMillis + 86399999L
                        isFiltroAtivo = true
                        val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
                        textoPeriodo = "${fmt.format(Date(inicioMillis))} – ${fmt.format(Date(fimMillis))}"
                        onFiltrarDatas(inicioMillis, fimDoDia)
                        mostrarCalendario = false
                    } else {
                        Toast.makeText(contexto, "Selecione o início e o fim", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                title = { Text("Selecione o período", modifier = Modifier.padding(16.dp)) },
                headline = {
                    Text(
                        "Data de Início — Data de Término",
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                },
                showModeToggle = false
            )
        }
    }
}

//calendário
@Composable
private fun TopBarModerna(
    apelido: String,
    isFiltroAtivo: Boolean,
    onLimparClick: () -> Unit,
    onCalendarioClick: () -> Unit,
    onLimparFiltro: () -> Unit
) {
    val corIconeCalendario by animateColorAsState(
        targetValue = if (isFiltroAtivo) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.75f),
        animationSpec = tween(300),
        label = "corCalendario"
    )
    val fundoIconeCalendario by animateColorAsState(
        targetValue = if (isFiltroAtivo) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(300),
        label = "fundoCalendario"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(DashColors.gradienteTopBar)
    ) {
        //decorativos
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Textos
            Column {
                Text(
                    text = "DASHBOARD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.55f),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Histórico",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (apelido.isEmpty()) "Nenhum dispositivo" else "Monitorizando: $apelido",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            // Botões
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Icone de calendário
                Box {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(fundoIconeCalendario)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = if (isFiltroAtivo) 0.35f else 0.18f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (isFiltroAtivo) onLimparFiltro() else onCalendarioClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFiltroAtivo) Icons.Default.Close else Icons.Default.DateRange,
                            contentDescription = if (isFiltroAtivo) "Limpar filtro" else "Filtrar por período",
                            tint = corIconeCalendario,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    //filtro ativo
                    if (isFiltroAtivo) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .border(1.5.dp, DashColors.azulPrimario, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                // Botão Limpar
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .clickable { onLimparClick() }
                ) {
                    Text(
                        text = "Limpar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun PeriodoTag(texto: String, onLimpar: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = Color(0xFFEFF6FF),
            modifier = Modifier.border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(99.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = DashColors.azulPrimario,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = texto,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DashColors.azulPrimario
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpar filtro",
                    tint = DashColors.cinzaMedio,
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { onLimpar() }
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(listaVideos: List<LogVideoApp>) {
    val total = listaVideos.size
    val seguros = listaVideos.count { it.seguro }
    val isVazio = total == 0
    val porcentagem = if (!isVazio) seguros.toFloat() / total.toFloat() else 0f
    val pct = (porcentagem * 100).toInt()

    val corGradiente = when {
        isVazio              -> Brush.horizontalGradient(listOf(DashColors.cinzaClaro, DashColors.cinzaMedio))
        porcentagem >= 0.70f -> DashColors.gradienteVerde
        porcentagem >= 0.50f -> DashColors.gradienteAmarelo
        else                 -> DashColors.gradienteVermelho
    }
    val corNumero = when {
        isVazio              -> DashColors.cinzaMedio
        porcentagem >= 0.70f -> DashColors.verdeSafe
        porcentagem >= 0.50f -> Color(0xFFD97706)
        else                 -> DashColors.vermelhoPerigo
    }
    val textoStatus = when {
        isVazio              -> "Nenhum vídeo analisado"
        porcentagem >= 0.70f -> "Adequado — bom desempenho"
        porcentagem >= 0.50f -> "Atenção — verifique os itens"
        else                 -> "Risco — muitos vídeos sinalizados"
    }

    val progressoAnimado by animateFloatAsState(
        targetValue = if (isVazio) 1f else porcentagem,
        animationSpec = tween(800),
        label = "progresso"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DashColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Nível de Segurança",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DashColors.cinzaMedio
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (isVazio) "--" else "$pct",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = corNumero,
                            lineHeight = 40.sp
                        )
                        if (!isVazio) {
                            Text(
                                text = "%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashColors.cinzaMedio,
                                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                            )
                        }
                    }
                    Text(text = textoStatus, fontSize = 12.sp, color = DashColors.cinzaEscuro)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$seguros de $total",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashColors.textoPrimario
                    )
                    Text(text = "vídeos seguros", fontSize = 10.sp, color = DashColors.cinzaMedio)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressoAnimado)
                        .clip(RoundedCornerShape(99.dp))
                        .background(corGradiente)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPilula(
                    label = "Seguros",
                    valor = "$seguros",
                    corFundo = DashColors.verdeFundo,
                    corTexto = DashColors.verdeSafe,
                    corPonto = DashColors.verdeSafe,
                    modifier = Modifier.weight(1f)
                )
                StatPilula(
                    label = "Sinalizados",
                    valor = "${total - seguros}",
                    corFundo = DashColors.vermelhoFundo,
                    corTexto = DashColors.vermelhoPerigo,
                    corPonto = DashColors.vermelhoPerigo,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatPilula(
    label: String,
    valor: String,
    corFundo: Color,
    corTexto: Color,
    corPonto: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(corFundo)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(corPonto)
        )
        Text(text = label, fontSize = 11.sp, color = DashColors.cinzaEscuro, modifier = Modifier.weight(1f))
        Text(text = valor, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = corTexto)
    }
}

// Titulo de seção
@Composable
private fun SectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = DashColors.cinzaMedio,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

//Card de Video
@Composable
fun VideoLogCardModerno(video: LogVideoApp) {
    val isSafe = video.seguro
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DashColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSafe) DashColors.verdeFundo else DashColors.vermelhoFundo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSafe) DashColors.verdeSafe else DashColors.vermelhoPerigo,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            ) {
                Text(
                    text = video.titulo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DashColors.textoPrimario,
                    maxLines = if (expandido) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.motivo,
                    fontSize = 11.sp,
                    color = DashColors.cinzaMedio,
                    maxLines = if (expandido) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault())
                        .format(Date(video.timestamp)),
                    fontSize = 10.sp,
                    color = DashColors.cinzaClaro,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (isSafe) DashColors.verdeFundo else DashColors.vermelhoFundo
            ) {
                Text(
                    text = if (isSafe) "Seguro" else "Sinalizado",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSafe) DashColors.verdeSafe else DashColors.vermelhoPerigo,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📭", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Nenhum vídeo analisado ainda",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DashColors.cinzaEscuro
            )
            Text(
                text = "Os vídeos monitorados aparecerão aqui",
                fontSize = 12.sp,
                color = DashColors.cinzaMedio,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}