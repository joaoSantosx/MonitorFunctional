package com.jvf.monitorfunctional

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.tasks.await
import com.jvf.monitorfunctional.ui.PremiumActivity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
// ─── Activity ─────────────────────────────────────────────────────────────────
class ConfiguracoesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        val codigoMonitorado = prefs.getString("codigo_monitorado", "") ?: ""
        val planoAtual = prefs.getString("tipo_plano", "FREE") ?: "FREE"

        if (codigoMonitorado.isEmpty()) {
            Toast.makeText(this, "Nenhum dispositivo vinculado para configurar.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ConfiguracoesScreen(
                codigoFilho = codigoMonitorado,
                planoAtual = planoAtual,
                onVoltar = { finish() }
            )
        }
    }
}

// ─── Paleta ───────────────────────────────────────────────────────────────────
private object CfgColors {
    val azulPrimario   = Color(0xFF1565C0)
    val azulFundo      = Color(0xFFEFF6FF)
    val azulBorda      = Color(0xFFBFDBFE)
    val verdePrimario  = Color(0xFF16A34A)
    val verdeFundo     = Color(0xFFF0FDF4)
    val verdeBorda     = Color(0xFFBBF7D0)
    val verdeTexto     = Color(0xFF166534)
    val amberFundo     = Color(0xFFFFFBEB)
    val amberBorda     = Color(0xFFFDE68A)
    val amberTexto     = Color(0xFF92400E)
    val amberIcone     = Color(0xFFD97706)
    val roxoFundo      = Color(0xFFF3F0FF)
    val roxoIcone      = Color(0xFF7C3AED)
    val vermelhoFundo  = Color(0xFFFFF1F2)
    val vermelhoIcone  = Color(0xFFDC2626)
    val cinzaFundo     = Color(0xFFF0F4F8)
    val bordaCard      = Color(0xFFE2E8F0)
    val textoPrimario  = Color(0xFF1E293B)
    val textoMuted     = Color(0xFF94A3B8)
    val textoSecundario= Color(0xFF64748B)
    val branco         = Color.White

    val gradienteTopBar = Brush.linearGradient(
        listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
    )
}

