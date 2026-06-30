# Aware Kids - Monitoramento Parental Inteligente

![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android_OS-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Gemini API](https://img.shields.io/badge/Gemini_AI-8E75B2?style=for-the-badge&logo=google&logoColor=white)

O **Aware Kids** é um ecossistema nativo Android projetado para dar aos pais controle total e tranquilidade sobre o conteúdo que seus filhos consomem em plataformas de vídeo. 

Diferente de bloqueadores tradicionais baseados em DNS, o sistema utiliza **Inteligência Artificial (Google Gemini) Server-Side** para analisar dinamicamente o contexto dos vídeos e comentários assistidos, categorizando-os e emitindo alertas via Push Notification em tempo real, baseados no nível de rigidez configurado pela família.

Desenvolvido por **João Victor Ferreira dos Santos, Brenda de Oliveira, Ramon Roque**.

---

## 🚀 Modelo de Negócios (SaaS Freemium)

O projeto foi arquitetado sob um modelo **Freemium**, processando limites de uso e transações diretamente via Firebase Cloud Functions para evitar burlas no lado do cliente (Client-Side).

### Plano Free
* **Dashboard em Tempo Real:** O painel principal reage instantaneamente a novos vídeos utilizando `SnapshotListeners` do Firebase.
* **Termômetro de Segurança:** Indicador visual animado que calcula a porcentagem de conteúdo seguro assistido recentemente.
* **Limite Diário de Análises:** Cota de avaliação de até 40 vídeos diários controlada via transações no servidor. Ao atingir o limite, o monitoramento é pausado (exigindo upgrade).
* **Níveis de Rigidez da IA:** Três configurações possíveis (Alta, Média, Baixa) que calibram os pesos estruturais da análise.
* **Relatório de Buscas:** Registro completo dos termos pesquisados na barra de buscas do YouTube pelo monitorado.

### Plano Premium
* **Monitoramento Ilimitado (24/7):** Remoção completa da trava de 40 vídeos diários.
* **Filtros Personalizados Familiares:** Inserção de palavras-chave exclusivas (Whitelist e Blacklist) interpretadas dinamicamente pela IA.
* **Filtro Histórico por Data:** Desbloqueia o `DateRangePicker` nativo para paginação do histórico de vídeos em períodos específicos.
* **Score e Gráficos Semanais:** Análise profunda dos padrões de consumo e categorias acessadas.

---

## 🧠 Arquitetura e Engenharia de Software

O ecossistema foi dividido em um cliente leve (Android) e um cérebro robusto e seguro na nuvem (Node.js).

### 📱 Front-end / Sensor (Android)
* **UI/UX (Jetpack Compose):** Componentes modulares (Material Design 3), gerenciamento de estado e navegação fluida (sem falhas de tela branca no roteamento).
* **Otimização Extrema de Bateria:** Implementação de `BroadcastReceiver` dinâmico. O `AccessibilityService` entra em suspensão imediata quando o dispositivo é bloqueado (Screen Off), poupando CPU e bateria.
* **Auth Invisível:** Utilização de UID Anônimo do Firebase Authentication para vincular e rastrear dispositivos dependentes sem exigir criação de contas por parte da criança.
* **Validador de Código:** Tratamento robusto para pareamento seguro entre o app do Responsável e do Monitorado.

### ☁️ Back-end e IA (Node.js + Cloud Functions)
* **Server-Side AI:** Toda a lógica de Inteligência Artificial, Prompt Engineering e chaves de API (Gemini e YouTube Data API v3) foram isoladas no servidor, garantindo segurança absoluta.
* **Cache Global Multitenant:** Arquitetura de mitigação de custos. O servidor mapeia o raio-X neutro dos vídeos no Firestore. Se múltiplas crianças assistem ao mesmo vídeo, a API da IA é acionada apenas uma vez (Hit/Miss Cache), cruzando dados via JavaScript em microssegundos com as regras locais de cada família.
* **Engenharia de Prompt Anti-Burla:** Instruções supremas e blindagem contra *Prompt Injection*, impedindo que instruções maliciosas nos comentários do YouTube ou regras das famílias subvertam o modelo.

---

## 🛡️ Segurança e Defesa (Anti-Tampering)

O aplicativo conta com camadas sobrepostas de segurança para impedir a evasão do monitoramento por usuários avançados:

1. **Ofuscação R8/ProGuard:** O código-fonte, classes e rotas são severamente ofuscados, minificados e otimizados durante a compilação, tornando tentativas de engenharia reversa via descompilação do APK ineficazes.
2. **Defesa Ativa de Configurações:** O `AccessibilityService` detecta e bloqueia ativamente tentativas de acesso à tela de configurações do Android, forçando o retorno à tela inicial (Home) caso o usuário tente desativar o serviço manualmente.
3. **Firestore Security Rules:** O banco de dados possui regras estritas que validam payloads, tipos de dados e permissões, garantindo que usuários autenticados só leiam/escrevam nos nós pertencentes aos seus respectivos códigos de pareamento.
4. **Termos e Consentimento:** Aceite explícito dos Termos de Uso acoplado ao fluxo de ativação, garantindo compliance.

---

## 📸 Telas do Aplicativo

* **Painel de Controle:** Central de comando com atalhos de assinatura e status de monitoramento.
* **Relatório de Vídeos & Buscas:** Exibe os motivos detalhados gerados pela IA e o log de pesquisas.
* **Configurações:** Painel de regras de rigidez, notificações, PIN e listas de exceções.
* **Paywall:** Tela de conversão Premium com detalhamento de benefícios.

---

## Como executar o projeto

1. Clone este repositório:
   ```bash
   git clone [https://github.com/joaoSantosx/MonitorFunctional.git] (https://github.com/joaoSantosx/MonitorFunctional.git) 
