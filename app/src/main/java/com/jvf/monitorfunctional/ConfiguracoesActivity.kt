package com.jvf.monitorfunctional

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.tasks.await
import com.jvf.monitorfunctional.ui.PremiumActivity

class ConfiguracoesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("MonitorPrefs", MODE_PRIVATE)
        val codigoMonitorado = prefs.getString("codigo_monitorado", "") ?: ""
        val planoAtual = prefs.getString("tipo_plano", "FREE") ?: "FREE"
        if (codigoMonitorado.isEmpty()) {
            Toast.makeText(this, "Nenhum dispositivo vinculado para configurar.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContent {
            ConfiguracoesScreen(
                codigoFilho = codigoMonitorado,
                planoAtual = planoAtual,
                onVoltar = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(codigoFilho: String, planoAtual: String, onVoltar: () -> Unit) {
    val contexto = LocalContext.current
    val db = Firebase.firestore
    var carregando by remember { mutableStateOf(true) }

    //Estados da IA
    var nivelSelecionado by remember { mutableStateOf("ALTA") }
    //Estados pin
    var mostrarDialogPin by remember { mutableStateOf(false) }
    var novoPinDigitado by remember { mutableStateOf("") }
    //Estados das Notificações
    var notificacoesExpandido by remember { mutableStateOf(false) }
    var categoriasSalvas by remember { mutableStateOf(setOf("Todas as categorias")) }
    var categoriasEditando by remember { mutableStateOf(setOf("Todas as categorias")) }
    val listaCategorias = listOf("Violência", "Adulto", "Educativo", "Entretenimento", "Filtro Personalizado", "Outros")
    //regras dinâmicas (blocklist)
    var novaPalavra by remember {mutableStateOf("")}
    var palavrasMonitoradas by remember { mutableStateOf(listOf<String>()) }
    //regras dinâmicas(whitelist)
    var novaPalavraPermitida by remember {mutableStateOf("")}
    var palavrasPermitidas by remember { mutableStateOf(listOf<String>()) }
    // Busca os dados iniciais do Firebase (IA,Notificações,PIN)
    LaunchedEffect(codigoFilho) {
        try {
            // Busca IA e palavras filtradas e espera
            val docRegra = db.collection("regras_parentais").document(codigoFilho).get().await()
            if (docRegra.exists()) {
                nivelSelecionado = docRegra.getString("nivel") ?: "ALTA"

                val palavrasSalvas = docRegra.get("palavras_monitoradas") as? List<*>
                if (palavrasSalvas != null) {
                    palavrasMonitoradas = palavrasSalvas.map { it.toString() }
                }

                // 🚨 BUSCA AS PALAVRAS PERMITIDAS
                val palavrasPermitidasSalvas = docRegra.get("palavras_permitidas") as? List<*>
                if (palavrasPermitidasSalvas != null) {
                    palavrasPermitidas = palavrasPermitidasSalvas.map { it.toString() }
                }
            }

            // Busca Notificações e esperao
            val docNotif = db.collection("configuracoes_notificacao").document(codigoFilho).get().await()
            if (docNotif.exists()) {
                val salvas = docNotif.get("categorias_permitidas") as? List<*>
                if (salvas != null && salvas.isNotEmpty()) {
                    val setSalvo = salvas.map { it.toString() }.toSet()
                    categoriasSalvas = setSalvo
                    categoriasEditando = setSalvo
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            carregando = false
        }
    }

    val salvarRegraIA = { novoNivel: String ->
        nivelSelecionado = novoNivel
        db.collection("regras_parentais").document(codigoFilho).set(hashMapOf("nivel" to novoNivel), SetOptions.merge())
    }
    //blacklist
    fun adicionarPalavra() {
        if (novaPalavra.isNotBlank() && !palavrasMonitoradas.map { it.lowercase() }.contains(novaPalavra.trim().lowercase())) {
            val novaLista = palavrasMonitoradas + novaPalavra.trim()
            palavrasMonitoradas = novaLista

            db.collection("regras_parentais").document(codigoFilho)
                .set(hashMapOf("palavras_monitoradas" to novaLista), SetOptions.merge())
            novaPalavra = ""
            Toast.makeText(contexto, "Regra adicionada!", Toast.LENGTH_SHORT).show()
        }
    }

    fun removerPalavra(palavra: String) {
        val novaLista = palavrasMonitoradas.filter { it != palavra }
        palavrasMonitoradas = novaLista

        db.collection("regras_parentais").document(codigoFilho)
            .set(hashMapOf("palavras_monitoradas" to novaLista), SetOptions.merge())
    }

    //whitelist
    fun adicionarPalavraPermitida() {
        if (novaPalavraPermitida.isNotBlank() && !palavrasPermitidas.map { it.lowercase() }.contains(novaPalavraPermitida.trim().lowercase())) {
            val novaLista = palavrasPermitidas + novaPalavraPermitida.trim()
            palavrasPermitidas = novaLista
            db.collection("regras_parentais").document(codigoFilho).set(hashMapOf("palavras_permitidas" to novaLista), SetOptions.merge())
            novaPalavraPermitida = ""
            Toast.makeText(contexto, "Exceção adicionada!", Toast.LENGTH_SHORT).show()
        }
    }

    fun removerPalavraPermitida(palavra: String) {
        val novaLista = palavrasPermitidas.filter { it != palavra }
        palavrasPermitidas = novaLista
        db.collection("regras_parentais").document(codigoFilho).set(hashMapOf("palavras_permitidas" to novaLista), SetOptions.merge())
    }

    // Lógica dos checkboxes
    fun alternarCategoria(opcao: String) {
        val novoSet = categoriasEditando.toMutableSet()
        if (opcao == "Todas as categorias") {
            novoSet.clear()
            novoSet.add("Todas as categorias")
        } else {
            novoSet.remove("Todas as categorias")
            if (novoSet.contains(opcao)) {
                novoSet.remove(opcao)
                if (novoSet.isEmpty()) novoSet.add("Todas as categorias")
            } else {
                novoSet.add(opcao)
            }
        }
        categoriasEditando = novoSet
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        if (carregando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // função para tela pequena não cortar o texto
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filtro de notificações
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { notificacoesExpandido = !notificacoesExpandido }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF1565C0))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Filtro de Notificações", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Escolha quais alertas deseja receber", color = Color.Gray, fontSize = 14.sp)
                            }
                            Icon(
                                imageVector = if (notificacoesExpandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null, tint = Color.Gray
                            )
                        }

                        // Animação suave pra expandir
                        AnimatedVisibility(visible = notificacoesExpandido) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(8.dp))

                                ItemCheckbox("Todas as categorias", categoriasEditando.contains("Todas as categorias")) { alternarCategoria("Todas as categorias") }
                                listaCategorias.forEach { cat ->
                                    val isBloqueado = (cat == "Filtro Personalizado" && planoAtual != "PREMIUM")
                                    ItemCheckbox(
                                        texto = if (isBloqueado) "$cat 🔒" else cat,
                                        estaMarcado = categoriasEditando.contains(cat) && !isBloqueado,
                                        isDesativado = isBloqueado // Vamos precisar ajustar o ItemCheckbox lá embaixo
                                    ) {
                                        if (isBloqueado) {
                                            contexto.startActivity(android.content.Intent(contexto, PremiumActivity::class.java))
                                        } else {
                                            alternarCategoria(cat)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Botões Salvar/Cancelar
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        categoriasEditando = categoriasSalvas // Reverte pro que estava salvo
                                        notificacoesExpandido = false
                                    }) {
                                        Text("Cancelar", color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            categoriasSalvas = categoriasEditando // Confirma a edição
                                            db.collection("configuracoes_notificacao").document(codigoFilho)
                                                .set(hashMapOf("categorias_permitidas" to categoriasSalvas.toList()))
                                            notificacoesExpandido = false
                                            Toast.makeText(contexto, "Filtros salvos!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                    ) {
                                        Text("Salvar")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                Text("Nível de Rigidez da Inteligência Artificial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)

                OpcaoRigidezCard("Alta (Recomendado)", "Alerta sobre palavrões e jogos violentos. Foco em conteúdo infantil.", Color(0xFF4CAF50), nivelSelecionado == "ALTA") { salvarRegraIA("ALTA") }
                OpcaoRigidezCard("Média", "Desconsidera jogos infantis e humor leve. Alerta sobre pornografia e violência explícita.", Color(0xFFFF9800), nivelSelecionado == "MEDIA") { salvarRegraIA("MEDIA") }
                OpcaoRigidezCard("Baixa", "Desconsidera jogos violentos fictícios. Alerta apenas pornografia e crimes.", Color(0xFFF44336), nivelSelecionado == "BAIXA") { salvarRegraIA("BAIXA") }


                Text("Filtros Personalizados da Família", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)

                //Blacklist e whitelist
                if (planoAtual == "PREMIUM") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Adicione temas específicos que o aplicativo deve considerar como nocivo com base nas regras da sua casa (ex: Futebol, Maquiagem, Susto, Armas).",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Campo de entrada e botão Add
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = novaPalavra,
                                    onValueChange = { novaPalavra = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Ex: Câmera escondida") },
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { adicionarPalavra() },
                                    modifier = Modifier.height(56.dp)
                                        .padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF1565C0
                                        )
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Adicionar",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Lista dinâmica em formato de "Chips"
                            if (palavrasMonitoradas.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                // etiquetas quebrem a linha automaticamente se a tela for pequena
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    palavrasMonitoradas.forEach { palavra ->
                                        InputChip(
                                            selected = false,
                                            onClick = { removerPalavra(palavra) },
                                            label = { Text(palavra) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remover",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Gray
                                                )
                                            },
                                            colors = InputChipDefaults.inputChipColors(
                                                containerColor = Color(0xFFF1F5F9), // Cinza bem clarinho
                                                labelColor = Color(0xFF1E293B)
                                            ),
                                            border = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                    //Whitelist
                    Text("Exceções Permitidas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Adicione palavras que a IA deve sempre liberar, mesmo que pareçam suspeitas (ex: Minecraft, Roblox).", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = novaPalavraPermitida,
                                    onValueChange = { novaPalavraPermitida = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Ex: Minecraft") },
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { adicionarPalavraPermitida() },
                                    modifier = Modifier.height(56.dp).padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Verde para liberar
                                ) { Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White) }
                            }

                            if (palavrasPermitidas.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    palavrasPermitidas.forEach { palavra ->
                                        InputChip(
                                            selected = false,
                                            onClick = { removerPalavraPermitida(palavra) },
                                            label = { Text(palavra) },
                                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32)) },
                                            colors = InputChipDefaults.inputChipColors(containerColor = Color(0xFFE8F5E9), labelColor = Color(0xFF1B5E20)), // Fundo verde claro, texto verde escuro
                                            border = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // MODO FREE: Mostra o Banner de Venda
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            contexto.startActivity(android.content.Intent(contexto, PremiumActivity::class.java))
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Desbloqueie Filtros Específicos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF57F17))
                                Text("Assine o Premium para criar regras baseadas nos valores da sua família.", color = Color.DarkGray, fontSize = 14.sp)
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                // Pin de segurança
                Text("Segurança do Dispositivo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PIN de Manutenção", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Senha para desativar a proteção no celular do dependente.", color = Color.Gray, fontSize = 14.sp)
                        }
                        Button(
                            onClick = { mostrarDialogPin = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Text("Alterar")
                        }
                    }
                }

                // Pop-up para alterar o PIN acompanhando o bloco
                if (mostrarDialogPin) {
                    AlertDialog(
                        onDismissRequest = { mostrarDialogPin = false },
                        title = { Text("Alterar PIN") },
                        text = {
                            Column {
                                Text("Digite uma nova senha (recomendado 4 números):")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = novoPinDigitado,
                                    onValueChange = { novoPinDigitado = it },
                                    label = { Text("Novo PIN") },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (novoPinDigitado.isNotBlank()) {
                                    // Salva no Firebase mesclando com os dados existentes (IA)
                                    db.collection("regras_parentais").document(codigoFilho)
                                        .set(hashMapOf("pin_manutencao" to novoPinDigitado), SetOptions.merge())
                                    mostrarDialogPin = false
                                    novoPinDigitado = ""
                                    Toast.makeText(contexto, "PIN atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("Salvar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { mostrarDialogPin = false }) { Text("Cancelar") }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
//checkbox das notificações
@Composable
fun ItemCheckbox(texto: String, estaMarcado: Boolean, isDesativado: Boolean = false,onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isDesativado) { onClick() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = estaMarcado,
            onCheckedChange = null,
            enabled = !isDesativado,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1565C0))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            texto,
            fontSize = 16.sp,
            color = if (isDesativado) Color.LightGray else Color(0xFF1E293B),
            fontWeight = if (estaMarcado) FontWeight.Bold else FontWeight.Normal
        )
    }
}
//card rigidez
@Composable
fun OpcaoRigidezCard(titulo: String, descricao: String, corDestaque: Color, selecionado: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selecionado) corDestaque.copy(alpha = 0.1f) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selecionado) 4.dp else 1.dp),
        border = if (selecionado) BorderStroke(2.dp, corDestaque) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selecionado) corDestaque else Color.DarkGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(descricao, color = Color.Gray, fontSize = 14.sp)
            }
            if (selecionado) {
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = corDestaque, modifier = Modifier.size(28.dp))
            }
        }
    }
}