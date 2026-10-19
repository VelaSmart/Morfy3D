# DECKRA App

Aplicativo para configurar e monitorar o controlador físico DECKRA. O alvo inicial é **Windows 10/11**, com Arduino Leonardo ou Arduino Micro conectado por USB Serial.

**Branch de desenvolvimento:** `deckra-app`  
**Versão de protótipo:** `0.1.0`  
**Estado real:** scaffold visual e firmware inicial; integração ponta a ponta e testes em hardware ainda pendentes.

> Este README pertence somente ao DECKRA App. O README da raiz do repositório continua documentando o MORFY 3D/site comercial.

## Ele vai rodar na web ou no PC?

**O produto planejado é um aplicativo desktop para instalar e executar no PC.** A tecnologia escolhida é Tauri 2, com interface React/TypeScript e backend Rust.

Há duas formas diferentes de executar o código neste estágio:

- **No navegador, para desenvolver a interface:** `npm run dev` abre o frontend Vite em um navegador. Isso serve para revisar layout e testar o modo de demonstração. Não significa que a conexão real com o Arduino esteja funcionando no navegador.
- **Como aplicativo no Windows:** `npm run tauri dev` inicia a janela desktop do Tauri. É o caminho planejado para acessar as portas COM pelo backend Rust e, futuramente, gerar instaladores Windows (`.msi` e `.exe` via NSIS).

A interface atual ainda não chama os comandos Rust. Portanto, **nem no navegador nem na janela desktop a conexão serial real está integrada à interface neste momento**. O navegador poderia usar Web Serial em navegadores compatíveis, mas isso exigiria uma implementação específica e teria limitações de compatibilidade e permissões. Não é o caminho principal escolhido para o produto.

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
- [x] Configurados os alvos de empacotamento Windows MSI e NSIS.
- [x] Criada a interface visual com visão geral, estado demonstrativo, lista de controles, prévia de cor RGB e console de atividade.
- [x] Criado um modo de demonstração que injeta mensagens simuladas.
- [x] Criada a função inicial para formatar comandos RGB como JSON.
- [x] Melhorada a validação do parser para aceitar somente os tipos de mensagem conhecidos e validar campos básicos.

### Backend e firmware

- [x] Criado o módulo Rust com comando para listar portas seriais disponíveis.
- [x] Criado um comando de teste que tenta abrir e fechar uma porta serial.
- [x] Criado um ponto inicial para envio serial, que por enquanto retorna explicitamente que o envio não está habilitado.
- [x] Criado firmware inicial para Arduino Leonardo/Micro.
- [x] Firmware anuncia identificação e envia leituras de dois potenciômetros e dois botões em JSON.
- [x] Firmware aceita o comando `ping` e responde com um ACK.
- [x] Adicionadas notas iniciais de escopo, hardware e protocolo em `docs/`.

**Importante:** itens marcados como feitos significam que existe código inicial no repositório, não que tenham sido validados em execução ou com hardware real.

## O que falta fazer

### Prioridade 1: executar e validar o projeto

- [ ] Instalar dependências e confirmar `npm install`, `npm run build` e `npm run tauri dev` em uma máquina Windows 10/11.
- [ ] Compilar o backend Rust e resolver qualquer problema de dependência ou permissões.
- [ ] Adicionar testes automatizados para o parser e os comandos do protocolo.
- [ ] Registrar os resultados reais dos testes neste README, sem marcar como validado antes de executar.

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

### Prioridade 4: funcionalidades do produto

- [ ] Criar editor de mapeamento de botões e potenciômetros.
- [ ] Salvar, carregar, editar e validar perfis localmente.
- [ ] Persistir configurações entre reinicializações.
- [ ] Definir o comportamento de atalhos globais e verificar requisitos de permissões do Windows.
- [ ] Avaliar separadamente funções como controle de volume por aplicativo, que exigem integração adicional com o Windows.
- [ ] Criar instalador assinado e testar instalação, atualização e desinstalação.
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

Os comandos acima são as instruções configuradas no projeto; ainda precisam ser executados e validados em ambiente Windows. O modo de demonstração não requer Arduino.

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
| Execução no navegador | Prevista para desenvolvimento visual, sem serial real integrada |
| Aplicativo desktop Windows | Configuração Tauri criada, execução ainda não validada |
| Backend serial | Listagem e teste de abertura inicial; sessão persistente pendente |
| Firmware Arduino | Esqueleto funcional em código, ainda não testado em placa |
| Entradas físicas | Leitura de exemplo para 2 potenciômetros e 2 botões |
| LEDs RGB | Prévia visual; controle físico ainda não implementado |
| Perfis e mapeamentos | Ainda não implementados |
| Testes e instalador | Ainda não validados |

Última revisão documental: 19/10/2026.
