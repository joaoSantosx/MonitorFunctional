#  Aware Kids - Monitoramento Parental Inteligente

![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android_OS-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Gemini API](https://img.shields.io/badge/Gemini_AI-8E75B2?style=for-the-badge&logo=google&logoColor=white)

O **Aware Kids** é um aplicativo nativo Android projetado para dar aos pais controle total e tranquilidade sobre o conteúdo que seus filhos consomem em plataformas de vídeo. 

Diferente de bloqueadores tradicionais, o app utiliza **Inteligência Artificial (Google Gemini)** para analisar dinamicamente o contexto dos vídeos assistidos, categorizando-os e emitindo alertas em tempo real baseados no nível de rigidez configurados pela família.

Desenvolvido por **João Victor Ferreira dos Santos, Brenda de Oliveira, Ramon Roque**.

---

## Funcionalidades e Modelo de Negócios (SaaS)

O projeto foi arquitetado sob um modelo **Freemium**, separando as funcionalidades essenciais das avançadas através de Paywalls interativos e verificação de plano em tempo real na nuvem.

### Plano Free
* **Dashboard em Tempo Real:** O painel principal reage instantaneamente a novos vídeos assistidos utilizando `SnapshotListeners` do Firebase.
* **Termômetro de Segurança:** Indicador visual animado que calcula a porcentagem de conteúdo seguro assistido recentemente.
* **Níveis de Rigidez da IA:** Três configurações possíveis (Alta, Média, Baixa) que alteram o prompt de análise da inteligência artificial.
* **Filtro de Notificações:** Controle granular sobre quais categorias de alertas (Violência, Adulto, Educativo, etc.) acionam notificações no celular do responsável.
* **Segurança Anti-Fraude:** PIN de manutenção exigido para alterar configurações críticas ou desativar o monitoramento.

### Plano Premium
* **Filtros Personalizados de Família:** Permite adicionar palavras-chave ou temas específicos (ex: "Susto", "Futebol") que devem ser considerados como nocivos pelo agente de IA.
* **Filtro Histórico por Data:** Desbloqueia o `DateRangePicker` nativo do Android, permitindo consultas complexas e paginação do histórico de vídeos por períodos específicos.
* **Gráficos de Horários de Uso:** Análise profunda dos horários de pico de consumo de tela do monitorado.

---

## Arquitetura e Tecnologias

O aplicativo foi construído seguindo as melhores práticas do ecossistema Android moderno:

* **UI/UX:** Totalmente construído com **Jetpack Compose** (Material Design 3). Componentes modulares, gerenciamento de estado (`StateHoisting`) e animações fluidas (`animateColorAsState`, `animateFloatAsState`).
* **Backend & Database:** **Firebase Cloud Firestore** operando como "Única fonte da verdade". A sincronização bidirecional garante que se o plano (Free/Premium) for alterado no banco, a interface do usuário se reestrutura instantaneamente sem necessidade de recarregar a tela.
*Inteligência Artificial & Prompt Engineering:** Integração com LLMs para processamento de linguagem natural. Utilização de engenharia de prompts avançada para injetar regras absolutas de exceção e bloqueio baseadas no usuário.
* **Captura de Dados:** Uso de `AccessibilityService` para leitura de nós de tela (ViewNodes) no YouTube, capturando títulos e pesquisas de forma invisível.
* **Persistência Local:** Uso estratégico do `SharedPreferences` para cache de códigos de vinculação e estado de planos, otimizando as requisições ao banco.

---

## Telas do Aplicativo

1. **Painel de Controle (Menu):** Central de comando do responsável com acesso rápido aos relatórios, status de monitoramento e atalhos de assinatura.
2. **Relatório de Vídeos:** Exibe o termômetro de segurança, o filtro premium de calendário e a lista de vídeos assistidos com motivos detalhados gerados pela IA.
3. **Score da Semana:** Gráfico gerencial (`LinearProgressIndicator`) agrupando os dados de consumo da semana por categoria.
4. **Relatório de Buscas:** Tela que exibe os últimos termos pesquisados pelo monitorado na barra de buscas do YouTube.
5. **Configurações:** Painel contendo regras de rigidez, filtros de notificação, gerenciamento de PIN e a configuração das listas de exceções (Upsell).
6. **Paywall (Premium):** Tela de vendas otimizada, destacando os benefícios do plano pago e simulando o gateway de assinatura.

---

## Como executar o projeto

1. Clone este repositório:
   ```bash
   git clone [https://github.com/joaoSantosx/MonitorFunctional.git] (https://github.com/joaoSantosx/MonitorFunctional.git) 
