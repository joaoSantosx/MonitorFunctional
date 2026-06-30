const admin = require("firebase-admin");
const { onDocumentCreated } = require ("firebase-functions/v2/firestore");
const {onCall, HttpsError} = require ("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const {GoogleGenerativeAI, HarmCategory, HarmBlockThreshold} = require("@google/generative-ai");

admin.initializeApp();

const geminiApiKey = defineSecret("GEMINI_API_KEY");
const youtubeApiKey = defineSecret("YOUTUBE_API_KEY")


//Função auxiliar de segurança
const sanitizarTermos = (arrayTermos) => {
    if (!arrayTermos || !Array.isArray(arrayTermos)) return [];
    return arrayTermos.map(termo =>
    termo.replace(/[<>{}""'';:]/g, '')
    .substring(0,35)
    .trim()
    ).filter(termo => termo.length > 0);
    };


exports.enviarAlertaVideoInadequado = onDocumentCreated("historico_parental/{docId}", async (event) => {
    const novoVideo = event.data.data();

    if (!novoVideo) return null;

    const codigoFilho = novoVideo.codigo_pareamento;

    if (!codigoFilho || codigoFilho === "SEM_CODIGO") {
        console.log("Vídeo sem código de pareamento. Abortando.");
        return null;
    }

    if (novoVideo.nivel_rigidez_usado !== "BLOQUEIO_FREE") {
        const regrasRef = admin.firestore().collection("regras_parentais").doc(codigoFilho);
        const hojeStr = new Date().toISOString().split('T')[0];
        try {
            await admin.firestore().runTransaction(async (transaction) => {
                const doc = await transaction.get(regrasRef);

                let contagem = 1;
                let planoAtual = "FREE";

                if (doc.exists) {
                    const data = doc.data();
                    planoAtual = data.plano || "FREE";
                    const dataUltimo = data.data_ultimo_video || "";
                    contagem = data.contagem_videos_hoje || 0;

                    if (dataUltimo !== hojeStr) {
                        contagem = 1;
                    } else {
                        contagem += 1;
                    }
                }
                transaction.set(regrasRef, {
                    data_ultimo_video: hojeStr,
                    contagem_videos_hoje: contagem,
                    plano: planoAtual
                }, { merge: true });

                console.log(`Contador Freemium: ${contagem} vídeos hoje para ${codigoFilho}`);
            });
        } catch (error) {
            console.error("Erro ao atualizar contador Freemium no Firebase:", error);
        }
    }





    //Lógica para envio de notificações
    if (novoVideo.seguro === true) {
        console.log("Vídeo seguro. Nenhuma notificação necessária.");
        return null;
    }

    const codigoPareamento = novoVideo.codigo_pareamento;
    const titulo = novoVideo.titulo;
    const motivoIa = novoVideo.motivo_ia || "";

    if (!codigoPareamento) {
        console.log("Vídeo sem código de pareamento. Abortando.");
        return null;
    }

    let categoriaDetectada = "Outros"; // Padrão
    const categoriasPossiveis = ["Violência", "Adulto", "Educativo", "Entretenimento","Filtro Personalizado", "Outros"];

    for (const cat of categoriasPossiveis) {
        if (motivoIa.toLowerCase().includes(cat.toLowerCase())) {
            categoriaDetectada = cat;
            break;
        }
    }
    let categoriasPermitidas = ["Todas as categorias"];

    try {
        const configDoc = await admin.firestore().collection("configuracoes_notificacao").doc(codigoPareamento).get();
        if (configDoc.exists) {
            const salvas = configDoc.data().categorias_permitidas;
            if (salvas && salvas.length > 0) {
                categoriasPermitidas = salvas;
            }
        }
    } catch (error) {
        console.error("Erro ao buscar configurações de notificação:", error);
    }

    const paiQuerReceberTodas = categoriasPermitidas.includes("Todas as categorias");
    const paiQuerReceberEsta = categoriasPermitidas.includes(categoriaDetectada);

    if (!paiQuerReceberTodas && !paiQuerReceberEsta) {
        console.log(`🔇 Alerta Silenciado: A IA considerou o vídeo como inseguro, mas o pai optou por não receber notificações da categoria '${categoriaDetectada}'.`);
        return null;
    }

    const tituloNotificacao = categoriaDetectada == "Filtro Personalizado"
    ? "Alerta de Filtro Personalizado"
    : "⚠️ Alerta de Segurança!:";



    const payload = {
        notification: {
            title: tituloNotificacao,
            body: `Registrado (${categoriaDetectada}): ${titulo}`
        },
        topic: `alerta_${codigoPareamento}`
    };

    try {
        const response = await admin.messaging().send(payload);
        console.log("Notificação enviada com sucesso:", response);
    } catch (error) {
        console.error("Erro ao enviar notificação:", error);
    }

    return null;
});

// Analisar vídeo com Cache Global Neutro e Motor de Regras Local
exports.analisarVideoGemini = onCall(
  { secrets: [geminiApiKey, youtubeApiKey] },
  async (request) => {
    // Barreira de Segurança
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "O usuário deve estar autenticado no aplicativo.");
    }

    const data = request.data;
    const tituloVideo = data.tituloVideo;
    const videoId = data.videoId;
    const nivelRigidez = data.nivelRigidez || "MEDIA";
    const palavrasMonitoradas = data.palavrasMonitoradas || [];
    const palavrasPermitidas = data.palavrasPermitidas || [];

    if (!tituloVideo) {
      throw new HttpsError("invalid-argument", "O título do vídeo é obrigatório.");
    }

    // 1. DEFINIÇÃO DA CHAVE DE COORDENAÇÃO DO CACHE GLOBAL
    const chaveCache = videoId ? videoId : Buffer.from(tituloVideo).toString('base64').substring(0, 40);
    const cacheRef = admin.firestore().collection("cache_global_videos").doc(chaveCache);

    let perfilVideo = null;

    try {
      // Tenta buscar o Raio-X do vídeo no Cache Global
      const cacheDoc = await cacheRef.get();

      if (cacheDoc.exists) {
        console.log(`[CACHE GLOBAL] ⚡ Hit! Usando perfil existente para: ${tituloVideo}`);
        perfilVideo = cacheDoc.data();
      } else {
        console.log(`[CACHE GLOBAL] ⏳ Miss! Mapeando vídeo inédito na nuvem: ${tituloVideo}`);

        let comentarios = "";

        // ==========================================
        // BUSCA DE COMENTÁRIOS (SEU BLOCO ORIGINAL ROBUSTO)
        // ==========================================
        if (videoId){
            try {
               const url = `https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&videoId=${videoId}&textFormat=plainText&key=${youtubeApiKey.value()}&maxResults=40`;

                const response = await fetch(url);

                if (response.status == 200) {
                    const json = await response.json();
                    const items = json.items || [];
                    const listaComentarios = [];

                    if (items.length > 0){
                        for (const item of items) {
                            const texto = item.snippet?.topLevelComment?.snippet?.textDisplay || "";
                            listaComentarios.push(texto.replace(/\n/g, " "));
                        }
                        comentarios = listaComentarios.join(" | ");
                    } else {
                        console.log(`Nenhum comentário encontrado para o vídeo ${videoId}.`);
                    }
                } else if (response.status === 403) {
                    console.log(`Comentários desativados para o vídeo ${videoId}.`);
                } else {
                    console.error(`Erro na API do youtube: Código ${response.status}`);
                }
            } catch (error) {
                console.error("Erro ao buscar comentários do Youtube no servidor:", error);
            }
        }
        // ==========================================

        // Chamamos o Gemini para extrair as características NEUTRAS do vídeo
        const genAI = new GoogleGenerativeAI(geminiApiKey.value());
        const safetySettings = [
          { category: HarmCategory.HARM_CATEGORY_HARASSMENT, threshold: HarmBlockThreshold.BLOCK_ONLY_HIGH },
          { category: HarmCategory.HARM_CATEGORY_HATE_SPEECH, threshold: HarmBlockThreshold.BLOCK_ONLY_HIGH },
          { category: HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, threshold: HarmBlockThreshold.BLOCK_ONLY_HIGH },
          { category: HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, threshold: HarmBlockThreshold.BLOCK_ONLY_HIGH },
        ];

        const model = genAI.getGenerativeModel({
          model: "gemini-2.5-flash",
          safetySettings: safetySettings
        });

        const promptNeutro = `Você é um analista de metadados de vídeo neutro e cirúrgico. Analise o contexto estrutural do vídeo com base no título e comentários.

                        Título do vídeo: "${tituloVideo}"
                        Comentários de usuários: [${comentarios}]

                        Sua missão é descrever o que há no vídeo sem aplicar regras parentais de censura. Seja preciso sobre presença de jogos, palavrões ou temas.

                        Responda APENAS um JSON cru (sem markdown, sem \`\`\`json):
                        {
                          "categoria": "Violência, Adulto, Educativo, Entretenimento ou Outros",
                          "nivel_violencia": "NENHUMA", "LEVE" ou "EXPLICITA",
                          "nivel_palavroes": "NENHUMA", "LEVE" ou "PESADA",
                          "contem_tema_adulto": true ou false,
                          "estritamente_infantil": true ou false,
                          "tags_conteudo": ["coloque", "aqui", "palavras", "chave", "do", "contexto", "e", "jogos"],
                          "resumo_neutro": "Explicação curta de uma frase sobre o assunto real do vídeo."
                        }`;

        const result = await model.generateContent(promptNeutro);
        const textoResposta = result.response.text() || "";
        const jsonLimpo = textoResposta.replace(/```json/g, "").replace(/```/g, "").trim();

        perfilVideo = JSON.parse(jsonLimpo);

        // Salva o raio-X estrutural na coleção global
        await cacheRef.set(perfilVideo);
        console.log(`[CACHE GLOBAL] ✅ Sucesso! Vídeo catalogado na biblioteca global.`);
      }

      // ==========================================
      // 2. MOTOR DE REGRAS LOCAL (JS ULTRA RÁPIDO)
      // ==========================================
      const monitoradasLimpas = sanitizarTermos(palavrasMonitoradas);
      const permitidasLimpas = sanitizarTermos(palavrasPermitidas);

      let ehSeguroParaEstaFamilia = true;
      let motivoFinal = `Categoria: ${perfilVideo.categoria}. ${perfilVideo.resumo_neutro}`;
      let categoriaDecidida = perfilVideo.categoria;

      const verificarPresencaDeTermos = (listaDeTermos) => {
          const tLower = tituloVideo.toLowerCase();
          return listaDeTermos.find(termo => {
              const termoLower = termo.toLowerCase();
              return tLower.includes(termoLower) ||
                     perfilVideo.tags_conteudo.some(tag => tag.toLowerCase().includes(termoLower));
          });
      };

      // REGRA A: Lista de Exceções (Palavras Permitidas)
      const termoAutorizadoEncontrado = verificarPresencaDeTermos(permitidasLimpas);
      if (termoAutorizadoEncontrado) {
          console.log(`[MECANISMO LOCAL] Vídeo autorizado pelo Filtro de Exceção contendo: ${termoAutorizadoEncontrado}`);
          return { ehSeguro: true, detalhes: `Autorizado por Filtro de Exceção. Categoria: ${perfilVideo.categoria}. ${perfilVideo.resumo_neutro}` };
      }

      // REGRA B: Lista de Bloqueio Manual (Palavras Monitoradas)
      const termoProibidoEncontrado = verificarPresencaDeTermos(monitoradasLimpas);
      if (termoProibidoEncontrado) {
          ehSeguroParaEstaFamilia = false;
          categoriaDecidida = "Filtro Personalizado";
          motivoFinal = `Categoria: Filtro Personalizado. O termo bloqueado "${termoProibidoEncontrado}" foi detectado no contexto do vídeo.`;
      } else {
          // REGRA C: Avaliação por Nível de Rigidez Padrão
          switch (nivelRigidez.toUpperCase()) {
              case "BAIXA":
                  if (perfilVideo.nivel_violencia === "EXPLICITA" || perfilVideo.contem_tema_adulto === true || perfilVideo.categoria === "Adulto") {
                      ehSeguroParaEstaFamilia = false;
                      motivoFinal = `Categoria: ${perfilVideo.categoria}. Alerta de conteúdo inadequado (Pornografia, crimes ou extrema violência).`;
                  }
                  break;
              case "MEDIA":
                  if (perfilVideo.nivel_violencia === "EXPLICITA" || perfilVideo.nivel_palavroes === "PESADA" || perfilVideo.contem_tema_adulto === true || perfilVideo.categoria === "Adulto" || perfilVideo.categoria === "Violência") {
                      ehSeguroParaEstaFamilia = false;
                      motivoFinal = `Categoria: ${perfilVideo.categoria}. Conteúdo contendo violência, palavrões pesados ou temas adultos.`;
                  }
                  break;
              case "ALTA":
                  if (perfilVideo.estritamente_infantil === false || perfilVideo.nivel_violencia !== "NENHUMA" || perfilVideo.nivel_palavroes !== "NENHUMA" || perfilVideo.contem_tema_adulto === true || perfilVideo.categoria === "Adulto" || perfilVideo.categoria === "Violência") {
                      ehSeguroParaEstaFamilia = false;
                      motivoFinal = `Categoria: ${perfilVideo.categoria}. O vídeo não se enquadra na categoria de conteúdo 'estritamente infantil' exigida pelas regras de filtragem rigorosas.`;
                  }
                  break;
          }
      }

      return { ehSeguro: ehSeguroParaEstaFamilia, detalhes: motivoFinal };

    } catch (error) {
      console.error("[MECANISMO IA CORRUPTO] Erro no processamento do fluxo com cache:", error);
      return { ehSeguro: true, detalhes: "Vídeo liberado temporariamente (Contingência de rede ativa)." };
    }
  }
);