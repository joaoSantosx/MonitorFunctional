package com.jvf.monitorfunctional.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

class PremiumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PremiumScreen(
                    onVoltar = { finish() },
                    onAssinar = {
                        val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
                        val codigoFilho = prefs.getString("codigo_monitorado", "") ?: ""

                        if (codigoFilho.isNotEmpty()) {
                            Toast.makeText(this, "Processando...", Toast.LENGTH_SHORT).show()

                            val db = Firebase.firestore
                            db.collection("regras_parentais").document(codigoFilho)
                                .set(hashMapOf("plano" to "PREMIUM"), com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(this, "🎉 Bem-vindo ao Aware Kids Premium!", Toast.LENGTH_LONG).show()
                                    finish() // Fecha a tela de vendas e volta pro app já desbloqueado
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Erro de conexão. Tente novamente.", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Vincule um dispositivo primeiro.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(onVoltar: () -> Unit, onAssinar: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícone Dourado
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFFFF9C4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premium",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Aware Kids Premium", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Desbloqueie o controle total e tenha tranquilidade absoluta sobre o que seu filho consome.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Lista de Benefícios
            BeneficioItem("Análise de Vídeos Ilimitada")
            BeneficioItem("Gráficos de Horários de Uso")
            BeneficioItem("Filtros de Palavras Personalizados")
            BeneficioItem("Suporte Prioritário")

            Spacer(modifier = Modifier.weight(1f))

            // Botão de Assinar
            Button(
                onClick = onAssinar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)), // Amarelo Premium
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Assinar por R$ 9,90/mês", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun BeneficioItem(texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = Color(0xFF4CAF50))
        Spacer(modifier = Modifier.width(16.dp))
        Text(texto, fontSize = 16.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
    }
}