package com.jvf.monitorfunctional.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AcessoPorHora(
    val hora: Int, // 0 a 23
    val quantidade: Int
)
// Gráfico em si
@Composable
fun GraficoUsoHorario(dados: List<AcessoPorHora>) {
    val maxAcessos = dados.maxOfOrNull { it.quantidade }?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Picos de Acesso (Últimas 24h)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))

            // Layout Principal do Gráfico
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Eixo Y (Escala de quantidades)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(30.dp) // Largura fixa para alinhar os números
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = maxAcessos.toString(), fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End)
                    Text(text = (maxAcessos / 2).toString(), fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End)
                    Text(text = "0", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End)
                }

                // Área das Barras
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dados.forEach { item ->
                        val proporcaoAltura = (item.quantidade.toFloat() / maxAcessos).coerceAtLeast(0.05f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.dp)
                                .fillMaxHeight(proporcaoAltura)
                                .background(
                                    color = if (item.quantidade == maxAcessos && item.quantidade > 0)
                                        Color(0xFF1565C0)
                                    else
                                        Color(0xFFBBDEFB),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legenda do Eixo X (Horas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00h", fontSize = 10.sp, color = Color.Gray)
                Text("06h", fontSize = 10.sp, color = Color.Gray)
                Text("12h", fontSize = 10.sp, color = Color.Gray)
                Text("18h", fontSize = 10.sp, color = Color.Gray)
                Text("23h", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}