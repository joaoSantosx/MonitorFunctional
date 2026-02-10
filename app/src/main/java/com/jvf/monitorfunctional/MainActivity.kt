package com.jvf.monitorfunctional

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Referências aos Cards (Botões)
        val btnResponsavel = findViewById<CardView>(R.id.cardResponsavel)
        val btnDependente = findViewById<CardView>(R.id.cardDependente)

        // 1. Fluxo do Responsável: Vai para a Dashboard
        btnResponsavel.setOnClickListener {
            // Verifica se a DashboardActivity existe (você vai criar/já criou)
            try {
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Dashboard ainda não criada!", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Fluxo do Filho: Vai para as Configurações de Acessibilidade
        btnDependente.setOnClickListener {
            abrirConfiguracoesAcessibilidade()
        }
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
}