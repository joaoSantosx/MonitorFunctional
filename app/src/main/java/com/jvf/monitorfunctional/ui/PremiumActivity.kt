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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.firestore.SetOptions


class PremiumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PremiumScreen(
                onVoltar = { finish() },
                onAssinar = {
                    val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
                    val codigoFilho = prefs.getString("codigo_monitorado", "") ?: ""

                    if (codigoFilho.isNotEmpty()) {
                        Toast.makeText(this, "Processando...", Toast.LENGTH_SHORT).show()
                        Firebase.firestore
                            .collection("regras_parentais")
                            .document(codigoFilho)
                            .set(hashMapOf("plano" to "PREMIUM"), SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(this, "Bem-vindo ao Aware Kids Premium!", Toast.LENGTH_LONG).show()
                                finish()
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

private object PremColors {
    val heroBg        = Color(0xFF1C1433)
    val heroCircle    = Color(0xFF2A1F4A)

    // Dourado
    val ouro          = Color(0xFFFAC775)
    val ouroBg        = Color(0x26FAC775) // 15% alpha
    val ouroBorda     = Color(0x4DFAC775) // 30% alpha
    val ouroTexto     = Color(0xFF412402)

    // Benefícios
    val amberFundo    = Color(0xFFFAEEDA)
    val amberIcone    = Color(0xFFBA7517)
    val azulFundo     = Color(0xFFEFF6FF)
    val azulIcone     = Color(0xFF1565C0)
    val verdeFundo    = Color(0xFFF0FDF4)
    val verdeIcone    = Color(0xFF16A34A)
    val verdeBorda    = Color(0xFFBBF7D0)
    val verdeCheck    = Color(0xFF16A34A)
    val roxoFundo     = Color(0xFFF3F0FF)
    val roxoIcone     = Color(0xFF7C3AED)
    val cinzaFundo    = Color(0xFFF0F4F8)
    val bordaCard     = Color(0xFFE2E8F0)
    val cinzaClaro    = Color(0xFFF1F5F9)
    val textoPrimario = Color(0xFF1E293B)
    val textoMuted    = Color(0xFF94A3B8)
    val branco        = Color.White

    val gradienteHero = Brush.linearGradient(
        listOf(Color(0xFF1C1433), Color(0xFF2D1B5E))
    )
}

// Tela
@Composable
fun PremiumScreen(onVoltar: () -> Unit, onAssinar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremColors.cinzaFundo)
            .verticalScroll(rememberScrollState())
    ) {
        HeroPremium(onVoltar = onVoltar)

        PremSectionTitle("O que está incluído")

        CardBeneficios()

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PremColors.ouro)
                .clickable { onAssinar() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PremColors.ouroTexto,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Assinar por R$ 9,90/mês",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PremColors.ouroTexto
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = PremColors.textoMuted,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Cancele quando quiser · Sem taxas ocultas",
                fontSize = 10.sp,
                color = PremColors.textoMuted
            )
        }
    }
}

@Composable
private fun HeroPremium(onVoltar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PremColors.gradienteHero)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 250.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(PremColors.ouroBg)
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(x = (-20).dp, y = 100.dp)
                .clip(CircleShape)
                .background(Color(0x0FFAC775))
        )

        Box(
            modifier = Modifier
                .padding(top = 20.dp, start = 20.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PremColors.ouroBg)
                    .border(2.dp, PremColors.ouroBorda, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PremColors.ouro,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = PremColors.ouroBg,
                modifier = Modifier.border(1.dp, PremColors.ouroBorda, RoundedCornerShape(99.dp))
            ) {
                Text(
                    text = "PREMIUM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PremColors.ouro,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Aware Kids\nPremium",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Text(
                text = "Controle total sobre o conteúdo que\nseu filho consome no YouTube.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "R$",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremColors.ouro,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "9,90",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = PremColors.ouro,
                    lineHeight = 48.sp
                )
                Text(
                    text = "/mês",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

// Card benefícios
@Composable
private fun CardBeneficios() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PremColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PremColors.bordaCard)
    ) {
        Column {
            BeneficioRow(
                iconeBg = PremColors.amberFundo,
                iconeColor = PremColors.amberIcone,
                icone = Icons.Default.Shield,
                titulo = "Análise Ilimitada de Vídeos",
                subtitulo = "Sem limite de vídeos monitorados",
                isUltimo = false
            )
            BeneficioRow(
                iconeBg = PremColors.azulFundo,
                iconeColor = PremColors.azulIcone,
                icone = Icons.Default.BarChart,
                titulo = "Gráficos de Horários",
                subtitulo = "Visualize os picos de uso do app",
                isUltimo = false
            )
            BeneficioRow(
                iconeBg = PremColors.roxoFundo,
                iconeColor = PremColors.roxoIcone,
                icone = Icons.Default.Tune,
                titulo = "Filtros Personalizados",
                subtitulo = "Regras baseadas nos valores da família",
                isUltimo = false
            )
            BeneficioRow(
                iconeBg = PremColors.verdeFundo,
                iconeColor = PremColors.verdeIcone,
                icone = Icons.Default.DateRange,
                titulo = "Histórico Completo",
                subtitulo = "Filtro por período ilimitado",
                isUltimo = true
            )
        }
    }
}

@Composable
private fun BeneficioRow(
    iconeBg: Color,
    iconeColor: Color,
    icone: ImageVector,
    titulo: String,
    subtitulo: String,
    isUltimo: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = iconeColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PremColors.textoPrimario)
                Text(subtitulo, fontSize = 11.sp, color = PremColors.textoMuted, modifier = Modifier.padding(top = 1.dp))
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(PremColors.verdeFundo)
                    .border(1.5.dp, PremColors.verdeBorda, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PremColors.verdeCheck,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        if (!isUltimo) {
            HorizontalDivider(
                color = PremColors.cinzaClaro,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}
@Composable
private fun PremSectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = PremColors.textoMuted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}