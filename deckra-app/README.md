# DECKRA App

Aplicativo para configurar e monitorar o controlador físico DECKRA. O alvo inicial é **Windows 10/11**, com Arduino Leonardo ou Arduino Micro conectado por USB Serial.

**Branch de desenvolvimento:** `deckra-app`  
**Versão de protótipo:** `0.1.0`  
**Estado real:** scaffold visual e firmware inicial; integração ponta a ponta e testes em hardware ainda pendentes.

> Este README pertence somente ao DECKRA App. O README da raiz do repositório continua documentando o MORFY 3D/site comercial.

## Ele vai rodar na web ou no PC?

**O produto planejado é um aplicativo desktop para instalar e executar no PC.** A tecnologia escolhida é Tauri 2, com interface React/TypeScript e backend Rust.

- **No navegador, durante o desenvolvimento:** `npm run dev` abre o frontend Vite para revisar a interface e o modo de demonstração. Isso não significa que a conexão real com o Arduino esteja funcionando no navegador.
- **Como aplicativo no Windows:** `npm run tauri dev` inicia a janela desktop do Tauri. Esse é o caminho planejado para acessar portas COM pelo backend Rust e gerar um instalador para o usuário final.

A interface atual ainda não chama os comandos Rust. Portanto, **a conexão serial real ainda não está integrada à interface**. O navegador poderia usar Web Serial em navegadores compatíveis, mas isso exigiria implementação específica e teria limitações de compatibilidade e permissões. Não é o caminho principal escolhido para o produto.

## Instalação simples para o usuário

A versão pública do DECKRA deve ter uma instalação simples, sem exigir que a pessoa instale Node.js, Rust, Git ou Arduino IDE.

### Experiência desejada

1. A pessoa abre a página de lançamentos do DECKRA no GitHub.
2. Baixa o instalador para Windows, por exemplo `DECKRA-Setup-0.1.0-x64.exe`.
3. Dá dois cliques no arquivo e segue o assistente de instalação.
4. O instalador coloca o aplicativo no PC e cria os atalhos previstos.
5. A pessoa abre o DECKRA Control Center pelo menu Iniciar ou atalho.

O instalador deve empacotar o aplicativo compilado e seus recursos necessários. **Não deve baixar o código-fonte e tentar compilar o projeto no computador do usuário.**

### Como usar o GitHub para distribuir os instaladores

O repositório GitHub será a fonte oficial do código. O fluxo recomendado é:

- Desenvolver e revisar o código na branch `deckra-app`.
- Quando uma versão estiver pronta, criar uma tag de versão, como `v0.1.0`.
- Usar GitHub Actions para compilar e empacotar o aplicativo Windows em um ambiente de build.
- Publicar os instaladores gerados em **GitHub Releases**, anexados à versão correspondente.
- Colocar no README e na página do projeto um link para a última versão estável.
- Opcionalmente, implementar no futuro um verificador de atualizações que consulte as versões publicadas no GitHub e informe quando houver uma nova versão.

**Importante:** o instalador final deve baixar o binário pronto em Releases, não os arquivos soltos da branch. A branch contém código-fonte e pode conter alterações incompletas. Se o repositório for privado, as pessoas que forem baixar os arquivos precisarão de acesso autorizado; para distribuição pública, os Releases precisam estar acessíveis ao público. Não colocar tokens ou credenciais Git dentro do aplicativo.

### Escolha do formato

A configuração atual do Tauri prevê os alvos **NSIS (`.exe`) e MSI (`.msi`)**. Para a experiência de “baixar, dar dois cliques e instalar”, o instalador NSIS `.exe` é a opção principal recomendada. O MSI pode ser mantido como alternativa para instalações gerenciadas.

Ainda falta configurar e testar o pipeline de build e publicação automática. A presença dos formatos no arquivo de configuração não significa que já exista um instalador pronto para baixar.

## Tecnologias e arquitetura

