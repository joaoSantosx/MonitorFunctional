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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import java.util.*

data class HistoricoPesquisa(val termo: String, val dataHora: Date)

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
            PesquisasScreen(
                codigoFilho = codigoMonitorado,
                onVoltar = { finish() }
            )
        }
    }
}

private object PesqColors {
    val roxoEscuro     = Color(0xFF4527A0)
    val roxoPrimario   = Color(0xFF673AB7)
    val roxoFundo      = Color(0xFFF3F0FF)
    val roxoIcone      = Color(0xFF7C3AED)
    val cinzaFundo     = Color(0xFFF0F4F8)
    val bordaCard      = Color(0xFFE2E8F0)
    val textoPrimario  = Color(0xFF1E293B)
    val textoMuted     = Color(0xFF94A3B8)
    val cinzaClaro     = Color(0xFFF1F5F9)
    val cinzaBadgeBg   = Color(0xFFF1F5F9)
    val cinzaBadgeText = Color(0xFF64748B)
    val branco         = Color.White

    val gradienteTopBar = Brush.linearGradient(
        listOf(Color(0xFF4527A0), Color(0xFF673AB7))
    )
}
private fun ehHoje(date: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date }
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun ehOntem(date: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date }
    val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun labelDia(date: Date): String {
    return when {
        ehHoje(date)   -> "Hoje · " + SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(date)
        ehOntem(date)  -> "Ontem · " + SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(date)
        else           -> SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(date)
    }
}

private fun badgeLabel(date: Date): String = when {
    ehHoje(date)  -> "Hoje"
    ehOntem(date) -> "Ontem"
    else          -> SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(date)
}

private sealed class PesquisaListItem {
    data class Separador(val label: String) : PesquisaListItem()
    data class Item(val pesquisa: HistoricoPesquisa) : PesquisaListItem()
}

private fun agruparPorDia(lista: List<HistoricoPesquisa>): List<PesquisaListItem> {
    val resultado = mutableListOf<PesquisaListItem>()
    var ultimoDia = ""
    lista.forEach { pesquisa ->
        val diaLabel = labelDia(pesquisa.dataHora)
        if (diaLabel != ultimoDia) {
            resultado.add(PesquisaListItem.Separador(diaLabel))
            ultimoDia = diaLabel
        }
        resultado.add(PesquisaListItem.Item(pesquisa))
    }
    return resultado
}

