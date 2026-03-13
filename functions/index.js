const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.enviarAlertaVideoInadequado = onDocumentCreated("historico_parental/{docId}", async (event) => {
    const novoVideo = event.data.data();

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
            title: "⚠️ Alerta de Segurança!",
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