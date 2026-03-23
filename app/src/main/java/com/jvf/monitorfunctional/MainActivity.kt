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
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
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
                onDependenteClick  = { fluxoDependente() }
            )
        }
    }

    private fun fluxoResponsavel() {
        if (auth.currentUser != null) {
            startActivity(Intent(this, MenuActivity::class.java))
        } else {
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
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }
}

private object MainColors {
    val azulEscuro    = Color(0xFF0D47A1)
    val azulPrimario  = Color(0xFF1565C0)
    val azulFundo     = Color(0xFFEFF6FF)
    val azulBorda     = Color(0xFFBFDBFE)
    val azulBadgeBg   = Color(0xFFEFF6FF)
    val azulBadgeText = Color(0xFF1565C0)

    val verdePrimario = Color(0xFF16A34A)
    val verdeFundo    = Color(0xFFF0FDF4)
    val verdeBadgeBg  = Color(0xFFF0FDF4)
    val verdeBadgeText= Color(0xFF16A34A)

    val cinzaFundo    = Color(0xFFF0F4F8)
    val bordaCard     = Color(0xFFE2E8F0)
    val textoPrimario = Color(0xFF1E293B)
    val textoMuted    = Color(0xFF64748B)
    val textoSutil    = Color(0xFFCBD5E1)
    val branco        = Color.White

    val gradienteHero = Brush.linearGradient(
        listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
    )
}

//Tela
@Composable
fun SelecaoPerfilScreen(
    onResponsavelClick: () -> Unit,
    onDependenteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainColors.cinzaFundo)
            .verticalScroll(rememberScrollState())
    ) {

        HeroSection()

        SectionTitle("Selecione seu perfil")

        PerfilCardModerno(
            badge       = "Responsável",
            titulo      = "Sou o Responsável",
            descricao   = "Monitore dispositivos, veja alertas e configure a IA de proteção.",
            icone       = Icons.Default.SupervisedUserCircle,
            iconeBg     = MainColors.azulFundo,
            iconeColor  = MainColors.azulPrimario,
            accentColor = MainColors.azulPrimario,
            badgeBg     = MainColors.azulBadgeBg,
            badgeText   = MainColors.azulBadgeText,
            destaque    = true,
            onClick     = onResponsavelClick
        )

        PerfilCardModerno(
            badge       = "Monitorado",
            titulo      = "Sou o Filho",
            descricao   = "Este é o aparelho da criança que será protegido pelo aplicativo.",
            icone       = Icons.Default.Person,
            iconeBg     = MainColors.verdeFundo,
            iconeColor  = MainColors.verdePrimario,
            accentColor = MainColors.verdePrimario,
            badgeBg     = MainColors.verdeBadgeBg,
            badgeText   = MainColors.verdeBadgeText,
            destaque    = false,
            onClick     = onDependenteClick
        )

        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aware Kids · Proteção inteligente para sua família",
                fontSize = 10.sp,
                color = MainColors.textoSutil,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MainColors.gradienteHero)
    ) {
        //decorativos
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
                .padding(top = 48.dp, bottom = 40.dp),
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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "BEM-VINDO AO AWARE KIDS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Quem vai usar\neste aparelho?",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Escolha o perfil para configurar o aplicativo\ncorretamente neste dispositivo.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

//Título
@Composable
private fun SectionTitle(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MainColors.textoMuted.copy(alpha = 0.7f),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun PerfilCardModerno(
    badge: String,
    titulo: String,
    descricao: String,
    icone: ImageVector,
    iconeBg: Color,
    iconeColor: Color,
    accentColor: Color,
    badgeBg: Color,
    badgeText: Color,
    destaque: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (destaque) MainColors.azulBorda else MainColors.bordaCard
    val borderWidth = if (destaque) 1.5.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MainColors.branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(90.dp)
                    .background(
                        color = accentColor,
                        shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                //caixinha
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icone,
                        contentDescription = null,
                        tint = iconeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                //Textos
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = badgeBg,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = titulo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MainColors.textoPrimario
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = descricao,
                        fontSize = 12.sp,
                        color = MainColors.textoMuted,
                        lineHeight = 17.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MainColors.cinzaFundo)
                        .border(1.dp, MainColors.bordaCard, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MainColors.textoSutil,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
