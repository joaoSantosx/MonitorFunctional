package com.jvf.monitorfunctional

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.content.Context
import androidx.appcompat.app.AlertDialog
import java.util.UUID
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()

        // Referências aos Cards (Botões)
        val btnResponsavel = findViewById<CardView>(R.id.cardResponsavel)
        val btnDependente = findViewById<CardView>(R.id.cardDependente)

        // 1. Fluxo do Responsável: Vai para a Dashboard
        btnResponsavel.setOnClickListener {
            if (auth.currentUser != null) {
                // Já está logado! Vai direto pro Painel.
                startActivity(Intent(this, DashboardActivity::class.java))
            } else {
                // Não está logado, inicia fluxo do Google
                iniciarLoginGoogle()
            }
        }

        // 2. Fluxo do Filho: Vai para as Configurações de Acessibilidade

            btnDependente.setOnClickListener {
                val codigoFilho = obterOuGerarCodigoFilho()
                AlertDialog.Builder(this)
                    .setTitle("Modo Dependente Ativado")
                    .setMessage("O código deste dispositivo é:\n\n$codigoFilho\n\nPeça para o seu Responsável inserir este código no celular dele.")
                    .setPositiveButton("Ligar Acessibilidade") { _, _ ->
                        abrirConfiguracoesAcessibilidade() // Sua função que abre as configs
                    }
                    .show()
            }
        }

    private fun iniciarLoginGoogle() {
        // Configura o pedido de login pegando o ID que o próprio Firebase injeta no app
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Login com o Google deu certo, agora passa a credencial pro Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Erro no Login Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Bem-vindo, ${auth.currentUser?.displayName}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                } else {
                    Toast.makeText(this, "Falha na Autenticação", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun obterOuGerarCodigoFilho(): String {
        val prefs = getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
        var codigo = prefs.getString("codigo_filho", null)

        //Se for primeiro acesso
        if (codigo == null) {
            codigo = UUID.randomUUID().toString().substring(0, 6).uppercase()
            prefs.edit().putString("codigo_filho", codigo).apply()
        }
        return codigo
    }

    private fun abrirConfiguracoesAcessibilidade() {
        Toast.makeText(this, "Procure por 'Monitor Parental JVF' e ative.", Toast.LENGTH_LONG).show()

        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir as configurações.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun solicitarCodigoDoFilho() {
        val input = EditText(this)
        input.hint = "Ex: A7F9B2"

        AlertDialog.Builder(this)
            .setTitle("Vincular Dispositivo")
            .setMessage("Digite o código de 6 dígitos gerado no celular do seu filho:")
            .setView(input)
            .setPositiveButton("Vincular") { _, _ ->
                val codigoDigitado = input.text.toString().trim().uppercase()

                if (codigoDigitado.length >= 5) {
                    val prefs = getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("codigo_monitorado", codigoDigitado).apply()

                    // Vai pro Dashboard
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}