package com.jvf.monitorfunctional

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.firestore
import java.util.UUID
import androidx.core.content.edit
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.Crossfade
import android.util.Log
import com.google.firebase.Firebase

class DependenteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val codigoFilho = obterOuGerarCodigoFilho()

        registrarCodigoNaNuvem(codigoFilho)

        setContent {
            DependenteFluxo(
                codigo = codigoFilho,
                onAbrirConfiguracoes = {
                    Toast.makeText(
                        this,
                        "Procure por 'Monitor Aware Kids' e ative.",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }
    }

    private fun obterOuGerarCodigoFilho(): String {
        val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        var codigo = prefs.getString("codigo_filho", null)
        if (codigo == null) {
            codigo = UUID.randomUUID().toString().substring(0, 6).uppercase()
            prefs.edit { putString("codigo_filho", codigo) }
        }
        return codigo
    }
}

private fun registrarCodigoNaNuvem(codigo: String){
    val bancoDeDados = Firebase.firestore

    val dadosDispositivo = hashMapOf(
        "status" to "ativo",
        "criado_em" to java.util.Date()
    )
    bancoDeDados.collection("dispositivos_ativos")
        .document(codigo)
        .set(dadosDispositivo)
        .addOnSuccessListener{
            Log.d("Aware Kids", "Sucesso! Código $codigo registrado na nuvem.")
        }
        .addOnFailureListener { e ->
            Log.e("Aware Kids", "Falha ao registrar código na nuvem", e)
        }
}


// ─── Paleta ───────────────────────────────────────────────────────────────────
private object DepColors {
    val verdeEscuro    = Color(0xFF14532D)
    val verdePrimario  = Color(0xFF16A34A)
    val verdeFundo     = Color(0xFFF0FDF4)
    val verdeBorda     = Color(0xFFBBF7D0)
    val verdeTexto     = Color(0xFF166534)
    val verdeIcone     = Color(0xFF16A34A)

    val azulPrimario   = Color(0xFF1565C0)
    val azulFundo      = Color(0xFFEFF6FF)
    val azulBorda      = Color(0xFFBFDBFE)

    val vermelhoPerigo = Color(0xFFDC2626)
    val vermelhoFundo  = Color(0xFFFFF1F2)

    val cinzaFundo     = Color(0xFFF0F4F8)
    val bordaCard      = Color(0xFFE2E8F0)
    val textoPrimario  = Color(0xFF1E293B)
    val textoMuted     = Color(0xFF94A3B8)
    val textoSecundario= Color(0xFF64748B)
    val branco         = Color.White

    val gradienteHero = Brush.linearGradient(
        listOf(Color(0xFF14532D), Color(0xFF16A34A))
    )
    val gradienteBotao = Brush.horizontalGradient(
        listOf(Color(0xFF14532D), Color(0xFF16A34A))
    )
}

//Tela principal
@Composable
fun DependenteFluxo(
    codigo: String,
    onAbrirConfiguracoes: () -> Unit
) {
    val contexto = LocalContext.current
    val prefs = contexto.getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
    var termoAceito by remember {
        mutableStateOf(
            prefs.getBoolean(
                "termo_acessibilidade_aceito",
                false
            )
        )
    }

    Crossfade(targetState = termoAceito, label = "animacao_telas_dependente") { aceito ->
        if (!aceito) {
            ProminentDisclosureScreen(
                onAcceptClick = {
                    prefs.edit { putBoolean("termo_acessibilidade_aceito", true) }
                    termoAceito = true
                    onAbrirConfiguracoes() // Leva para as configurações do Android após aceitar
                }
            )
        } else {
            DependenteScreen(
                codigo = codigo,
                onAbrirConfiguracoes = onAbrirConfiguracoes
            )
        }
    }
}
@Composable
fun ProminentDisclosureScreen(
    onAcceptClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DepColors.cinzaFundo)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(DepColors.verdeFundo),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = DepColors.verdePrimario,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Estamos quase lá!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DepColors.textoPrimario,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Esta é a parte mais importante para a proteção do seu filho.",
            fontSize = 16.sp,
            color = DepColors.verdeTexto,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DepColors.branco),
            border = androidx.compose.foundation.BorderStroke(1.dp, DepColors.verdeBorda)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DivulgacaoItem(
                    titulo = "O que monitoramos?",
                    descricao = "O Aware Kids utiliza a permissão de Acessibilidade para ler os títulos dos vídeos e os termos pesquisados dentro do aplicativo do YouTube."
                )
                Spacer(modifier = Modifier.height(16.dp))
                DivulgacaoItem(
                    titulo = "Por que precisamos disso?",
                    descricao = "Para que nossa Inteligência Artificial possa analisar o conteúdo em tempo real e alertar você caso seu filho encontre algo inadequado."
                )
                Spacer(modifier = Modifier.height(16.dp))
                DivulgacaoItem(
                    titulo = "Sua privacidade é prioridade",
                    descricao = "Esses dados são usados exclusivamente para a proteção parental. Eles são criptografados, não são compartilhados com terceiros e nunca serão usados para fins publicitários."
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAcceptClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DepColors.verdePrimario)
        ) {
            Text("Entendi e desejo ativar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DepColors.branco)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DivulgacaoItem(titulo: String, descricao: String) {
    Column {
        Text(
            text = titulo,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DepColors.textoPrimario
        )
        Text(
            text = descricao,
            fontSize = 13.sp,
            color = DepColors.textoMuted,
            lineHeight = 18.sp
        )
    }
}

//Painel do Dependente
@Composable
fun DependenteScreen(
    codigo: String,
    onAbrirConfiguracoes: () -> Unit
) {
    val contexto = LocalContext.current
    val prefs = contexto.getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
    var protecaoAtiva by remember { mutableStateOf(prefs.getBoolean("protecao_ativa", true)) }
    var mostrarDialogSenha by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DepColors.cinzaFundo)
            .verticalScroll(rememberScrollState())
    ) {
        HeroVerde(protecaoAtiva = protecaoAtiva)

        Spacer(modifier = Modifier.height(4.dp))

        SectionTitle("Código de vinculação")
        CodigoCard(codigo = codigo)

        InfoCard(
            texto = "O responsável deve inserir este código no aplicativo dele para começar o monitoramento."
        )

        SectionTitle("Configurações")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DepColors.gradienteBotao)
                .clickable { onAbrirConfiguracoes() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = DepColors.branco,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ativar Monitoramento",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DepColors.branco
                )
            }
        }

        BotaoManutencaoModerno(
            protecaoAtiva = protecaoAtiva,
            onClicar = {
                if (protecaoAtiva) {
                    mostrarDialogSenha = true
                } else {
                    prefs.edit { putBoolean("protecao_ativa", true) }
                    protecaoAtiva = true
                    Toast.makeText(contexto, "Proteção Reativada!", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (mostrarDialogSenha) {
        DialogPin(
            codigoFilho = codigo,
            onSucesso = {
                prefs.edit { putBoolean("protecao_ativa", false) }
                protecaoAtiva = false
                mostrarDialogSenha = false
                Toast.makeText(contexto, "Configurações Liberadas!", Toast.LENGTH_LONG).show()
            },
            onDismiss = { mostrarDialogSenha = false }
        )
    }
}


@Composable
private fun HeroVerde(protecaoAtiva: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulso")
    val escala by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "escala"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DepColors.gradienteHero)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 250.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(x = (-20).dp, y = 80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 44.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DISPOSITIVO PROTEGIDO",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Modo Dependente",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "Este aparelho está sendo monitorado\npara a sua segurança.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(99.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(if (protecaoAtiva) escala else 1f)
                            .clip(CircleShape)
                            .background(
                                if (protecaoAtiva) Color(0xFF4ADE80) else Color(0xFFFCA5A5)
                            )
                    )
                    Text(
                        text = if (protecaoAtiva) "Proteção ativa" else "Proteção desativada",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CodigoCard(codigo: String) {
    val contexto = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DepColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, DepColors.verdeBorda)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SEU CÓDIGO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = DepColors.verdeIcone,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = codigo,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 10.sp,
                color = DepColors.azulPrimario,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Mostre este código ao responsável para vincular o monitoramento.",
                fontSize = 11.sp,
                color = DepColors.textoMuted,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = DepColors.azulFundo,
                modifier = Modifier
                    .border(1.dp, DepColors.azulBorda, RoundedCornerShape(99.dp))
                    .clickable {
                        val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("codigo_filho", codigo))
                        Toast.makeText(contexto, "Código copiado!", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar",
                        tint = DepColors.azulPrimario,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Copiar código",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DepColors.azulPrimario
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DepColors.verdeFundo)
            .border(1.dp, DepColors.verdeBorda, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = DepColors.verdeIcone,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 1.dp)
        )
        Text(
            text = texto,
            fontSize = 11.sp,
            color = DepColors.verdeTexto,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun BotaoManutencaoModerno(
    protecaoAtiva: Boolean,
    onClicar: () -> Unit
) {
    val bordaCor  = if (protecaoAtiva) DepColors.bordaCard else DepColors.vermelhoFundo
    val iconeCor  = if (protecaoAtiva) DepColors.textoMuted else DepColors.vermelhoPerigo
    val textoCor  = if (protecaoAtiva) DepColors.textoSecundario else DepColors.vermelhoPerigo
    val texto     = if (protecaoAtiva) "Desativar Proteção (Requer PIN)" else "Proteção Desativada — Trancar"
    val icone     = if (protecaoAtiva) Icons.Default.Lock else Icons.Default.LockOpen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DepColors.branco)
            .border(1.dp, bordaCor, RoundedCornerShape(14.dp))
            .clickable { onClicar() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = iconeCor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = texto,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textoCor
        )
    }
}
@Composable
private fun SectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = DepColors.textoMuted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}
@Composable
private fun DialogPin(
    codigoFilho: String,
    onSucesso: () -> Unit,
    onDismiss: () -> Unit
) {
    val contexto = LocalContext.current
    var senhaDigitada by remember { mutableStateOf("") }
    var validandoOnline by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!validandoOnline) onDismiss() },
        title = { Text("Área do Responsável", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Digite o PIN mestre para liberar o acesso às configurações do Android.",
                    fontSize = 14.sp,
                    color = DepColors.textoSecundario
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = senhaDigitada,
                    onValueChange = { senhaDigitada = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    enabled = !validandoOnline
                )
                if (validandoOnline) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = DepColors.verdePrimario
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !validandoOnline && senhaDigitada.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DepColors.verdePrimario),
                onClick = {
                    validandoOnline = true
                    com.google.firebase.Firebase.firestore
                        .collection("regras_parentais")
                        .document(codigoFilho)
                        .get()
                        .addOnSuccessListener { doc ->
                            validandoOnline = false
                            val pinCorreto = if (doc.exists() && doc.getString("pin_manutencao") != null)
                                doc.getString("pin_manutencao")
                            else "1234"

                            if (senhaDigitada == pinCorreto) {
                                onSucesso()
                            } else {
                                Toast.makeText(contexto, "Senha Incorreta", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            validandoOnline = false
                            Toast.makeText(contexto, "Erro de rede. Verifique a internet.", Toast.LENGTH_SHORT).show()
                        }
                }
            ) { Text("Destravar") }
        },
        dismissButton = {
            TextButton(
                enabled = !validandoOnline,
                onClick = onDismiss
            ) { Text("Cancelar") }
        }
    )
}