- **Interface:** React + TypeScript + Vite.
- **Aplicativo desktop:** Tauri 2.
- **Acesso nativo ao hardware:** Rust + biblioteca `serialport`.
- **Firmware:** Arduino C++ para Leonardo/Micro (ATmega32U4).
- **Protocolo inicial:** JSON Lines, uma mensagem JSON por linha, a 115200 baud.

O frontend não deve abrir a porta serial diretamente. A interface deve chamar comandos do backend Tauri, e o backend deve gerenciar a conexão, validar mensagens e encaminhar eventos para a interface.

## O que já foi feito

### Estrutura e interface

- [x] Criada a pasta independente `deckra-app/`, separada do código do site MORFY 3D.
- [x] Criado o scaffold do frontend React/TypeScript/Vite.
- [x] Criada a configuração inicial do Tauri 2 e da janela desktop.
- [x] Configurados no Tauri os alvos de empacotamento Windows MSI e NSIS.
- [x] Criada a interface visual com visão geral, estado demonstrativo, lista de controles, prévia de cor RGB e console de atividade.
- [x] Criado um modo de demonstração que injeta mensagens simuladas.
- [x] Criada a função inicial para formatar comandos RGB como JSON.
- [x] Melhorada a validação do parser para aceitar somente tipos de mensagem conhecidos e validar campos básicos.
- [x] Documentada a experiência desejada de instalação simples e distribuição via GitHub Releases.

### Backend e firmware

- [x] Criado o módulo Rust com comando para listar portas seriais disponíveis.
- [x] Criado um comando de teste que tenta abrir e fechar uma porta serial.
- [x] Criado um ponto inicial para envio serial, que por enquanto retorna explicitamente que o envio não está habilitado.
- [x] Criado firmware inicial para Arduino Leonardo/Micro.
- [x] Firmware anuncia identificação e envia leituras de dois potenciômetros e dois botões em JSON.
- [x] Firmware aceita o comando `ping` e responde com um ACK.
- [x] Adicionadas notas iniciais de escopo, hardware e protocolo em `docs/`.

**Importante:** itens marcados como feitos significam que existe código inicial no repositório ou uma decisão documentada, não que tenham sido validados em execução ou com hardware real.

## O que falta fazer

### Prioridade 1: compilar e validar

- [ ] Instalar dependências e confirmar `npm install`, `npm run build` e `npm run tauri dev` em Windows 10/11.
- [ ] Compilar o backend Rust e resolver problemas de dependência ou permissões.
- [ ] Adicionar testes automatizados para o parser e os comandos do protocolo.
- [ ] Registrar os resultados reais dos testes neste README.

### Prioridade 2: conexão serial real

- [ ] Ligar a interface aos comandos Tauri usando `@tauri-apps/api/core`.
- [ ] Exibir portas detectadas dinamicamente em vez da lista ilustrativa COM3–COM6.
- [ ] Implementar uma sessão serial persistente no backend, com abrir, ler, escrever e fechar.
- [ ] Encaminhar eventos serial recebidos para a interface em tempo real.
- [ ] Tratar desconexão física, porta ocupada, timeout, reconexão e erros de protocolo.
- [ ] Validar a identificação e a versão do protocolo antes de confiar nos dados do dispositivo.

### Prioridade 3: hardware e firmware

- [ ] Definir se a placa final será Arduino Leonardo ou Micro.
- [ ] Confirmar a quantidade real de potenciômetros, botões e LEDs RGB.
- [ ] Confirmar esquema elétrico, pinagem, tipo de LED RGB e resistores/drivers necessários.
- [ ] Ajustar a taxa de leitura dos potenciômetros e implementar debounce dos botões.
- [ ] Implementar no firmware o comando `set_rgb`, com validação de ID, canais e comprimento da mensagem.
- [ ] Implementar o envio RGB pelo backend e mostrar confirmação ou erro na interface.
- [ ] Testar o protocolo com a placa real e documentar o resultado.

