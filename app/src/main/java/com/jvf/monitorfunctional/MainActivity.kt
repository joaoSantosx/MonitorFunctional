package com.jvf.monitorfunctional

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Login cancelado ou falhou.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            SelecaoPerfilScreen(
                onResponsavelClick = { fluxoResponsavel() },
                onDependenteClick = { fluxoDependente() }
            )
        }
    }

    private fun fluxoResponsavel() {
        if (auth.currentUser != null) {
            //Vai direto pro menu do pai.
            startActivity(Intent(this, MenuActivity::class.java))
        } else {
            // Não está logado, inicia fluxo do Google
            iniciarLoginGoogle()
        }
    }
    private fun fluxoDependente() {
        startActivity(Intent(this, DependenteActivity::class.java))
    }

    private fun iniciarLoginGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }
}

@Composable
fun SelecaoPerfilScreen(
    onResponsavelClick: () -> Unit,
    onDependenteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            // Cabeçalho
            Text(
                text = "Quem vai usar este aparelho?",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Escolha o perfil para configurar o aplicativo corretamente neste dispositivo.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // CARD O Responsável (Pai/Mãe)
            PerfilCard(
                titulo = "Sou o Responsável",
                descricao = "Quero monitorar este ou outros aparelhos, ver alertas e configurar a IA.",
                icone = Icons.Default.Security,
                corDestaque = Color(0xFF1565C0),
                onClick = onResponsavelClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CARD O Dependente (Filho)
            PerfilCard(
                titulo = "Sou o Filho (Monitorado)",
                descricao = "Este é o aparelho da criança que será protegido pelo aplicativo.",
                icone = Icons.Default.Face,
                corDestaque = Color(0xFF4CAF50),
                onClick = onDependenteClick
            )

            // Mola invisível para não quebrar em telas diferentes
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
@Composable
fun PerfilCard(
    titulo: String,
    descricao: String,
    icone: ImageVector,
    corDestaque: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo com o icone
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(corDestaque.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = corDestaque,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = descricao,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}