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
import androidx.compose.material.icons.filled.Security
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

class DependenteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val codigoFilho = obterOuGerarCodigoFilho()

        setContent {
            DependenteScreen(
                codigo = codigoFilho,
                contexto = this,
                onAbrirConfiguracoes = {
                    Toast.makeText(this, "Procure por 'Monitor Parental JVF' e ative.", Toast.LENGTH_LONG).show()
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
            prefs.edit {
                putString("codigo_filho", codigo)
            }
        }
        return codigo
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependenteScreen(codigo: String, contexto: Context, onAbrirConfiguracoes: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Modo Dependente",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = "Este aparelho está sendo protegido.\nPara o responsável monitorar, insira o código abaixo no celular dele:",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Card com o Código
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = codigo,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                    color = Color(0xFF1565C0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botão que liga o serviço
            Button(
                onClick = onAbrirConfiguracoes,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Ativar Monitoramento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            BotaoManutencao(contexto = contexto, codigoFilho = codigo)
        }
    }
}

@Composable
fun BotaoManutencao(contexto: Context, codigoFilho: String) {
    val prefs = contexto.getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
    var protecaoAtiva by remember { mutableStateOf(prefs.getBoolean("protecao_ativa", true)) }
    var mostrarDialogSenha by remember { mutableStateOf(false) }
    var senhaDigitada by remember { mutableStateOf("") }
    var validandoOnline by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            if (protecaoAtiva) {
                mostrarDialogSenha = true // Pede senha
            } else {
                prefs.edit {
                    putBoolean("protecao_ativa", true)
                }
                protecaoAtiva = true
                Toast.makeText(contexto, "Proteção Reativada!", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            imageVector = if (protecaoAtiva) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
            tint = if (protecaoAtiva) Color.Gray else Color(0xFFF44336)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (protecaoAtiva) "Desativar Proteção (Requer PIN)" else "Proteção Desativada (Trancar)",
            color = if (protecaoAtiva) Color.Gray else Color(0xFFF44336)
        )
    }

    if (mostrarDialogSenha) {
        AlertDialog(
            onDismissRequest = { if (!validandoOnline) mostrarDialogSenha = false },
            title = { Text("Área do Responsável") },
            text = {
                Column {
                    Text("Digite o PIN mestre para liberar o acesso às configurações do Android.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = senhaDigitada,
                        onValueChange = { senhaDigitada = it },
                        label = { Text("PIN") },
                        singleLine = true,
                        enabled = !validandoOnline
                    )
                    if (validandoOnline) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !validandoOnline && senhaDigitada.isNotBlank(),
                    onClick = {
                        validandoOnline = true
                        val db = com.google.firebase.Firebase.firestore

                        db.collection("regras_parentais").document(codigoFilho).get()
                            .addOnSuccessListener { doc ->
                                validandoOnline = false
                                val pinCorreto = if (doc.exists() && doc.getString("pin_manutencao") != null) {
                                    doc.getString("pin_manutencao")
                                } else {
                                    "1234" // Se o pai nunca configurou, o padrão será 1234
                                }

                                if (senhaDigitada == pinCorreto) {
                                    prefs.edit {
                                        putBoolean("protecao_ativa", false)
                                    }
                                    protecaoAtiva = false
                                    mostrarDialogSenha = false
                                    senhaDigitada = ""
                                    Toast.makeText(contexto, "Configurações Liberadas!", Toast.LENGTH_LONG).show()
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
                    onClick = { mostrarDialogSenha = false }
                ) { Text("Cancelar") }
            }
        )
    }
}