### Prioridade 4: instalador fácil e atualizações

- [ ] Criar um workflow GitHub Actions para compilar o app em Windows.
- [ ] Gerar um instalador NSIS `.exe` x64, com nome, ícone, versão e informações do DECKRA.
- [ ] Avaliar se será necessário oferecer também MSI para instalação corporativa.
- [ ] Testar a instalação em um Windows limpo, sem Node.js, Rust ou ferramentas de desenvolvimento.
- [ ] Testar abertura, desinstalação, reinstalação e preservação das configurações do usuário.
- [ ] Publicar instaladores e notas de versão no GitHub Releases.
- [ ] Adicionar ao README um link para baixar a versão estável mais recente quando o primeiro Release estiver publicado.
- [ ] Configurar assinatura digital do instalador quando houver certificado disponível, para melhorar a confiança do Windows/SmartScreen.
- [ ] Planejar atualização automática ou notificação de nova versão, sem embutir tokens de acesso no aplicativo.

### Prioridade 5: funcionalidades do produto

- [ ] Criar editor de mapeamento de botões e potenciômetros.
- [ ] Salvar, carregar, editar e validar perfis localmente.
- [ ] Persistir configurações entre reinicializações.
- [ ] Definir o comportamento de atalhos globais e verificar requisitos do Windows.
- [ ] Avaliar controle de volume por aplicativo, que exige integração adicional com o sistema operacional.
- [ ] Preparar documentação de usuário e instruções de recuperação de erros.

## Como executar durante o desenvolvimento

Requisitos previstos: Node.js LTS, npm, Rust toolchain e os pré-requisitos de compilação do Tauri para Windows.

Na pasta do aplicativo:

```powershell
cd deckra-app/app
npm install
npm run dev
```

Esse comando abre o frontend para desenvolvimento no navegador. Para iniciar a janela desktop:

```powershell
npm run tauri dev
```

Essas são as instruções configuradas no projeto; ainda precisam ser executadas e validadas em ambiente Windows. O modo de demonstração não requer Arduino.

## Estrutura

```text
deckra-app/
├── README.md
├── docs/
│   ├── PRODUCT_SCOPE.md
│   ├── HARDWARE_DECISIONS.md
│   └── PROTOCOL.md
├── app/
│   ├── src/                 # Interface React e protocolo
│   └── src-tauri/           # Aplicativo desktop e backend Rust
└── firmware/
    └── deckra_controller/   # Firmware inicial Arduino
```

## Segurança elétrica

- Confirmar o esquema e o pinout da placa antes de fazer as ligações.
- Usar resistores limitadores de corrente e, se necessário, drivers para os LEDs RGB.
- Não alimentar LEDs de corrente elevada diretamente pelos GPIOs.
- Manter os sinais dos potenciômetros dentro da faixa permitida pelas entradas analógicas.
- Nunca executar comandos arbitrários recebidos pela serial.
- Validar dados recebidos e restringir os comandos a uma lista permitida.

## Estado resumido

| Área | Situação |
|---|---|
| Interface visual | Scaffold criado, com dados demonstrativos |
| Execução no navegador | Desenvolvimento visual; sem serial real integrada |
| Aplicativo desktop Windows | Configuração Tauri criada; execução ainda não validada |
| Backend serial | Listagem e teste de abertura inicial; sessão persistente pendente |
| Firmware Arduino | Esqueleto criado, ainda não testado em placa |
| Entradas físicas | Leitura de exemplo para 2 potenciômetros e 2 botões |
| LEDs RGB | Prévia visual; controle físico ainda não implementado |
| Instalador | Alvos NSIS/MSI configurados; pipeline e binários ainda não publicados |
| GitHub Releases | Estratégia documentada; publicação ainda pendente |
| Perfis e mapeamentos | Ainda não implementados |
| Testes | Ainda não validados |

Última revisão documental: 19/10/2026.