// ─── Tela principal ───────────────────────────────────────────────────────────
@Composable
fun ConfiguracoesScreen(codigoFilho: String, planoAtual: String, onVoltar: () -> Unit) {
    val contexto = LocalContext.current
    val db = Firebase.firestore
    val isPremium = planoAtual == "PREMIUM"
    var carregando by remember { mutableStateOf(true) }

    // Estados — IA
    var nivelSelecionado by remember { mutableStateOf("ALTA") }

    // Estados — Notificações
    var notificacoesExpandido by remember { mutableStateOf(false) }
    var categoriasSalvas by remember { mutableStateOf(setOf("Todas as categorias")) }
    var categoriasEditando by remember { mutableStateOf(setOf("Todas as categorias")) }
    val listaCategorias = listOf(
        "Violência", "Adulto", "Educativo",
        "Entretenimento", "Outros",
        "Filtro Personalizado"
    )

    // Estados — Filtros personalizados
    var novaPalavra by remember { mutableStateOf("") }
    var palavrasMonitoradas by remember { mutableStateOf(listOf<String>()) }
    var novaPalavraPermitida by remember { mutableStateOf("") }
    var palavrasPermitidas by remember { mutableStateOf(listOf<String>()) }

    // Estados — PIN
    var mostrarDialogPin by remember { mutableStateOf(false) }
    var novoPinDigitado by remember { mutableStateOf("") }

    // Carrega dados do Firebase
    LaunchedEffect(codigoFilho) {
        try {
            val docRegra = db.collection("regras_parentais").document(codigoFilho).get().await()
            if (docRegra.exists()) {
                nivelSelecionado = docRegra.getString("nivel") ?: "ALTA"
                palavrasMonitoradas = (docRegra.get("palavras_monitoradas") as? List<*>)
                    ?.map { it.toString() } ?: emptyList()
                palavrasPermitidas = (docRegra.get("palavras_permitidas") as? List<*>)
                    ?.map { it.toString() } ?: emptyList()
            }
            val docNotif = db.collection("configuracoes_notificacao").document(codigoFilho).get().await()
            if (docNotif.exists()) {
                val salvas = (docNotif.get("categorias_permitidas") as? List<*>)
                    ?.map { it.toString() }?.toSet()
                if (!salvas.isNullOrEmpty()) {
                    categoriasSalvas = salvas
                    categoriasEditando = salvas
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            carregando = false
        }
    }

    // Funções de lógica
    val salvarRegraIA: (String) -> Unit = { novoNivel ->
        nivelSelecionado = novoNivel
        db.collection("regras_parentais").document(codigoFilho)
            .set(hashMapOf("nivel" to novoNivel), SetOptions.merge())
        Unit
    }

    fun alternarCategoria(opcao: String) {
        val novo = categoriasEditando.toMutableSet()
        if (opcao == "Todas as categorias") {
            novo.clear(); novo.add("Todas as categorias")
        } else {
            novo.remove("Todas as categorias")
            if (novo.contains(opcao)) {
                novo.remove(opcao)
                if (novo.isEmpty()) novo.add("Todas as categorias")
            } else {
                novo.add(opcao)
            }
        }
        categoriasEditando = novo
    }

    fun adicionarPalavra() {
        val termo = novaPalavra.trim()
        if (termo.isNotBlank() && !palavrasMonitoradas.map { it.lowercase() }.contains(termo.lowercase())) {
            val novaLista = palavrasMonitoradas + termo
            palavrasMonitoradas = novaLista
            db.collection("regras_parentais").document(codigoFilho)
                .set(hashMapOf("palavras_monitoradas" to novaLista), SetOptions.merge())
            novaPalavra = ""
            Toast.makeText(contexto, "Regra adicionada!", Toast.LENGTH_SHORT).show()
        }
    }

    fun removerPalavra(palavra: String) {
        val novaLista = palavrasMonitoradas.filter { it != palavra }
        palavrasMonitoradas = novaLista
        db.collection("regras_parentais").document(codigoFilho)
            .set(hashMapOf("palavras_monitoradas" to novaLista), SetOptions.merge())
    }

    fun adicionarPalavraPermitida() {
        val termo = novaPalavraPermitida.trim()
        if (termo.isNotBlank() && !palavrasPermitidas.map { it.lowercase() }.contains(termo.lowercase())) {
            val novaLista = palavrasPermitidas + termo
            palavrasPermitidas = novaLista
            db.collection("regras_parentais").document(codigoFilho)
                .set(hashMapOf("palavras_permitidas" to novaLista), SetOptions.merge())
            novaPalavraPermitida = ""
            Toast.makeText(contexto, "Exceção adicionada!", Toast.LENGTH_SHORT).show()
        }
    }

    fun removerPalavraPermitida(palavra: String) {
        val novaLista = palavrasPermitidas.filter { it != palavra }
        palavrasPermitidas = novaLista
        db.collection("regras_parentais").document(codigoFilho)
            .set(hashMapOf("palavras_permitidas" to novaLista), SetOptions.merge())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CfgColors.cinzaFundo)
    ) {
        CfgTopBar(onVoltar = onVoltar)

        if (carregando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CfgColors.azulPrimario)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                // ── Notificações ────────────────────────────────────────
                CfgSectionTitle("Notificações")
                SecaoNotificacoes(
                    expandido = notificacoesExpandido,
                    onToggle = { notificacoesExpandido = !notificacoesExpandido },
                    categoriasSalvas = categoriasSalvas,
                    categoriasEditando = categoriasEditando,
                    listaCategorias = listaCategorias,
                    isPremium = isPremium,
                    onAlternar = { alternarCategoria(it) },
                    onSalvar = {
                        categoriasSalvas = categoriasEditando
                        db.collection("configuracoes_notificacao").document(codigoFilho)
                            .set(hashMapOf("categorias_permitidas" to categoriasEditando.toList()))
                        notificacoesExpandido = false
                        Toast.makeText(contexto, "Filtros salvos!", Toast.LENGTH_SHORT).show()
                    },
                    onCancelar = {
                        categoriasEditando = categoriasSalvas
                        notificacoesExpandido = false
                    }
                )

                // ── Inteligência Artificial ──────────────────────────────
                CfgSectionTitle("Inteligência Artificial")
                SecaoNivelIA(
                    nivelSelecionado = nivelSelecionado,
                    onSelecionar = salvarRegraIA
                )

                // ── Filtros da Família ───────────────────────────────────
                CfgSectionTitle(
                    if (isPremium) "Filtros da Família" else "Filtros da Família · Premium"
                )

                if (isPremium) {
                    SecaoBlacklist(
                        palavras = palavrasMonitoradas,
                        novoValor = novaPalavra,
                        onNovoValorChange = { novaPalavra = it },
                        onAdicionar = { adicionarPalavra() },
                        onRemover = { removerPalavra(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SecaoWhitelist(
                        palavras = palavrasPermitidas,
                        novoValor = novaPalavraPermitida,
                        onNovoValorChange = { novaPalavraPermitida = it },
                        onAdicionar = { adicionarPalavraPermitida() },
                        onRemover = { removerPalavraPermitida(it) }
                    )
                } else {
                    BannerPremium(
                        onClick = {
                            contexto.startActivity(Intent(contexto, PremiumActivity::class.java))
                        }
                    )
                }

                // ── Segurança ────────────────────────────────────────────
                CfgSectionTitle("Segurança")
                SecaoPin(onAlterar = { mostrarDialogPin = true })

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog PIN
    if (mostrarDialogPin) {
        AlertDialog(
            onDismissRequest = { mostrarDialogPin = false },
            title = { Text("Alterar PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Digite uma nova senha (recomendado 4 números):",
                        fontSize = 14.sp,
                        color = CfgColors.textoSecundario
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = novoPinDigitado,
                        onValueChange = { novoPinDigitado = it },
                        label = { Text("Novo PIN") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = CfgColors.azulPrimario),
                    onClick = {
                        if (novoPinDigitado.isNotBlank()) {
                            db.collection("regras_parentais").document(codigoFilho)
                                .set(hashMapOf("pin_manutencao" to novoPinDigitado), SetOptions.merge())
                            mostrarDialogPin = false
                            novoPinDigitado = ""
                            Toast.makeText(contexto, "PIN atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogPin = false }) { Text("Cancelar") }
            }
        )
    }
}

// ─── TopBar ───────────────────────────────────────────────────────────────────
@Composable
private fun CfgTopBar(onVoltar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CfgColors.gradienteTopBar)
    ) {
        Box(modifier = Modifier.size(110.dp).offset(x = 280.dp, y = (-30).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
        Box(modifier = Modifier.size(60.dp).offset(x = 290.dp, y = 10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.04f)))

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("RESPONSÁVEL", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.2.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Configurações", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Personalize as regras de proteção", fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}

// ─── Título de seção ──────────────────────────────────────────────────────────
@Composable
private fun CfgSectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = CfgColors.textoMuted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

// ─── Card base ────────────────────────────────────────────────────────────────
@Composable
private fun CfgBaseCard(
    icone: ImageVector,
    iconeBg: Color,
    iconeColor: Color,
    titulo: String,
    subtitulo: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    expandido: Boolean = false,
    conteudoExpandido: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CfgColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CfgColors.bordaCard)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icone, contentDescription = null, tint = iconeColor, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CfgColors.textoPrimario)
                    Text(subtitulo, fontSize = 11.sp, color = CfgColors.textoMuted, modifier = Modifier.padding(top = 1.dp))
                }
                trailing?.invoke()
            }
            if (conteudoExpandido != null) {
                AnimatedVisibility(visible = expandido) {
                    Column {
                        HorizontalDivider(color = CfgColors.bordaCard, thickness = 0.5.dp)
                        conteudoExpandido()
                    }
                }
            }
        }
    }
}

// ─── Seção: Notificações ──────────────────────────────────────────────────────
@Composable
private fun SecaoNotificacoes(
    expandido: Boolean,
    onToggle: () -> Unit,
    categoriasSalvas: Set<String>,
    categoriasEditando: Set<String>,
    listaCategorias: List<String>,
    isPremium: Boolean,
    onAlternar: (String) -> Unit,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit
) {
    val contexto = LocalContext.current
    val resumo = if (categoriasSalvas.contains("Todas as categorias")) "Todas as categorias ativas"
    else "${categoriasSalvas.size} categorias selecionadas"

    CfgBaseCard(
        icone = Icons.Default.Notifications,
        iconeBg = CfgColors.azulFundo,
        iconeColor = CfgColors.azulPrimario,
        titulo = "Filtro de Notificações",
        subtitulo = resumo,
        expandido = expandido,
        onClick = onToggle,
        trailing = {
            Icon(
                imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CfgColors.textoMuted,
                modifier = Modifier.size(20.dp)
            )
        },
        conteudoExpandido = {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                // "Todas as categorias"
                ItemCheckboxModerno(
                    texto = "Todas as categorias",
                    marcado = categoriasEditando.contains("Todas as categorias"),
                    desativado = false,
                    onClick = { onAlternar("Todas as categorias") }
                )

                listaCategorias.forEach { cat ->
                    val bloqueado = cat == "Filtro Personalizado" && !isPremium

                    ItemCheckboxModerno(
                        texto = cat,
                        marcado = categoriasEditando.contains(cat) && !bloqueado,
                        desativado = bloqueado,
                        badgePro = bloqueado,
                        onClick = {
                            if (bloqueado) {
                                contexto.startActivity(Intent(contexto, PremiumActivity::class.java))
                            } else {
                                onAlternar(cat)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancelar) {
                        Text("Cancelar", color = CfgColors.textoMuted, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onSalvar,
                        colors = ButtonDefaults.buttonColors(containerColor = CfgColors.azulPrimario),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", fontSize = 13.sp)
                    }
                }
            }
        }
    )
}

// ─── Checkbox item ────────────────────────────────────────────────────────────
@Composable
private fun ItemCheckboxModerno(
    texto: String,
    marcado: Boolean,
    desativado: Boolean,
    badgePro: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true) { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = marcado,
            onCheckedChange = null,
            enabled = !desativado,
            colors = CheckboxDefaults.colors(checkedColor = CfgColors.azulPrimario)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = texto,
            fontSize = 13.sp,
            color = if (desativado) CfgColors.textoMuted else CfgColors.textoPrimario,
            fontWeight = if (marcado) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (badgePro) {
            Surface(shape = RoundedCornerShape(99.dp), color = CfgColors.amberBorda) {
                Text("PRO", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = CfgColors.amberTexto, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

// ─── Seção: Nível IA ──────────────────────────────────────────────────────────
@Composable
private fun SecaoNivelIA(nivelSelecionado: String, onSelecionar: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CfgColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CfgColors.bordaCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(CfgColors.roxoFundo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = CfgColors.roxoIcone, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Nível de Rigidez", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CfgColors.textoPrimario)
                    Text("Define o quão estrito o filtro é", fontSize = 11.sp, color = CfgColors.textoMuted, modifier = Modifier.padding(top = 1.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotaoNivel(
                    label = "Alta",
                    descricao = "Foco em conteúdo infantil",
                    selecionado = nivelSelecionado == "ALTA",
                    fundoCor = CfgColors.verdeFundo,
                    bordaCor = CfgColors.verdeBorda,
                    textoCor = CfgColors.verdePrimario,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelecionar("ALTA") }
                )
                BotaoNivel(
                    label = "Média",
                    descricao = "Ignora humor leve",
                    selecionado = nivelSelecionado == "MEDIA",
                    fundoCor = CfgColors.amberFundo,
                    bordaCor = CfgColors.amberBorda,
                    textoCor = CfgColors.amberIcone,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelecionar("MEDIA") }
                )
                BotaoNivel(
                    label = "Baixa",
                    descricao = "Só adulto e crimes",
                    selecionado = nivelSelecionado == "BAIXA",
                    fundoCor = CfgColors.vermelhoFundo,
                    bordaCor = Color(0xFFFECDD3),
                    textoCor = CfgColors.vermelhoIcone,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelecionar("BAIXA") }
                )
            }
        }
    }
}

@Composable
private fun BotaoNivel(
    label: String,
    descricao: String,
    selecionado: Boolean,
    fundoCor: Color,
    bordaCor: Color,
    textoCor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selecionado) fundoCor else CfgColors.cinzaFundo)
            .border(1.5.dp, if (selecionado) bordaCor else CfgColors.bordaCard, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selecionado) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = textoCor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(3.dp))
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selecionado) textoCor else CfgColors.textoMuted)
        Text(descricao, fontSize = 9.sp, color = CfgColors.textoMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

// ─── Seção: Blacklist ─────────────────────────────────────────────────────────
@Composable
private fun SecaoBlacklist(
    palavras: List<String>,
    novoValor: String,
    onNovoValorChange: (String) -> Unit,
    onAdicionar: () -> Unit,
    onRemover: (String) -> Unit
) {
    CfgBaseCard(
        icone = Icons.Default.Block,
        iconeBg = CfgColors.vermelhoFundo,
        iconeColor = CfgColors.vermelhoIcone,
        titulo = "Palavras Bloqueadas",
        subtitulo = if (palavras.isEmpty()) "Nenhuma regra adicionada" else "${palavras.size} ${if (palavras.size == 1) "regra" else "regras"} ativas",
        expandido = true,
        conteudoExpandido = {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    "Temas tratados como nocivos pela IA (ex: Câmera escondida, Susto, Armas).",
                    fontSize = 11.sp, color = CfgColors.textoSecundario, lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                CampoAdicionar(
                    valor = novoValor,
                    placeholder = "Ex: Câmera escondida",
                    corBotao = CfgColors.vermelhoIcone,
                    onValorChange = onNovoValorChange,
                    onAdicionar = onAdicionar
                )
                if (palavras.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        palavras.forEach { palavra ->
                            ChipItem(texto = palavra, fundoCor = Color(0xFFF1F5F9), textoCor = CfgColors.textoPrimario, iconeColor = CfgColors.textoMuted, onRemover = { onRemover(palavra) })
                        }
                    }
                }
            }
        }
    )
}

// ─── Seção: Whitelist ─────────────────────────────────────────────────────────
@Composable
private fun SecaoWhitelist(
    palavras: List<String>,
    novoValor: String,
    onNovoValorChange: (String) -> Unit,
    onAdicionar: () -> Unit,
    onRemover: (String) -> Unit
) {
    CfgBaseCard(
        icone = Icons.Default.CheckCircle,
        iconeBg = CfgColors.verdeFundo,
        iconeColor = CfgColors.verdePrimario,
        titulo = "Exceções Permitidas",
        subtitulo = if (palavras.isEmpty()) "Nenhuma exceção adicionada" else "${palavras.size} ${if (palavras.size == 1) "exceção" else "exceções"} ativas",
        expandido = true,
        conteudoExpandido = {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    "Palavras sempre liberadas pela IA, mesmo que pareçam suspeitas (ex: Minecraft, Roblox).",
                    fontSize = 11.sp, color = CfgColors.textoSecundario, lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                CampoAdicionar(
                    valor = novoValor,
                    placeholder = "Ex: Minecraft",
                    corBotao = CfgColors.verdePrimario,
                    onValorChange = onNovoValorChange,
                    onAdicionar = onAdicionar
                )
                if (palavras.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        palavras.forEach { palavra ->
                            ChipItem(texto = palavra, fundoCor = CfgColors.verdeFundo, textoCor = CfgColors.verdeTexto, iconeColor = CfgColors.verdePrimario, onRemover = { onRemover(palavra) })
                        }
                    }
                }
            }
        }
    )
}