//Tela principal
@Composable
fun PesquisasScreen(codigoFilho: String, onVoltar: () -> Unit) {
    val db = Firebase.firestore
    var listaPesquisas by remember { mutableStateOf(listOf<HistoricoPesquisa>()) }
    var carregando by remember { mutableStateOf(true) }
    var filtro by remember { mutableStateOf("") }

    LaunchedEffect(codigoFilho) {
        db.collection("historico_pesquisas")
            .whereEqualTo("codigo_pareamento", codigoFilho)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) { carregando = false; return@addSnapshotListener }
                if (snapshot != null) {
                    listaPesquisas = snapshot.documents.mapNotNull { doc ->
                        val termo = doc.getString("termo") ?: return@mapNotNull null
                        val data  = doc.getDate("data_hora") ?: Date()
                        HistoricoPesquisa(termo, data)
                    }
                    carregando = false
                }
            }
    }

    // Stats (sem filtro)
    val totalHoje    = listaPesquisas.count { ehHoje(it.dataHora) }
    val ultimaBusca  = listaPesquisas.firstOrNull()?.dataHora
        ?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--"
    val termosUnicos = listaPesquisas.map { it.termo.lowercase().trim() }.toSet().size

    // Lista filtrada
    val listaFiltrada = remember(listaPesquisas, filtro) {
        val base = if (filtro.isBlank()) listaPesquisas
        else listaPesquisas.filter { it.termo.contains(filtro, ignoreCase = true) }
        agruparPorDia(base)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PesqColors.cinzaFundo)
    ) {
        PesquisasTopBar(
            totalHoje    = totalHoje,
            ultimaBusca  = ultimaBusca,
            termosUnicos = termosUnicos,
            carregando   = carregando,
            onVoltar     = onVoltar
        )

        Spacer(modifier = Modifier.height(8.dp))

        PesqSectionTitle("Histórico de buscas")

        // Barra de filtro local
        FiltroBarra(
            valor    = filtro,
            onChange = { filtro = it },
            onLimpar = { filtro = "" }
        )

        Spacer(modifier = Modifier.height(4.dp))

        when {
            carregando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PesqColors.roxoPrimario)
                }
            }
            listaFiltrada.isEmpty() -> {
                EstadoVazioPesquisas(filtroAtivo = filtro.isNotBlank())
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(listaFiltrada) { item ->
                        when (item) {
                            is PesquisaListItem.Separador -> SeparadorData(item.label)
                            is PesquisaListItem.Item      -> CardPesquisaModerno(item.pesquisa)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PesquisasTopBar(
    totalHoje: Int,
    ultimaBusca: String,
    termosUnicos: Int,
    carregando: Boolean,
    onVoltar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PesqColors.gradienteTopBar)
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
                        text = "MONITORAMENTO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pesquisas no YouTube",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Últimas 100 buscas registradas",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PesqStatPilula(
                    valor  = if (carregando) "..." else "$totalHoje",
                    label  = "Total hoje",
                    modifier = Modifier.weight(1f)
                )
                PesqStatPilula(
                    valor  = if (carregando) "..." else ultimaBusca,
                    label  = "Última busca",
                    modifier = Modifier.weight(1f)
                )
                PesqStatPilula(
                    valor  = if (carregando) "..." else "$termosUnicos",
                    label  = "Termos únicos",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PesqStatPilula(valor: String, label: String, modifier: Modifier = Modifier) {
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

//Barra de filtro
@Composable
private fun FiltroBarra(valor: String, onChange: (String) -> Unit, onLimpar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PesqColors.branco)
            .border(1.dp, PesqColors.bordaCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = PesqColors.textoMuted,
            modifier = Modifier.size(16.dp)
        )
        BasicTextField(
            value = valor,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontSize = 13.sp,
                color = PesqColors.textoPrimario
            ),
            cursorBrush = SolidColor(PesqColors.roxoPrimario),
            singleLine = true,
            decorationBox = { inner ->
                if (valor.isEmpty()) {
                    Text(
                        text = "Filtrar termos...",
                        fontSize = 13.sp,
                        color = PesqColors.textoMuted
                    )
                }
                inner()
            }
        )
        if (valor.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Limpar filtro",
                tint = PesqColors.textoMuted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onLimpar() }
            )
        }
    }
}


@Composable
private fun SeparadorData(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = PesqColors.bordaCard, thickness = 0.5.dp)
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = PesqColors.textoMuted
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = PesqColors.bordaCard, thickness = 0.5.dp)
    }
}

@Composable
fun CardPesquisaModerno(pesquisa: HistoricoPesquisa) {
    val isHoje  = ehHoje(pesquisa.dataHora)
    val isOntem = ehOntem(pesquisa.dataHora)

    val badgeBg   = if (isHoje) PesqColors.roxoFundo   else PesqColors.cinzaBadgeBg
    val badgeText = if (isHoje) PesqColors.roxoIcone   else PesqColors.cinzaBadgeText
    val badge     = badgeLabel(pesquisa.dataHora)

    val horaFormatada = SimpleDateFormat("HH:mm", Locale.getDefault()).format(pesquisa.dataHora)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PesqColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PesqColors.bordaCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PesqColors.roxoFundo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = PesqColors.roxoIcone,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pesquisa.termo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PesqColors.textoPrimario,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = horaFormatada,
                    fontSize = 10.sp,
                    color = PesqColors.textoMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Badge de dia
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = badgeBg
            ) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeText,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PesqSectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = PesqColors.textoMuted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

//Estado vazio
@Composable
private fun EstadoVazioPesquisas(filtroAtivo: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (filtroAtivo) "🔍" else "📭", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (filtroAtivo) "Nenhum resultado para \"\"" else "Nenhuma pesquisa registrada ainda",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PesqColors.textoMuted
            )
            Text(
                text = if (filtroAtivo) "Tente outro termo de busca" else "As pesquisas do YouTube aparecerão aqui",
                fontSize = 12.sp,
                color = PesqColors.textoMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}