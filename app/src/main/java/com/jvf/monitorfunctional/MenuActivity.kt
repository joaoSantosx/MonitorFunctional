package com.jvf.monitorfunctional

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import com.jvf.monitorfunctional.ui.PremiumActivity

private object MenuColors {
    val azulPrimario = Color(0xFF1565C0)
    val cinzaFundo = Color(0xFFF0F4F8)
    val branco = Color.White

    val gradienteTopBar = Brush.linearGradient(
        colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
    )
    val azulIconeBg = Color(0xFFEFF6FF)
    val azulIcone = Color(0xFF1565C0)
    val tealIconeBg = Color(0xFFE6FFF9)
    val tealIcone = Color(0xFF0D9488)
    val roxoIconeBg = Color(0xFFF3F0FF)
    val roxoIcone = Color(0xFF7C3AED)
    val verdeIconeBg = Color(0xFFF0FDF4)
    val verdeIcone = Color(0xFF16A34A)
    val cinzaIconeBg = Color(0xFFF8FAFC)
    val cinzaIcone = Color(0xFF94A3B8)

    val laranjaIconeBg = Color(0xFFFFF7ED)
    val laranjaIcone = Color(0xFFEA580C)

    // Premium
    val amareloFundo = Color(0xFFFFFBEB)
    val amareloBorda = Color(0xFFFDE68A)
    val amareloTexto = Color(0xFF92400E)

    val textoPrimario = Color(0xFF1E293B)
    val textoMuted = Color(0xFF94A3B8)
    val bordaCard = Color(0xFFE2E8F0)
    val chevronCor = Color(0xFFCBD5E1)
} // ⬅️ A CHAVE DE FECHAMENTO FOI ADICIONADA AQUI

class MenuActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var apelidoState = mutableStateOf("")
    private var planoState = mutableStateOf("FREE")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Você não receberá alertas de vídeos perigosos.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        pedirPermissaoDeNotificacao()

        setContent {
            MenuScreen(
                apelidoAtual = apelidoState.value,
                planoAtual = planoState.value,
                onVerRelatorio = {
                    if (apelidoState.value.isNotEmpty()) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                    } else {
                        Toast.makeText(this, "Nenhum dispositivo vinculado ainda!", Toast.LENGTH_SHORT).show()
                    }
                },
                onVerScoreSemanal = {
                    if (apelidoState.value.isNotEmpty()) {
                        startActivity(Intent(this, ScoreActivity::class.java))
                        Toast.makeText(this, "Abrindo Score da Semana...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Nenhum dispositivo vinculado ainda!", Toast.LENGTH_SHORT).show()
                    }
                },
                onVerGrafico = {
                    if (apelidoState.value.isEmpty()) {
                        Toast.makeText(this, "Nenhum dispositivo vinculado ainda!", Toast.LENGTH_SHORT).show()
                    } else if (planoState.value != "PREMIUM") {
                        startActivity(Intent(this, PremiumActivity::class.java))
                    } else {
                        startActivity(Intent(this, RelatoriosActivity::class.java))
                    }
                },
                onVerPesquisas = {
                    if (apelidoState.value.isNotEmpty()) {
                        startActivity(Intent(this, PesquisasActivity::class.java))
                    } else {
                        Toast.makeText(this, "Nenhum dispositivo vinculado ainda!", Toast.LENGTH_SHORT).show()
                    }
                },
                onVincularNovo = { solicitarCodigoDoFilho() },
                onConfiguracoes = {
                    startActivity(Intent(this, ConfiguracoesActivity::class.java))
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        apelidoState.value = prefs.getString("apelido_monitorado", "") ?: ""
        planoState.value = prefs.getString("tipo_plano", "FREE") ?: "FREE"
        val codigoMonitorado = prefs.getString("codigo_monitorado", "") ?: ""
        observarPlanoNaNuvem(codigoMonitorado)
    }

    private fun solicitarCodigoDoFilho() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val inputCodigo = EditText(this).apply { hint = "Código de 6 dígitos (Ex: A7F9B2)" }
        val inputApelido = EditText(this).apply { hint = "Apelido (Ex: Celular do Lucas)" }
        layout.addView(inputCodigo)
        layout.addView(inputApelido)

        AlertDialog.Builder(this)
            .setTitle("Vincular Dispositivo")
            .setMessage("Insira o código do dependente e escolha um apelido:")
            .setView(layout)
            .setPositiveButton("Vincular", null)
            .setNegativeButton("Cancelar", null)
            .create()
            .apply {
                setOnShowListener { dialog ->
                    val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                    button.setOnClickListener {
                        val codigo = inputCodigo.text.toString().trim().uppercase()
                        val apelido = inputApelido.text.toString().trim()

                        if (codigo.isNotEmpty() && apelido.isNotEmpty()) {

                            button.isEnabled = false
                            button.text = "Validando..."

                            Firebase.firestore.collection("dispositivos_ativos")
                                .document(codigo)
                                .get()
                                .addOnSuccessListener { documento ->
                                    if (documento.exists()) {
                                        prefs.edit {
                                            putString("codigo_monitorado", codigo)
                                            putString("apelido_monitorado", apelido)
                                        }
                                        apelidoState.value = apelido

                                        Firebase.messaging.subscribeToTopic("alerta_$codigo")
                                            .addOnCompleteListener { task ->
                                                val msg = if (task.isSuccessful) "Dispositivo vinculado e validado com sucesso!"
                                                else "Vinculado, mas falha ao ativar notificações."
                                                Toast.makeText(this@MenuActivity, msg, Toast.LENGTH_SHORT).show()
                                            }

                                        dialog.dismiss()
                                    } else {
                                        Toast.makeText(this@MenuActivity, "Código inválido! Verifique o celular do dependente.", Toast.LENGTH_LONG).show()
                                        button.isEnabled = true
                                        button.text = "Vincular"
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@MenuActivity, "Erro ao conectar com o servidor.", Toast.LENGTH_SHORT).show()
                                    button.isEnabled = true
                                    button.text = "Vincular"
                                }

                        } else {
                            Toast.makeText(this@MenuActivity, "Preencha os dois campos!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun observarPlanoNaNuvem(codigoFilho: String) {
        if (codigoFilho.isEmpty()) return
        Firebase.firestore.collection("regras_parentais").document(codigoFilho)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    Log.e("Aware Kids", "Erro ao ouvir plano", erro)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val planoNuvem = snapshot.getString("plano") ?: "FREE"
                    planoState.value = planoNuvem
                    prefs.edit().putString("tipo_plano", planoNuvem).apply()
                    Log.i("Aware Kids", "Sincronizado: O plano atual é $planoNuvem")
                }
            }
    }
}

//Tela
@Composable
fun MenuScreen(
    apelidoAtual: String,
    planoAtual: String,
    onVerRelatorio: () -> Unit,
    onVerScoreSemanal: () -> Unit,
    onVerGrafico: () -> Unit,
    onVerPesquisas: () -> Unit,
    onVincularNovo: () -> Unit,
    onConfiguracoes: () -> Unit
) {
    val isPremium = planoAtual == "PREMIUM"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MenuColors.cinzaFundo)
            .verticalScroll(rememberScrollState())
    ) {
        MenuTopBar(
            apelido = apelidoAtual,
            onConfiguracoes = onConfiguracoes
        )

        Spacer(modifier = Modifier.height(8.dp))

        //Monitoramento
        MenuSectionTitle("Monitoramento")

        MenuCard(
            iconeBg = MenuColors.azulIconeBg,
            iconeColor = MenuColors.azulIcone,
            icone = Icons.Default.Description,
            titulo = "Relatório de Vídeos",
            subtitulo = "Histórico de conteúdo assistido",
            onClick = onVerRelatorio
        )

        MenuCard(
            iconeBg = MenuColors.laranjaIconeBg,
            iconeColor = MenuColors.laranjaIcone,
            icone = Icons.Default.PieChart,
            titulo = "Score Semanal",
            subtitulo = "Resumo percentual de segurança",
            onClick = onVerScoreSemanal
        )

        MenuCardPremium(
            isPremium = isPremium,
            titulo = "Gráfico de Horários",
            subtitulo = if (isPremium) "Descubra os picos de uso do app" else "Desbloqueie análises de horário",
            onClick = onVerGrafico
        )

        MenuCard(
            iconeBg = MenuColors.roxoIconeBg,
            iconeColor = MenuColors.roxoIcone,
            icone = Icons.Default.Search,
            titulo = "Pesquisas no YouTube",
            subtitulo = "Termos buscados pelo monitorado",
            onClick = onVerPesquisas
        )

        Spacer(modifier = Modifier.height(4.dp))

        //Dispositivos
        MenuSectionTitle("Dispositivos")

        MenuCard(
            iconeBg = MenuColors.verdeIconeBg,
            iconeColor = MenuColors.verdeIcone,
            icone = Icons.Default.Add,
            titulo = "Vincular Dispositivo",
            subtitulo = "Adicionar celular de um filho(a)",
            onClick = onVincularNovo
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

//TopBar
@Composable
private fun MenuTopBar(apelido: String, onConfiguracoes: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(MenuColors.gradienteTopBar)
    ) {
        // Círculos decorativos
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
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "PAINEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Olá, Responsável",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tudo sob controle hoje?",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                // Botão de configurações
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .clickable { onConfiguracoes() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            DevicePill(apelido = apelido)
        }
    }
}

@Composable
private fun DevicePill(apelido: String) {
    val temDispositivo = apelido.isNotEmpty()

    val infiniteTransition = rememberInfiniteTransition(label = "pulso")
    val escala by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    Surface(
        shape = RoundedCornerShape(99.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(99.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(if (temDispositivo) escala else 1f)
                    .clip(CircleShape)
                    .background(
                        if (temDispositivo) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.35f)
                    )
            )
            Text(
                text = if (temDispositivo) apelido else "Nenhum dispositivo vinculado",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = if (temDispositivo) 1f else 0.6f)
            )
        }
    }
}

@Composable
private fun MenuSectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MenuColors.textoMuted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun MenuCard(
    iconeBg: Color,
    iconeColor: Color,
    icone: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MenuColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MenuColors.bordaCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ícone em caixinha
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = iconeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MenuColors.textoPrimario
                )
                Text(
                    text = subtitulo,
                    fontSize = 12.sp,
                    color = MenuColors.textoMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            //Cadeado
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MenuColors.chevronCor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MenuCardPremium(
    isPremium: Boolean,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    val fundoCor = if (isPremium) MenuColors.branco else MenuColors.amareloFundo
    val bordaCor = if (isPremium) MenuColors.bordaCard else MenuColors.amareloBorda
    val iconeBg = if (isPremium) MenuColors.tealIconeBg else MenuColors.cinzaIconeBg
    val iconeColor = if (isPremium) MenuColors.tealIcone else MenuColors.cinzaIcone
    val tituloCor = if (isPremium) MenuColors.textoPrimario else MenuColors.textoMuted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fundoCor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, bordaCor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = iconeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = tituloCor
                    )
                    if (!isPremium) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = MenuColors.amareloBorda
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MenuColors.amareloTexto,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitulo,
                    fontSize = 12.sp,
                    color = MenuColors.textoMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Cadeado
            if (!isPremium) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = MenuColors.chevronCor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MenuColors.chevronCor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}