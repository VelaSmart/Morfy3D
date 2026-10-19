# DECKRA App

Aplicativo desktop para configurar o controlador físico DECKRA no Windows 10/11.

**Branch de desenvolvimento:** `deckra-app`  
**Estado:** scaffold inicial; comunicação e UI em desenvolvimento.  
**Hardware alvo:** Arduino Leonardo ou Arduino Micro.  
**Transporte inicial:** USB Serial (CDC).

## Arquitetura escolhida

- **Desktop:** Tauri 2 + React + TypeScript + Vite.
- **Comunicação serial:** módulo Rust no backend Tauri, usando uma biblioteca serialport, sem acesso serial direto no frontend.
- **Firmware:** C/C++ para Arduino Leonardo/Micro, com eventos serializados em JSON Lines durante o protótipo.
- **Testes:** parser de protocolo com testes unitários e um modo de demonstração sem hardware.

Leonardo e Micro usam ATmega32U4 e podem oferecer USB nativo, mas a primeira versão usará a interface USB Serial para manter a comunicação simples. Não assumir que a porta COM terá o mesmo número em todos os PCs.

## Funcionalidades do primeiro protótipo

1. Mostrar o estado da conexão serial.
2. Permitir selecionar uma porta COM e conectar/desconectar.
3. Receber identificação e eventos dos potenciômetros e botões.
4. Mostrar valores recebidos para diagnóstico.
5. Enviar comandos limitados para controlar LEDs RGB.
6. Simular dados sem hardware para desenvolver a interface.

Perfis persistentes, mapeamento de atalhos globais, volume por aplicativo e atualização de firmware serão etapas posteriores. Controle de volume por aplicativo exige integração própria com o Windows; não é garantido pelo Arduino nem pelo protocolo serial isoladamente.

## Estrutura

```text
deckra-app/
├── README.md
├── docs/
│   ├── PRODUCT_SCOPE.md
│   ├── HARDWARE_DECISIONS.md
│   └── PROTOCOL.md
├── app/                 # Tauri + React + TypeScript
├── firmware/
│   └── deckra_controller/
└── tests/
```

## Rodar o frontend

Requisitos: Node.js LTS, npm, Rust toolchain e dependências de compilação do Tauri para Windows.

```powershell
cd deckra-app/app
npm install
npm run dev
```

Para iniciar o shell desktop, após instalar os pré-requisitos do Tauri:

```powershell
npm run tauri dev
```

O acesso à porta serial é planejado para o backend Rust. O scaffold da interface não representa ainda uma conexão serial validada em hardware.

## Segurança elétrica e operacional

- Verificar o esquema elétrico real antes de conectar potenciômetros e LEDs.
- Usar resistores limitadores de corrente nos LEDs RGB; confirmar se são anodo comum ou catodo comum.
- Conectar potenciômetros aos pinos analógicos apenas depois de confirmar o circuito e os limites de tensão.
- Não alimentar LEDs de alta corrente diretamente pelos pinos do microcontrolador.
- Validar todos os dados serializados antes de executar ações.
- Não executar comandos arbitrários recebidos pela porta serial.