// ─── Campo de adicionar ───────────────────────────────────────────────────────
@Composable
private fun CampoAdicionar(
    valor: String,
    placeholder: String,
    corBotao: Color,
    onValorChange: (String) -> Unit,
    onAdicionar: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(CfgColors.cinzaFundo)
                .border(1.dp, CfgColors.bordaCard, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = valor,
                onValueChange = onValorChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 13.sp, color = CfgColors.textoPrimario),
                cursorBrush = SolidColor(CfgColors.azulPrimario),
                singleLine = true,
                decorationBox = { inner ->
                    if (valor.isEmpty()) Text(placeholder, fontSize = 13.sp, color = CfgColors.textoMuted)
                    inner()
                }
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(corBotao)
                .clickable { onAdicionar() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = CfgColors.branco, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Chip ─────────────────────────────────────────────────────────────────────
@Composable
private fun ChipItem(
    texto: String,
    fundoCor: Color,
    textoCor: Color,
    iconeColor: Color,
    onRemover: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(fundoCor)
            .border(1.dp, CfgColors.bordaCard, RoundedCornerShape(99.dp))
            .clickable { onRemover() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(texto, fontSize = 12.sp, color = textoCor, fontWeight = FontWeight.Medium)
        Icon(Icons.Default.Close, contentDescription = "Remover", tint = iconeColor, modifier = Modifier.size(12.dp))
    }
}

// ─── Seção: PIN ───────────────────────────────────────────────────────────────
@Composable
private fun SecaoPin(onAlterar: () -> Unit) {
    CfgBaseCard(
        icone = Icons.Default.Lock,
        iconeBg = CfgColors.azulFundo,
        iconeColor = CfgColors.azulPrimario,
        titulo = "PIN de Manutenção",
        subtitulo = "Senha para desativar proteção no dispositivo",
        trailing = {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = CfgColors.azulFundo,
                modifier = Modifier
                    .border(1.dp, CfgColors.azulBorda, RoundedCornerShape(99.dp))
                    .clickable { onAlterar() }
            ) {
                Text("Alterar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CfgColors.azulPrimario, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }
    )
}

// ─── Banner Premium ───────────────────────────────────────────────────────────
@Composable
private fun BannerPremium(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CfgColors.amberFundo)
            .border(1.5.dp, CfgColors.amberBorda, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFEF3C7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = CfgColors.amberIcone, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Desbloqueie Filtros Específicos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CfgColors.amberTexto)
            Text("Assine o Premium para criar regras baseadas nos valores da sua família.", fontSize = 11.sp, color = Color(0xFF78350F), lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = CfgColors.amberIcone, modifier = Modifier.size(18.dp))
    }
}