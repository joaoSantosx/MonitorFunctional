package com.jvf.monitorfunctional

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.core.content.edit

class MenuActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var apelidoState = mutableStateOf("")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("Monitor Funcional", "O pai autorizou os alertas!")
        } else {
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
                onVerRelatorio = {
                    if (apelidoState.value.isNotEmpty()) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                    } else {
                        Toast.makeText(this, "Nenhum dispositivo vinculado ainda!", Toast.LENGTH_SHORT).show()
                    }
                },
                onVincularNovo = {
                    solicitarCodigoDoFilho()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        apelidoState.value = prefs.getString("apelido_monitorado", "") ?: ""
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
            .setPositiveButton("Vincular") { _, _ ->
                val codigoDigitado = inputCodigo.text.toString().trim().uppercase()
                val apelidoDigitado = inputApelido.text.toString().trim()

                if (codigoDigitado.isNotEmpty() && apelidoDigitado.isNotEmpty()) {
                    prefs.edit {
                        putString("codigo_monitorado", codigoDigitado)
                        putString("apelido_monitorado", apelidoDigitado)
                    }

                    apelidoState.value = apelidoDigitado

                    Firebase.messaging.subscribeToTopic("alerta_$codigoDigitado")
                        .addOnCompleteListener { task ->
                            var msg = "Vinculado com sucesso e alertas ativados!"
                            if (!task.isSuccessful) {
                                msg = "Vinculado, mas falha ao ativar alertas."
                            }
                            Toast.makeText(this@MenuActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this@MenuActivity, "Preencha os dois campos!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(apelidoAtual: String, onVerRelatorio: () -> Unit, onVincularNovo: () -> Unit) {
    val contexto = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel do Responsável", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        contexto.startActivity(Intent(contexto, ConfiguracoesActivity::class.java))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1 Relatório Padrão
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onVerRelatorio() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Acessar Relatório Padrão", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (apelidoAtual.isEmpty()) "Nenhum dispositivo vinculado" else "Monitorando: $apelidoAtual",
                            color = Color.Gray, fontSize = 14.sp
                        )
                    }
                }
            }

            // Card 2 Relatório Analítico (Ultimas 24hrs)
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (apelidoAtual.isNotEmpty()) {
                        contexto.startActivity(Intent(contexto, RelatoriosActivity::class.java))
                    } else {
                        Toast.makeText(contexto, "Nenhum dispositivo vinculado ainda!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF009688), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Gráfico de Horários", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Descubra os picos de uso do app", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            // Card 3 Relatório de Pesquisas
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (apelidoAtual.isNotEmpty()) {
                        contexto.startActivity(Intent(contexto, PesquisasActivity::class.java))
                    } else {
                        Toast.makeText(contexto, "Nenhum dispositivo vinculado ainda!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Relatório de Pesquisas",
                        tint = Color(0xFF673AB7),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Relatório de Pesquisas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Veja os termos buscados pelo monitorado no YouTube", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            // Card 4: Vincular Novo Dispositivo
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onVincularNovo() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Novo Dispositivo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Vincular o celular de um filho(a)", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}