package com.jvf.monitorfunctional
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

class IntroductionActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        val jaViuIntroduction = prefs.getBoolean("introducao_concluido", false)

        // Se já viu a introdução, pula direto para o aplicativo
        if (jaViuIntroduction) {
            irParaProximaTela()
            return
        }

        setContent {
            IntroductionScreen(
                onFinalizar = {
                    prefs.edit {
                        putBoolean("introducao_concluido", true)
                    }
                    irParaProximaTela()
                }
            )
        }
    }

    private fun irParaProximaTela() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}

@Composable
fun IntroductionScreen(onFinalizar: () -> Unit) {
    var paginaAtual by remember { mutableIntStateOf(1) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            //troca suave entre a tela 1 e 2
            Crossfade(targetState = paginaAtual, label = "animacao_telas") { tela ->
                when (tela) {
                    1 -> ConteudoPagina(
                        icone = Icons.Default.Face,
                        titulo = "Bem-vindo ao\nKidsSafeTube",
                        descricao = "O app monitor parental inteligente projetado para garantir a segurança digital do seu filho no YouTube."
                    )
                    2 -> ConteudoPagina(
                        icone = Icons.Default.Security,
                        titulo = "Inteligência Artificial\na seu favor",
                        descricao = "Nós analisamos o contexto dos vídeos em tempo real. Você define as regras, e nossa IA bloqueia o conteúdo inadequado silenciosamente."
                    )
                }
            }

            // Rodapé com os botões e pontinhos da navegação
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bolinhas
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IndicadorPagina(ativo = paginaAtual == 1)
                    IndicadorPagina(ativo = paginaAtual == 2)
                }

                // Botão ação
                Button(
                    onClick = {
                        if (paginaAtual == 1) {
                            paginaAtual = 2
                        } else {
                            onFinalizar()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text(
                        text = if (paginaAtual == 1) "Próximo" else "Começar Agora",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (paginaAtual == 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ConteudoPagina(icone: ImageVector, titulo: String, descricao: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFE3F2FD), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF1565C0)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = titulo,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = descricao,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun IndicadorPagina(ativo: Boolean) {
    Box(
        modifier = Modifier
            .width(if (ativo) 24.dp else 10.dp)
            .height(10.dp)
            .clip(CircleShape)
            .background(if (ativo) Color(0xFF1565C0) else Color.LightGray)
    )
}